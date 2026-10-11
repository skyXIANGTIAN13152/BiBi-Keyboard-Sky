// 中文 ITN 的词法分析器，将候选片段拆成语义 Token。
package com.brycewg.asrkb.asr

internal enum class ChineseItnTokenType {
    PERCENT_PREFIX,
    PERMILLE_PREFIX,
    FRACTION_SEP,
    RATIO_SEP,
    DOT,
    YEAR_SUFFIX,
    MONTH_SUFFIX,
    DAY_SUFFIX,
    MINUTE_SUFFIX,
    SECOND_SUFFIX,
    ZERO,
    DIGIT,
    TEN,
    HUNDRED,
    THOUSAND,
    TEN_THOUSAND,
    HUNDRED_MILLION,
    OTHER
}

internal data class ChineseItnToken(
    val type: ChineseItnTokenType,
    val value: Long,
    val text: String,
    val pos: Int
)

internal object ChineseItnLexer {
    private val charValueMap = ChineseItnMappings.valueMapper

    fun tokenize(text: String): List<ChineseItnToken> {
        val tokens = ArrayList<ChineseItnToken>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch.isWhitespace() || Character.isSpaceChar(ch)) {
                i++
                continue
            }

            when {
                text.startsWith("百分之", i) -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.PERCENT_PREFIX, 0L, "百分之", i))
                    i += 3
                }
                text.startsWith("千分之", i) -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.PERMILLE_PREFIX, 0L, "千分之", i))
                    i += 3
                }
                text.startsWith("分之", i) -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.FRACTION_SEP, 0L, "分之", i))
                    i += 2
                }
                ch == '比' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.RATIO_SEP, 0L, ch.toString(), i))
                    i++
                }
                ch == '点' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.DOT, 0L, ch.toString(), i))
                    i++
                }
                ch == '年' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.YEAR_SUFFIX, 0L, ch.toString(), i))
                    i++
                }
                ch == '月' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.MONTH_SUFFIX, 0L, ch.toString(), i))
                    i++
                }
                ch == '日' || ch == '号' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.DAY_SUFFIX, 0L, ch.toString(), i))
                    i++
                }
                ch == '分' && isMinuteSuffix(text, i) -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.MINUTE_SUFFIX, 0L, ch.toString(), i))
                    i++
                }
                ch == '秒' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.SECOND_SUFFIX, 0L, ch.toString(), i))
                    i++
                }
                ch == '零' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.ZERO, 0L, ch.toString(), i))
                    i++
                }
                ch == '0' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.ZERO, 0L, ch.toString(), i))
                    i++
                }
                ch in '1'..'9' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.DIGIT, (ch.code - '0'.code).toLong(), ch.toString(), i))
                    i++
                }
                ch in charArrayOf('一', '二', '两', '三', '四', '五', '六', '七', '八', '九', '幺') -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.DIGIT, charValueMap[ch] ?: 0L, ch.toString(), i))
                    i++
                }
                ch == '十' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.TEN, 10L, ch.toString(), i))
                    i++
                }
                ch == '百' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.HUNDRED, 100L, ch.toString(), i))
                    i++
                }
                ch == '千' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.THOUSAND, 1000L, ch.toString(), i))
                    i++
                }
                ch == '万' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.TEN_THOUSAND, 10000L, ch.toString(), i))
                    i++
                }
                ch == '亿' -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.HUNDRED_MILLION, 100000000L, ch.toString(), i))
                    i++
                }
                else -> {
                    tokens.add(ChineseItnToken(ChineseItnTokenType.OTHER, 0L, ch.toString(), i))
                    i++
                }
            }
        }
        return tokens
    }

    private fun isMinuteSuffix(text: String, index: Int): Boolean {
        val nextIndex = index + 1
        if (nextIndex >= text.length) return true
        val next = text[nextIndex]
        return next == '秒' || next in "0-9零幺一二三四五六七八九十"
    }
}
