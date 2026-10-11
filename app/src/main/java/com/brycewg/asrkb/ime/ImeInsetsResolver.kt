package com.brycewg.asrkb.ime

import android.content.res.Resources
import androidx.core.view.WindowInsetsCompat

/**
 * 统一解析 IME 底部系统占位，避免不同 ROM 在导航手势/工具条场景下高度漏算。
 */
internal object ImeInsetsResolver {
    private const val GESTURE_INSET_THRESHOLD_DP = 40

    fun resolveBottomInset(insets: WindowInsetsCompat, resources: Resources): Int {
        val density = resources.displayMetrics.density
        val thresholdPx = (GESTURE_INSET_THRESHOLD_DP * density + 0.5f).toInt()
        return resolveBottomInset(
            navBarsBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
            mandatoryBottom = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures()).bottom,
            tappableBottom = insets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom,
            systemGesturesBottom = insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom,
            gestureInsetThresholdPx = thresholdPx
        )
    }

    internal fun resolveBottomInset(
        navBarsBottom: Int,
        mandatoryBottom: Int,
        tappableBottom: Int,
        systemGesturesBottom: Int,
        gestureInsetThresholdPx: Int
    ): Int {
        val nav = navBarsBottom.coerceAtLeast(0)
        val mandatory = mandatoryBottom.coerceAtLeast(0)
        val tappable = tappableBottom.coerceAtLeast(0)
        val gestures = systemGesturesBottom.coerceAtLeast(0)
        val threshold = gestureInsetThresholdPx.coerceAtLeast(0)
        return when {
            nav > 0 -> nav
            tappable > 0 -> tappable
            mandatory > threshold -> mandatory
            gestures > threshold -> gestures
            else -> 0
        }
    }
}
