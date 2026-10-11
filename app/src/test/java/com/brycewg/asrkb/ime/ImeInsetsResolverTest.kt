// IME 底部系统 inset 解析的 JVM 回归测试。
package com.brycewg.asrkb.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeInsetsResolverTest {
    @Test
    fun allZeroInsetsDoNotFallbackToNavigationBarFrameHeight() {
        val result = resolve(
            nav = 0,
            mandatory = 0,
            tappable = 0,
            systemGestures = 0
        )

        assertEquals(0, result)
    }

    @Test
    fun navigationBarsInsetWinsWhenPresent() {
        val result = resolve(
            nav = 120,
            mandatory = 160,
            tappable = 80,
            systemGestures = 200
        )

        assertEquals(120, result)
    }

    @Test
    fun tappableElementIsUsedWhenNavigationBarInsetIsMissing() {
        val result = resolve(
            nav = 0,
            mandatory = 160,
            tappable = 72,
            systemGestures = 200
        )

        assertEquals(72, result)
    }

    @Test
    fun smallGestureInsetsAreIgnored() {
        val result = resolve(
            nav = 0,
            mandatory = 24,
            tappable = 0,
            systemGestures = 32,
            threshold = 40
        )

        assertEquals(0, result)
    }

    @Test
    fun largeMandatoryGestureInsetIsUsedAsCompatibilitySignal() {
        val result = resolve(
            nav = 0,
            mandatory = 56,
            tappable = 0,
            systemGestures = 32,
            threshold = 40
        )

        assertEquals(56, result)
    }

    @Test
    fun largeSystemGestureInsetIsUsedWhenOtherInsetsAreMissing() {
        val result = resolve(
            nav = 0,
            mandatory = 0,
            tappable = 0,
            systemGestures = 64,
            threshold = 40
        )

        assertEquals(64, result)
    }

    private fun resolve(
        nav: Int,
        mandatory: Int,
        tappable: Int,
        systemGestures: Int,
        threshold: Int = 40
    ) = ImeInsetsResolver.resolveBottomInset(
        navBarsBottom = nav,
        mandatoryBottom = mandatory,
        tappableBottom = tappable,
        systemGesturesBottom = systemGestures,
        gestureInsetThresholdPx = threshold
    )
}
