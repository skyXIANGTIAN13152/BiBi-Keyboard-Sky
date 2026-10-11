/**
 * FireRedASR V2 麦克风伪流式引擎：按固定分片提供 onPartial 预览，结束时输出最终结果。
 *
 * 归属模块：asr
 */
package com.brycewg.asrkb.asr

import android.content.Context
import com.brycewg.asrkb.store.Prefs
import kotlinx.coroutines.CoroutineScope

internal class FireRedAsrPseudoStreamAsrEngine(
    context: Context,
    scope: CoroutineScope,
    prefs: Prefs,
    listener: StreamingAsrEngine.Listener,
    onRequestDuration: ((Long) -> Unit)? = null
) : LocalModelPseudoStreamAsrEngine(context, scope, prefs, listener, onRequestDuration) {

    companion object {
        private const val TAG = "FireRedPseudoStream"
    }

    private val delegate = FireRedAsrPseudoStreamDelegate(
        context = context,
        scope = scope,
        prefs = prefs,
        listener = listener,
        sampleRate = sampleRate,
        onRequestDuration = onRequestDuration,
        tag = TAG
    )

    override fun ensureReady(): Boolean = delegate.ensureReady()

    override fun onSessionStart(sessionId: Long) {
        delegate.onSessionStart(sessionId)
    }

    override fun onSegmentBoundary(sessionId: Long, pcmSegment: ByteArray) {
        delegate.onSegmentBoundary(sessionId, pcmSegment)
    }

    override suspend fun onSessionFinished(sessionId: Long, fullPcm: ByteArray) {
        delegate.onSessionFinished(sessionId, fullPcm)
    }
}
