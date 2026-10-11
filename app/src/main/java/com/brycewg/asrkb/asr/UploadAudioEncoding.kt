/**
 * 在线文件识别上传前音频编码与格式描述。
 *
 * 归属模块：asr
 */
package com.brycewg.asrkb.asr

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

enum class UploadAudioContainer(
    val extension: String,
    val mimeType: String,
    val publicFormat: String
) {
    M4A(extension = "m4a", mimeType = "audio/mp4", publicFormat = "m4a"),
    AAC(extension = "aac", mimeType = "audio/aac", publicFormat = "aac"),
    OGG_OPUS(extension = "ogg", mimeType = "audio/ogg", publicFormat = "ogg"),
    WAV(extension = "wav", mimeType = "audio/wav", publicFormat = "wav")
}

data class UploadAudioEncodingSpec(
    val container: UploadAudioContainer,
    val bitRate: Int = DEFAULT_AAC_BIT_RATE
) {
    companion object {
        val M4A_AAC_LC = UploadAudioEncodingSpec(UploadAudioContainer.M4A)
        val AAC_ADTS = UploadAudioEncodingSpec(UploadAudioContainer.AAC)
        val OGG_OPUS = UploadAudioEncodingSpec(
            container = UploadAudioContainer.OGG_OPUS,
            bitRate = DEFAULT_OPUS_BIT_RATE
        )
    }
}

data class UploadAudioData(
    val bytes: ByteArray,
    val container: UploadAudioContainer,
    val sampleRate: Int,
    val channels: Int,
    val sourceBytes: Int,
    val durationMs: Long,
    val encodeElapsedMs: Long,
    val feedElapsedMs: Long,
    val finishElapsedMs: Long
) {
    val fileName: String get() = "audio.${container.extension}"
    val mimeType: String get() = container.mimeType
    val format: String get() = container.publicFormat
}

private const val DEFAULT_AAC_BIT_RATE = 32_000
private const val DEFAULT_OPUS_BIT_RATE = 24_000
private const val CHANNEL_COUNT_MONO = 1
private const val ENCODER_TIMEOUT_US = 10_000L
private const val TAG_UPLOAD_AUDIO_ENCODING = "UploadAudioEncoding"

internal fun isOggOpusUploadEncodingSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

internal fun oggOpusUploadAudioEncodingSpecIfSupported(): UploadAudioEncodingSpec? = if (isOggOpusUploadEncodingSupported()) UploadAudioEncodingSpec.OGG_OPUS else null

internal fun encodePcmForUpload(
    context: Context,
    pcm: ByteArray,
    sampleRate: Int,
    spec: UploadAudioEncodingSpec
): UploadAudioData {
    val session = createUploadAudioEncodingSession(context, sampleRate, spec)
    return try {
        session.writePcm(pcm)
        session.finish().also { audio ->
            logUploadAudioCompression(
                compressed = true,
                sourceBytes = audio.sourceBytes,
                outputBytes = audio.bytes.size,
                durationMs = audio.durationMs,
                elapsedMs = audio.encodeElapsedMs,
                feedElapsedMs = audio.feedElapsedMs,
                finishElapsedMs = audio.finishElapsedMs,
                format = audio.format
            )
        }
    } finally {
        session.close()
    }
}

internal fun logUploadAudioCompression(
    compressed: Boolean,
    sourceBytes: Int,
    outputBytes: Int,
    durationMs: Long,
    elapsedMs: Long,
    feedElapsedMs: Long,
    finishElapsedMs: Long,
    format: String
) {
    val ratio = if (sourceBytes > 0) outputBytes.toDouble() / sourceBytes else 0.0
    Log.i(
        TAG_UPLOAD_AUDIO_ENCODING,
        "upload_audio_compression compressed=$compressed format=$format " +
            "before_bytes=$sourceBytes after_bytes=$outputBytes " +
            "ratio=${String.format(java.util.Locale.US, "%.3f", ratio)} " +
            "duration_ms=$durationMs elapsed_ms=$elapsedMs " +
            "feed_elapsed_ms=$feedElapsedMs finish_elapsed_ms=$finishElapsedMs"
    )
}

