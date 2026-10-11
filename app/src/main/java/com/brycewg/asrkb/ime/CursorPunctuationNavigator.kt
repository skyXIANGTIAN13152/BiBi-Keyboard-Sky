/**
 * 自定义键盘标点跳转按钮的纯定位逻辑。
 *
 * 归属模块：ime
 */
package com.brycewg.asrkb.ime

internal enum class PunctuationJumpDirection {
    Previous,
    Next
}

private const val SENTENCE_BOUNDARY_PUNCTUATION = "，,。.、；;？?！!～~＃#（）()"

internal fun punctuationJumpTarget(
    beforeCursor: String,
    afterCursor: String,
    direction: PunctuationJumpDirection
): Int? = when (direction) {
    PunctuationJumpDirection.Previous -> previousPunctuationTarget(beforeCursor)
    PunctuationJumpDirection.Next -> nextPunctuationTarget(beforeCursor.length, afterCursor)
}

private fun previousPunctuationTarget(beforeCursor: String): Int? {
    var index = beforeCursor.length - 1
    while (index >= 0) {
        if (isSentenceBoundaryPunctuation(beforeCursor[index])) {
            val target = index + 1
            if (target < beforeCursor.length) return target
        }
        index--
    }
    return null
}

private fun nextPunctuationTarget(beforeLength: Int, afterCursor: String): Int? {
    var index = 0
    while (index < afterCursor.length) {
        if (isSentenceBoundaryPunctuation(afterCursor[index])) {
            val target = beforeLength + index
            if (index > 0) return target
        }
        index++
    }
    return null
}

private fun isSentenceBoundaryPunctuation(ch: Char): Boolean = SENTENCE_BOUNDARY_PUNCTUATION.indexOf(ch) >= 0
