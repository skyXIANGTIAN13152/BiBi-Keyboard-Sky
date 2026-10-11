// 自定义键盘标点跳转按钮定位逻辑的 JVM 回归测试。
package com.brycewg.asrkb.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CursorPunctuationNavigatorTest {
    @Test
    fun previousJumpStopsAfterPreviousPunctuation() {
        val target = punctuationJumpTarget(
            beforeCursor = "你好，世界",
            afterCursor = "",
            direction = PunctuationJumpDirection.Previous
        )

        assertEquals(3, target)
    }

    @Test
    fun nextJumpStopsBeforeNextPunctuation() {
        val target = punctuationJumpTarget(
            beforeCursor = "你好",
            afterCursor = "世界，后面",
            direction = PunctuationJumpDirection.Next
        )

        assertEquals(4, target)
    }

    @Test
    fun previousJumpSkipsCurrentBoundary() {
        val target = punctuationJumpTarget(
            beforeCursor = "你好，世界。",
            afterCursor = "",
            direction = PunctuationJumpDirection.Previous
        )

        assertEquals(3, target)
    }

    @Test
    fun nextJumpSkipsCurrentBoundary() {
        val target = punctuationJumpTarget(
            beforeCursor = "你好",
            afterCursor = "，世界。再见",
            direction = PunctuationJumpDirection.Next
        )

        assertEquals(5, target)
    }

    @Test
    fun configuredPunctuationMarksAreRecognized() {
        val punctuations = "，,。.、；;？?！!～~＃#（）()"

        punctuations.forEach { ch ->
            assertEquals(
                "previous should recognize '$ch'",
                2,
                punctuationJumpTarget("a${ch}b", "", PunctuationJumpDirection.Previous)
            )
            assertEquals(
                "next should recognize '$ch'",
                2,
                punctuationJumpTarget("a", "b${ch}c", PunctuationJumpDirection.Next)
            )
        }
    }

    @Test
    fun missingPunctuationDoesNotMove() {
        assertNull(punctuationJumpTarget("hello world", "", PunctuationJumpDirection.Previous))
        assertNull(punctuationJumpTarget("hello", " world", PunctuationJumpDirection.Next))
    }
}