internal fun createUploadAudioEncodingSession(
    context: Context,
    sampleRate: Int,
    spec: UploadAudioEncodingSpec
): UploadAudioEncodingSession = when (spec.container) {
    UploadAudioContainer.M4A -> AacM4aEncodingSession(context, sampleRate, spec.bitRate)
    UploadAudioContainer.AAC -> AacAdtsEncodingSession(sampleRate, spec.bitRate)
    UploadAudioContainer.OGG_OPUS -> {
        check(isOggOpusUploadEncodingSupported()) {
            "OGG Opus upload audio requires Android 10 (API 29)"
        }
        OpusOggEncodingSession(context, sampleRate, spec.bitRate)
    }
    UploadAudioContainer.WAV -> error("WAV upload audio does not use an encoding session")
}

internal interface UploadAudioEncodingSession : AutoCloseable {
    fun writePcm(pcm: ByteArray)
    fun finish(): UploadAudioData
}

private abstract class BaseCodecEncodingSession(
    private val sampleRate: Int,
    bitRate: Int,
    private val mimeType: String,
    private val codecLabel: String
) : UploadAudioEncodingSession {
    private val codec: MediaCodec = MediaCodec.createEncoderByType(mimeType)
    private val bufferInfo = MediaCodec.BufferInfo()
    private var submittedSamples: Long = 0L
    private var sourceBytes: Int = 0
    private var feedElapsedNanos: Long = 0L
    private var finishElapsedNanos: Long = 0L
    private var finished = false
    private var closed = false

    init {
        val format = MediaFormat.createAudioFormat(
            mimeType,
            sampleRate,
            CHANNEL_COUNT_MONO
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, sampleRate / 5 * 2)
            configureFormat(this)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
    }

    override fun writePcm(pcm: ByteArray) {
        check(!finished) { "$codecLabel encoder has already been finished" }
        val startedAt = System.nanoTime()
        try {
            sourceBytes += pcm.size
            var offset = 0
            while (offset < pcm.size) {
                val inputIndex = codec.dequeueInputBuffer(ENCODER_TIMEOUT_US)
                if (inputIndex >= 0) {
                    val input = codec.getInputBuffer(inputIndex)
                        ?: error("$codecLabel encoder input buffer is null")
                    input.clear()
                    val length = minOf(input.remaining(), pcm.size - offset)
                    input.put(pcm, offset, length)
                    val presentationUs = submittedSamples * 1_000_000L / sampleRate
                    codec.queueInputBuffer(inputIndex, 0, length, presentationUs, 0)
                    submittedSamples += length / 2
                    offset += length
                }
                drain(endOfStream = false)
            }
        } finally {
            feedElapsedNanos += System.nanoTime() - startedAt
        }
    }

    override fun finish(): UploadAudioData {
        check(!finished) { "$codecLabel encoder has already been finished" }
        finished = true
        val startedAt = System.nanoTime()
        try {
            while (true) {
                val inputIndex = codec.dequeueInputBuffer(ENCODER_TIMEOUT_US)
                if (inputIndex >= 0) {
                    val presentationUs = submittedSamples * 1_000_000L / sampleRate
                    codec.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        presentationUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    break
                }
                drain(endOfStream = false)
            }
            drain(endOfStream = true)
        } finally {
            finishElapsedNanos += System.nanoTime() - startedAt
        }
        val feedElapsedMs = TimeUnit.NANOSECONDS.toMillis(feedElapsedNanos)
        val finishElapsedMs = TimeUnit.NANOSECONDS.toMillis(finishElapsedNanos)
        return buildResult(
            sampleRate = sampleRate,
            sourceBytes = sourceBytes,
            durationMs = submittedSamples * 1_000L / sampleRate,
            encodeElapsedMs = feedElapsedMs + finishElapsedMs,
            feedElapsedMs = feedElapsedMs,
            finishElapsedMs = finishElapsedMs
        )
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            codec.stop()
        } finally {
            codec.release()
            releaseOutput()
        }
    }

    private fun drain(endOfStream: Boolean) {
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, ENCODER_TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    onOutputFormat(codec.outputFormat)
                }
                outputIndex >= 0 -> {
                    val output = codec.getOutputBuffer(outputIndex)
                        ?: error("$codecLabel encoder output buffer is null")
                    if (
                        bufferInfo.size > 0 &&
                        (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    ) {
                        val payload = output.sliceFor(bufferInfo.offset, bufferInfo.size)
                        val sampleInfo = MediaCodec.BufferInfo().apply {
                            set(
                                0,
                                bufferInfo.size,
                                bufferInfo.presentationTimeUs,
                                bufferInfo.flags
                            )
                        }
                        onEncodedBuffer(payload, sampleInfo)
                    }
                    val flags = bufferInfo.flags
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    protected open fun configureFormat(format: MediaFormat) = Unit
    protected abstract fun onOutputFormat(format: MediaFormat)
    protected abstract fun onEncodedBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo)
    protected abstract fun buildResult(
        sampleRate: Int,
        sourceBytes: Int,
        durationMs: Long,
        encodeElapsedMs: Long,
        feedElapsedMs: Long,
        finishElapsedMs: Long
    ): UploadAudioData
    protected open fun releaseOutput() = Unit
}

