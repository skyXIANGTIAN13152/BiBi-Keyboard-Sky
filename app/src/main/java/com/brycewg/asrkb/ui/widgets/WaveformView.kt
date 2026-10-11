/**
 * IME 录音波形视图。
 *
 * 归属模块：ui/widgets
 */
package com.brycewg.asrkb.ui.widgets

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import jaygoo.widget.wlv.WaveLineView
import kotlin.math.pow

/**
 * 实时音频波形视图（封装第三方 WaveLineView）
 * - 保持原有 API：start/stop/updateAmplitude/setWaveformColor
 * - 使用 SurfaceView 提供更顺滑的波形动画
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private companion object {
        private const val MIN_UPDATE_INTERVAL_MS = 16L
        private const val MIN_VOLUME_DELTA = 2
    }

    private var isActive = false
    private var cachedGain: Float = computeGain(5)
    private var lastVolume: Int = -1
    private var lastVolumeDispatchUptimeMs: Long = 0L

    /** 波形灵敏度（1-10），数值越大响应越明显 */
    var sensitivity: Int = 5
        set(value) {
            val normalized = value.coerceIn(1, 10)
            if (field == normalized) return
            field = normalized
            cachedGain = computeGain(normalized)
        }

    private val waveView: WaveLineView = WaveLineView(context).apply {
        // 透明背景，融入容器
        setBackGroundColor(Color.TRANSPARENT)
        // 提高灵敏度使波形更明显（范围1-10，10最灵敏）
        setSensibility(10)
        setMoveSpeed(250f)
    }

    init {
        // 让 WaveLineView 充满容器
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(waveView, lp)
        // 初始隐藏由上层控制
        visibility = View.GONE
    }

    fun setWaveformColor(@ColorInt color: Int) {
        waveView.setLineColor(color)
        invalidate()
    }

    /** 更新振幅（0.0 - 1.0） */
    fun updateAmplitude(amplitude: Float) {
        if (!isActive) return
        val boosted = (amplitude * cachedGain).coerceIn(0f, 1f)
        // 映射到 [0,100] 并设置音量，WaveLineView 内部做平滑
        val vol = (boosted * 100f).toInt()
        val now = SystemClock.uptimeMillis()
        if (lastVolume >= 0) {
            val dt = now - lastVolumeDispatchUptimeMs
            if (dt < MIN_UPDATE_INTERVAL_MS && kotlin.math.abs(vol - lastVolume) < MIN_VOLUME_DELTA) {
                return
            }
        }
        lastVolume = vol
        lastVolumeDispatchUptimeMs = now
        waveView.setVolume(vol)
    }

    /** 启动波形动画 */
    fun start() {
        if (isActive) return
        isActive = true
        lastVolume = -1
        lastVolumeDispatchUptimeMs = 0L
        try {
            waveView.startAnim()
        } catch (t: Throwable) {
            android.util.Log.w("WaveformView", "startAnim failed", t)
        }
    }

    /** 停止波形动画 */
    fun stop() {
        if (!isActive) return
        isActive = false
        lastVolume = -1
        lastVolumeDispatchUptimeMs = 0L
        try {
            waveView.stopAnim()
        } catch (t: Throwable) {
            android.util.Log.w("WaveformView", "stopAnim failed", t)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        try {
            waveView.onWindowFocusChanged(hasWindowFocus)
        } catch (t: Throwable) {
            android.util.Log.w("WaveformView", "onWindowFocusChanged proxy failed", t)
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView == this && visibility != View.VISIBLE) {
            if (isActive) stop()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            stop()
            waveView.release()
        } catch (t: Throwable) {
            android.util.Log.w("WaveformView", "release failed", t)
        }
    }

    private fun computeGain(sensitivity: Int): Float = 0.25f * (48.0).pow((sensitivity - 1) / 9.0).toFloat()
}
