/**
 * FireRedASR 伪流式预览委托：负责分段预览、整段终识别与模型就绪检查。
 *
 * 归属模块：asr
 */
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

internal class FireRedAsrPseudoStreamDelegate(
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

    fun onSessionStart(sessionId: Long) {
        previewJob?.cancel()
        previewJob = null
        loadLog?.cancel("New pseudo stream session")
        loadLog = null
        activeSessionId = sessionId
        previewSegments = mutableListOf()
    }

    @Volatile
    private var loadLog: LocalAsrCallLogger.Session? = null

    fun ensureReady(): Boolean {
        val manager = FireRedAsrOnnxManager.getInstance()
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

            val useItn = try {
                prefs.frUseItn
            } catch (t: Throwable) {
                Log.w(tag, "Failed to get frUseItn for segment", t)
                false
            }
            val segmentText = if (useItn) ChineseItn.normalize(trimmed) else trimmed
            val normalizedSegment = try {
                TextSanitizer.trimTrailingPunctAndEmoji(segmentText)
            } catch (t: Throwable) {
                Log.w(
                    tag,
                    "trimTrailingPunctAndEmoji failed for segment, fallback to raw trimmed text",
                    t
                )
                segmentText
            }
            if (normalizedSegment.isEmpty()) return@launch

            try {
                previewMutex.withLock {
                    val segments = previewSegments
                    if (sessionId != activeSessionId) return@withLock
                    segments.add(normalizedSegment)
                    val merged = segments.joinToString(separator = "")
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
            vendor = AsrVendor.FireRedAsr,
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
                val useItn = try {
                    prefs.frUseItn
                } catch (t: Throwable) {
                    Log.w(tag, "Failed to get frUseItn for final", t)
                    false
                }
                val normalized = if (useItn) ChineseItn.normalize(raw) else raw
                val numThreads = try {
                    prefs.frNumThreads
                } catch (t: Throwable) {
                    Log.w(tag, "Failed to get frNumThreads for final", t)
                    1
                }
                val finalText = try {
                    SherpaPunctuationManager.getInstance().addOfflinePunctuation(
                        context = context,
                        text = normalized,
                        numThreads = numThreads
                    )
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to apply offline punctuation", t)
                    normalized
                }
                if (sessionId != activeSessionId) return
                localLog.successWithText(finalText)
                try {
                    listener.onFinal(finalText)
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
        val ui = listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi
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
        val ui = listener as? SenseVoiceFileAsrEngine.LocalModelLoadUi
        if (ui != null) {
            try {
                ui.onLocalModelLoadDone()
            } catch (t: Throwable) {
                Log.e(tag, "Failed to notify load done", t)
            }
        }
    }

    private suspend fun decodeOnce(pcm: ByteArray, reportErrorToUser: Boolean): String? {
        val manager = FireRedAsrOnnxManager.getInstance()
        if (!manager.isOnnxAvailable()) {
            if (reportErrorToUser) {
                try {
                    listener.onError(context.getString(R.string.error_local_asr_not_ready))
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to send not-ready error", t)
                }
            } else {
                Log.w(tag, "FireRedASR model not available")
            }
            return null
        }

        val modelFilesCheck = checkFireRedAsrModelFiles(context, prefs)
        val modelFiles = (modelFilesCheck as? LocalModelCheck.Ready)?.value
        if (modelFiles == null) {
            if (reportErrorToUser) {
                try {
                    listener.onError(
                        localModelErrorMessage(
                            context,
                            modelFilesCheck,
                            R.string.error_firered_asr_model_missing
                        )
                    )
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify model-missing error", t)
                }
            } else {
                Log.w(tag, "FireRedASR model directory missing")
            }
            return null
        }

        val samples = fireRedAsrPcmToFloatArray(pcm)
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
            prefs.frKeepAliveMinutes
        } catch (t: Throwable) {
            Log.w(tag, "Failed to get keep alive minutes", t)
            -1
        }
        val keepMs = if (keepMinutes <= 0) 0L else keepMinutes.toLong() * 60_000L
        val alwaysKeep = keepMinutes < 0

        val text = manager.decodeOffline(
            assetManager = null,
            tokens = modelFiles.tokensPath,
            ctcModel = modelFiles.ctcModelPath,
            encoder = modelFiles.encoderPath,
            decoder = modelFiles.decoderPath,
            provider = "cpu",
            numThreads = try {
                prefs.frNumThreads
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
                    vendor = AsrVendor.FireRedAsr,
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

        val sanitizedText = sanitizeFireRedAsrResult(text)
        if (sanitizedText.isEmpty()) {
            if (reportErrorToUser) {
                try {
                    listener.onError(context.getString(R.string.error_asr_empty_result))
                } catch (t: Throwable) {
                    Log.e(tag, "Failed to notify empty result error", t)
                }
            }
            return null
        }

        return sanitizedText
    }
}