private abstract class BaseAacEncodingSession(
    sampleRate: Int,
    bitRate: Int
) : BaseCodecEncodingSession(
    sampleRate = sampleRate,
    bitRate = bitRate,
    mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
    codecLabel = "AAC"
) {
    override fun configureFormat(format: MediaFormat) {
        format.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
    }
}

private class AacM4aEncodingSession(
    context: Context,
    sampleRate: Int,
    bitRate: Int
) : BaseAacEncodingSession(sampleRate, bitRate) {
    private val outputFile = File.createTempFile("asr_upload_", ".m4a", context.cacheDir)
    private val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var trackIndex = -1
    private var muxerStarted = false
    private var muxerReleased = false

    override fun onOutputFormat(format: MediaFormat) {
        check(!muxerStarted) { "AAC M4A muxer format changed after start" }
        trackIndex = muxer.addTrack(format)
        muxer.start()
        muxerStarted = true
    }

    override fun onEncodedBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        check(muxerStarted) { "AAC M4A muxer has not started" }
        muxer.writeSampleData(trackIndex, buffer, info)
    }

    override fun buildResult(
        sampleRate: Int,
        sourceBytes: Int,
        durationMs: Long,
        encodeElapsedMs: Long,
        feedElapsedMs: Long,
        finishElapsedMs: Long
    ): UploadAudioData {
        releaseMuxer()
        val bytes = outputFile.readBytes()
        if (!outputFile.delete()) {
            outputFile.deleteOnExit()
        }
        return UploadAudioData(
            bytes = bytes,
            container = UploadAudioContainer.M4A,
            sampleRate = sampleRate,
            channels = CHANNEL_COUNT_MONO,
            sourceBytes = sourceBytes,
            durationMs = durationMs,
            encodeElapsedMs = encodeElapsedMs,
            feedElapsedMs = feedElapsedMs,
            finishElapsedMs = finishElapsedMs
        )
    }

    override fun releaseOutput() {
        releaseMuxer()
        if (outputFile.exists() && !outputFile.delete()) {
            outputFile.deleteOnExit()
        }
    }

    private fun releaseMuxer() {
        if (muxerReleased) return
        muxerReleased = true
        try {
            if (muxerStarted) muxer.stop()
        } finally {
            muxer.release()
        }
    }
}

private class AacAdtsEncodingSession(
    private val sampleRate: Int,
    bitRate: Int
) : BaseAacEncodingSession(sampleRate, bitRate) {
    private val out = ByteArrayOutputStream()

    override fun onOutputFormat(format: MediaFormat) = Unit

    override fun onEncodedBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) return
        val payload = ByteArray(buffer.remaining())
        buffer.get(payload)
        out.write(buildAdtsHeader(payload.size, sampleRate, CHANNEL_COUNT_MONO))
        out.write(payload)
    }

    override fun buildResult(
        sampleRate: Int,
        sourceBytes: Int,
        durationMs: Long,
        encodeElapsedMs: Long,
        feedElapsedMs: Long,
        finishElapsedMs: Long
    ): UploadAudioData = UploadAudioData(
        bytes = out.toByteArray(),
        container = UploadAudioContainer.AAC,
        sampleRate = sampleRate,
        channels = CHANNEL_COUNT_MONO,
        sourceBytes = sourceBytes,
        durationMs = durationMs,
        encodeElapsedMs = encodeElapsedMs,
        feedElapsedMs = feedElapsedMs,
        finishElapsedMs = finishElapsedMs
    )
}

