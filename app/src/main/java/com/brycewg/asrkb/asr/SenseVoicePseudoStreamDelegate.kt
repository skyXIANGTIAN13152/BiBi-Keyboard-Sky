package com.brycewg.asrkb.asr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs
import com.brycewg.asrkb.util.TextSanitizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SenseVoicePseudoStreamDelegate(
    private val context: Context,
    private val scope: CoroutineScope,
    private val prefs: Prefs,
    private val listener: StreamingAsrEngine.Listener,
    private val sampleRate: Int,
    private val onRequestDuration: ((Long) -> Unit)?,
    private val tag: String
) {
    private val previewMutex = Mutex()

    @Volatile
    private var activeSessionId: Long = 0L

    @Volatile
    private var previewSegments = mutableListOf<String>()

    @Volatile
    private var previewJob: Job? = null

    @Volatile
    private var loadLog: LocalAsrCallLogger.Session? = null

    fun onSessionStart(sessionId: Long) {
        previewJob?.cancel()
        previewJob = null
        loadLog?.cancel("New pseudo stream session")
        loadLog = null
        activeSessionId = sessionId
        previewSegments = mutableListOf()
    }

    fun ensureReady(): Boolean {
        val manager = SenseVoiceOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) {
            try {
                listener.onError(context.getString(R.string.error_local_asr_not_ready))
            } catch (t: Throwable) {
                Log.e(tag, "Failed to send error callback", t)
            }
            return false
        }
        return true
    }

    fun onSegmentBoundary(sessionId: Long, pcmSegment: ByteArray) {
        // 预览识别放到后台，避免阻塞录音
        previewJob?.cancel()
        previewJob = scope.launch(Dispatchers.IO) {
            if (sessionId != activeSessionId) return@launch
            val text = try {
                decodeOnce(pcmSegment, reportErrorToUser = false)
            } catch (t: Throwable) {
                Log.e(tag, "Segment decode failed", t)
                null
            } ?: return@launch

            if (sessionId != activeSessionId) return@launch
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@launch

            // 对当前片段先做一次句末标点/emoji 修剪，然后再参与累积
            val normalizedSegment = try {
                TextSanitizer.trimTrailingPunctAndEmoji(trimmed)
            } catch (t: Throwable) {
                Log.w(
                    tag,
                    "trimTrailingPunctAndEmoji failed for segment, fallback to raw trimmed text",
                    t
                )
                trimmed
            }
            if (normalizedSegment.isEmpty()) return@launch

            try {
                previewMutex.withLock {
                    val segments = previewSegments
                    if (sessionId != activeSessionId) return@withLock
                    segments.add(normalizedSegment)
                    // 简单拼接，避免在中文场景插入多余空格
                    val merged = segments.joinToString(separator = "")
                    // 为安全起见，对整个预览再做一次尾部修剪
                    val previewOut = try {
                        TextSanitizer.trimTrailingPunctAndEmoji(merged)
                    } catch (t: Throwable) {
                        Log.w(
                            tag,
                            "trimTrailingPunctAndEmoji failed for preview, fallback to merged",
                            t
                        )
                        merged
                    }
                    if (sessionId != activeSessionId) return@withLock
                    try {
                        listener.onPartial(previewOut)
                    } catch (t: Throwable) {
                        Log.e(tag, "Failed to notify partial", t)
                    }
                }
            } catch (t: Throwable) {
                Log.e(tag, "Failed to update preview segments", t)
            }
        }
    }

    suspend fun onSessionFinished(sessionId: Long, fullPcm: ByteArray) {
        val t0 = System.currentTimeMillis()
        val localLog = LocalAsrCallLogger.startInference(
            prefs = prefs,
            vendor = AsrVendor.SenseVoice,
            source = "pseudo_stream_final",
            audioBytes = fullPcm.size,
            sampleRate = sampleRate
        )
        try {
            if (sessionId != activeSessionId) return
            previewJob?.cancel()
            previewJob = null
            val text = decodeOnce(fullPcm, reportErrorToUser = true)
            val dt = System.currentTimeMillis() - t0
            if (sessionId != activeSessionId) return
            try {
                onRequestDuration?.invoke(dt)
            } catch (t: Throwable) {
                Log.e(tag, "Failed to invoke duration callback", t)
            }

            if (sessionId != activeSessionId) return
            if (text.isNullOrBlank()) {
                if (sessionId != activeSessionId) return
                val msg = context.getString(R.string.error_asr_empty_result)
                localLog.failure(msg)
                try {
                    listener.onError(msg)
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify empty result error", t)
                }
            } else {
                val raw = text.trim()
                if (sessionId != activeSessionId) return
                localLog.successWithText(raw)
                try {
                    listener.onFinal(raw)
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify final result", t)
                }
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(tag, "Final recognition failed", t)
            if (sessionId != activeSessionId) return
            val msg = context.getString(
                R.string.error_recognize_failed_with_reason,
                t.message ?: ""
            )
            loadLog?.failure(t.message ?: msg)
            loadLog = null
            localLog.failure(msg)
            try {
                listener.onError(msg)
            } catch (e: Throwable) {
                Log.e(tag, "Failed to notify final recognition error", e)
            }
        } finally {
            loadLog?.failure("Model load did not complete")
            loadLog = null
            previewJob = null
            try {
                previewMutex.withLock {
                    val segments = previewSegments
                    if (sessionId != activeSessionId) return@withLock
                    segments.clear()
                }
            } catch (t: Throwable) {
                Log.e(tag, "Failed to reset preview segments after session", t)
            }
        }
    }

    private fun notifyLoadStart() {
        val ui = (listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi)
        if (ui != null) {
            try {
                ui.onLocalModelLoadStart()
            } catch (t: Throwable) {
                Log.e(tag, "Failed to notify load start", t)
            }
        } else {
            try {
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(
                            context,
                            context.getString(R.string.sv_loading_model),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (t: Throwable) {
                        Log.e(tag, "Failed to show toast", t)
                    }
                }
            } catch (t: Throwable) {
                Log.e(tag, "Failed to post toast", t)
            }
        }
    }

    private fun notifyLoadDone() {
        val ui = (listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi)
        if (ui != null) {
            try {
                ui.onLocalModelLoadDone()
            } catch (t: Throwable) {
                Log.e(tag, "Failed to notify load done", t)
            }
        }
    }

    private suspend fun decodeOnce(pcm: ByteArray, reportErrorToUser: Boolean): String? {
        val manager = SenseVoiceOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) {
            if (reportErrorToUser) {
                try {
                    listener.onError(context.getString(R.string.error_local_asr_not_ready))
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to send not-ready error", t)
                }
            } else {
                Log.w(tag, "SenseVoice model not available")
            }
            return null
        }

        val resolvedModelCheck = checkSenseVoiceModel(context, prefs)
        val resolvedModel = (resolvedModelCheck as? LocalModelCheck.Ready)?.value
        if (resolvedModel == null) {
            if (reportErrorToUser) {
                try {
                    listener.onError(
                        localModelErrorMessage(
                            context,
                            resolvedModelCheck,
                            R.string.error_sensevoice_model_missing
                        )
                    )
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify model-missing error", t)
                }
            } else {
                Log.w(tag, "SenseVoice model directory missing")
            }
            return null
        }

        val samples = pcmToFloatArray(pcm)
        if (samples.isEmpty()) {
            if (reportErrorToUser) {
                try {
                    listener.onError(context.getString(R.string.error_audio_empty))
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify empty audio error", t)
                }
            }
            return null
        }

        val keepMinutes = try {
            prefs.svKeepAliveMinutes
        } catch (t: Throwable) {
            Log.w(tag, "Failed to get keep alive minutes", t)
            -1
        }
        val keepMs = if (keepMinutes <= 0) 0L else keepMinutes.toLong() * 60_000L
        val alwaysKeep = keepMinutes < 0
        val text = manager.decodeOffline(
            assetManager = null,
            tokens = resolvedModel.tokensPath,
            model = resolvedModel.modelPath,
            language = try {
                resolveSvLanguageForVariant(prefs.svLanguage, resolvedModel.variant)
            } catch (t: Throwable) {
                Log.w(tag, "Failed to get language", t)
                "auto"
            },
            useItn = try {
                prefs.svUseItn
            } catch (t: Throwable) {
                Log.w(tag, "Failed to get useItn", t)
                false
            },
            provider = "cpu",
            numThreads = try {
                prefs.svNumThreads
            } catch (t: Throwable) {
                Log.w(tag, "Failed to get num threads", t)
                2
            },
            samples = samples,
            sampleRate = sampleRate,
            keepAliveMs = keepMs,
            alwaysKeep = alwaysKeep,
            onLoadStart = {
                loadLog = LocalAsrCallLogger.startLoad(
                    prefs = prefs,
                    vendor = AsrVendor.SenseVoice,
                    source = if (reportErrorToUser) "pseudo_stream_final_load" else "pseudo_stream_preview_load"
                )
                notifyLoadStart()
            },
            onLoadDone = {
                loadLog?.success("loaded=true")
                loadLog = null
                notifyLoadDone()
            }
        )

        if (text.isNullOrBlank()) {
            if (reportErrorToUser) {
                try {
                    listener.onError(context.getString(R.string.error_asr_empty_result))
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify empty result error", t)
                }
            }
            return null
        }

        return text.trim()
    }

    private fun pcmToFloatArray(pcm: ByteArray): FloatArray {
        if (pcm.isEmpty()) return FloatArray(0)
        val n = pcm.size / 2
        val out = FloatArray(n)
        var i = 0
        var offset = 0
        while (i < n) {
            val s = (pcm[offset + 1].toInt() shl 8) or (pcm[offset].toInt() and 0xFF)
            var f = s / 32768.0f
            if (f > 1f) {
                f = 1f
            } else if (f < -1f) {
                f = -1f
            }
            out[i] = f
            i++
            offset += 2
        }
        return out
    }
}
