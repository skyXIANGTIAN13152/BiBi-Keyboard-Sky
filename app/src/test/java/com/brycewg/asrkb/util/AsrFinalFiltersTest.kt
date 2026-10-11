// 识别结果末处理阈值逻辑的 JVM 回归测试。
package com.brycewg.asrkb.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsrFinalFiltersTest {
    @Test
    fun thresholdZeroOnlyMatchesEmptyEffectiveText() {
        assertTrue(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(true, 0, 0))
        assertFalse(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(true, 1, 0))
    }

    @Test
    fun thresholdNinetyNineKeepsLongerText() {
        assertTrue(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(true, 99, 99))
        assertFalse(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(true, 100, 99))
    }

    @Test
    fun thresholdOneHundredMeansUnlimited() {
        assertTrue(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(true, 0, 100))
        assertTrue(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(true, 10_000, 100))
    }

    @Test
    fun disabledNeverTrims() {
        assertFalse(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(false, 0, 100))
        assertFalse(AsrFinalFilters.shouldTrimTrailingPunctAndEmoji(false, 10_000, 100))
    }
}