@android.annotation.TargetApi(Build.VERSION_CODES.Q)
private class OpusOggEncodingSession(
    context: Context,
    sampleRate: Int,
    bitRate: Int
) : BaseCodecEncodingSession(
    sampleRate = sampleRate,
    bitRate = bitRate,
    mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS,
    codecLabel = "Opus"
) {
    private val outputFile = File.createTempFile("asr_upload_", ".ogg", context.cacheDir)
    private val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
    private var trackIndex = -1
    private var muxerStarted = false
    private var muxerReleased = false

    override fun onOutputFormat(format: MediaFormat) {
        check(!muxerStarted) { "Opus OGG muxer format changed after start" }
        trackIndex = muxer.addTrack(format)
        muxer.start()
        muxerStarted = true
    }

    override fun onEncodedBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        check(muxerStarted) { "Opus OGG muxer has not started" }
        muxer.writeSampleData(trackIndex, buffer, info)
    }

    override fun buildResult(
        sampleRate: Int,
        sourceBytes: Int,
        durationMs: Long,
        encodeElapsedMs: Long,
        feedElapsedMs: Long,
        finishElapsedMs: Long
    ): UploadAudioData {
        releaseMuxer()
        val bytes = outputFile.readBytes()
        if (!outputFile.delete()) {
            outputFile.deleteOnExit()
        }
        return UploadAudioData(
            bytes = bytes,
            container = UploadAudioContainer.OGG_OPUS,
            sampleRate = sampleRate,
            channels = CHANNEL_COUNT_MONO,
            sourceBytes = sourceBytes,
            durationMs = durationMs,
            encodeElapsedMs = encodeElapsedMs,
            feedElapsedMs = feedElapsedMs,
            finishElapsedMs = finishElapsedMs
        )
    }

    override fun releaseOutput() {
        releaseMuxer()
        if (outputFile.exists() && !outputFile.delete()) {
            outputFile.deleteOnExit()
        }
    }

    private fun releaseMuxer() {
        if (muxerReleased) return
        muxerReleased = true
        try {
            if (muxerStarted) muxer.stop()
        } finally {
            muxer.release()
        }
    }
}

private fun ByteBuffer.sliceFor(offset: Int, size: Int): ByteBuffer {
    val duplicate = duplicate()
    duplicate.position(offset)
    duplicate.limit(offset + size)
    return duplicate.slice()
}

private fun buildAdtsHeader(payloadSize: Int, sampleRate: Int, channels: Int): ByteArray {
    val profile = 2
    val freqIdx = aacSamplingFrequencyIndex(sampleRate)
    val packetLen = payloadSize + 7
    return byteArrayOf(
        0xFF.toByte(),
        0xF1.toByte(),
        (((profile - 1) shl 6) + (freqIdx shl 2) + (channels shr 2)).toByte(),
        (((channels and 3) shl 6) + (packetLen shr 11)).toByte(),
        ((packetLen and 0x7FF) shr 3).toByte(),
        (((packetLen and 7) shl 5) + 0x1F).toByte(),
        0xFC.toByte()
    )
}

private fun aacSamplingFrequencyIndex(sampleRate: Int): Int = when (sampleRate) {
    96_000 -> 0
    88_200 -> 1
    64_000 -> 2
    48_000 -> 3
    44_100 -> 4
    32_000 -> 5
    24_000 -> 6
    22_050 -> 7
    16_000 -> 8
    12_000 -> 9
    11_025 -> 10
    8_000 -> 11
    7_350 -> 12
    else -> error("Unsupported AAC sample rate: $sampleRate")
}
