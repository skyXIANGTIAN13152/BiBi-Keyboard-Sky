// 中文 ITN 的中文数字序列解析器。
package com.brycewg.asrkb.asr

internal object ChineseItnSequenceParser {
    val basicNumericTypes: Set<ChineseItnTokenType> = setOf(
        ChineseItnTokenType.DIGIT,
        ChineseItnTokenType.TEN,
        ChineseItnTokenType.HUNDRED,
        ChineseItnTokenType.THOUSAND,
        ChineseItnTokenType.TEN_THOUSAND,
        ChineseItnTokenType.HUNDRED_MILLION,
        ChineseItnTokenType.ZERO,
        ChineseItnTokenType.DOT
    )

    private data class ParseResult(val value: Long, val consumed: Int)

    fun parseSequence(text: String): String? {
        var (stripped, unit) = ChineseItnText.stripUnit(text)
        if (unit == "万" || unit == "亿") {
            stripped = text
            unit = ""
        }
        if (stripped.isEmpty()) return null

        var tokens = ChineseItnLexer.tokenize(stripped)
        if (tokens.isEmpty()) return null

        if (tokens.last().type == ChineseItnTokenType.OTHER) {
            var endIndex = tokens.size
            while (endIndex > 0 && tokens[endIndex - 1].type == ChineseItnTokenType.OTHER) {
                endIndex--
            }
            val otherUnit = tokens.subList(endIndex, tokens.size).joinToString("") { it.text }
            tokens = tokens.subList(0, endIndex)
            unit = otherUnit + unit
        }
        if (tokens.isEmpty()) return null

        if (tokens.last().type in setOf(
                ChineseItnTokenType.TEN_THOUSAND,
                ChineseItnTokenType.HUNDRED_MILLION
            )
        ) {
            val displayUnit = tokens.last().text
            val numbers = parseTokens(tokens.dropLast(1))
            if (numbers != null) {
                return numbers.joinToString(" ") + displayUnit + unit
            }
        }

        val numbers = parseTokens(tokens) ?: return null
        return numbers.joinToString(" ") + unit
    }

    fun parseTokens(tokens: List<ChineseItnToken>): List<String>? {
        if (tokens.isEmpty()) return null
        if (!tokens.all { it.type in basicNumericTypes }) return null

        val numbers = ArrayList<String>()
        var i = 0
        while (i < tokens.size) {
            val result = buildNumber(tokens, i) ?: return null
            var valueText = result.value.toString()
            var consumed = result.consumed

            val dotIndex = i + consumed
            if (dotIndex < tokens.size && tokens[dotIndex].type == ChineseItnTokenType.DOT) {
                var k = dotIndex + 1
                val decimal = StringBuilder()
                val decimalTypes = setOf(ChineseItnTokenType.DIGIT, ChineseItnTokenType.ZERO)
                while (k < tokens.size && tokens[k].type in decimalTypes) {
                    decimal.append(tokens[k].value)
                    k++
                }
                if (k == dotIndex + 1) return null
                valueText = if (decimal.isNotEmpty()) {
                    "${result.value}.$decimal"
                } else {
                    "${result.value}."
                }
                consumed = k - i
            }

            numbers.add(valueText)
            i += consumed
        }
        return numbers
    }

    private fun parseAtomic(tokens: List<ChineseItnToken>, i: Int): ParseResult? {
        if (i >= tokens.size) return null
        val token = tokens[i]

        if (token.type == ChineseItnTokenType.DIGIT) {
            val digit = token.value
            if (i + 1 < tokens.size) {
                when (tokens[i + 1].type) {
                    ChineseItnTokenType.HUNDRED_MILLION -> return ParseResult(digit * 100000000L, 2)
                    ChineseItnTokenType.TEN_THOUSAND -> return ParseResult(digit * 10000L, 2)
                    ChineseItnTokenType.THOUSAND -> return ParseResult(digit * 1000L, 2)
                    ChineseItnTokenType.HUNDRED -> {
                        var base = digit * 100L
                        var consumed = 2
                        var j = i + 2
                        if (j < tokens.size && tokens[j].type == ChineseItnTokenType.ZERO) {
                            if (j + 1 < tokens.size && tokens[j + 1].type == ChineseItnTokenType.DIGIT) {
                                base += tokens[j + 1].value
                                consumed += 2
                            }
                        } else if (j < tokens.size && tokens[j].type == ChineseItnTokenType.DIGIT) {
                            val tensDigit = tokens[j].value
                            if (j + 1 < tokens.size && tokens[j + 1].type == ChineseItnTokenType.TEN) {
                                base += tensDigit * 10L
                                consumed += 2
                                j += 2
                                if (j < tokens.size && tokens[j].type == ChineseItnTokenType.DIGIT) {
                                    base += tokens[j].value
                                    consumed += 1
                                }
                            } else {
                                base += tensDigit * 10L
                                consumed += 1
                            }
                        }
                        return ParseResult(base, consumed)
                    }
                    else -> Unit
                }
            }

            if (i + 2 < tokens.size &&
                tokens[i + 1].type == ChineseItnTokenType.TEN &&
                tokens[i + 2].type == ChineseItnTokenType.DIGIT
            ) {
                if (!(i + 3 < tokens.size && tokens[i + 3].type == ChineseItnTokenType.TEN)) {
                    return ParseResult(10L * digit + tokens[i + 2].value, 3)
                }
            }

            if (i + 1 < tokens.size && tokens[i + 1].type == ChineseItnTokenType.TEN) {
                if (!(i > 0 && tokens[i - 1].type == ChineseItnTokenType.DIGIT)) {
                    return ParseResult(10L * digit, 2)
                }
            }

            return ParseResult(digit, 1)
        }

        if (token.type == ChineseItnTokenType.TEN) {
            if (i + 2 < tokens.size &&
                tokens[i + 1].type == ChineseItnTokenType.DIGIT &&
                tokens[i + 2].type == ChineseItnTokenType.HUNDRED_MILLION
            ) {
                return ParseResult((10L + tokens[i + 1].value) * 100000000L, 3)
            }
            if (i + 1 < tokens.size && tokens[i + 1].type == ChineseItnTokenType.HUNDRED_MILLION) {
                return ParseResult(10L * 100000000L, 2)
            }
            if (i + 1 < tokens.size && tokens[i + 1].type == ChineseItnTokenType.DIGIT) {
                return ParseResult(10L + tokens[i + 1].value, 2)
            }
            return ParseResult(10L, 1)
        }

        if (token.type == ChineseItnTokenType.ZERO) return ParseResult(0L, 1)
        if (token.type in setOf(
                ChineseItnTokenType.HUNDRED,
                ChineseItnTokenType.THOUSAND,
                ChineseItnTokenType.TEN_THOUSAND,
                ChineseItnTokenType.HUNDRED_MILLION
            )
        ) {
            return ParseResult(token.value, 1)
        }
        return null
    }

    private fun buildNumber(tokens: List<ChineseItnToken>, i: Int): ParseResult? {
        val first = parseAtomic(tokens, i) ?: return null
        var value = first.value
        var consumed = first.consumed
        var j = i + consumed

        if (j < tokens.size) {
            when (tokens[j].type) {
                ChineseItnTokenType.TEN_THOUSAND -> if (value in 1 until 10000) {
                    value *= 10000L
                    consumed += 1
                    j += 1
                }
                ChineseItnTokenType.HUNDRED_MILLION -> if (value in 1 until 100000000) {
                    value *= 100000000L
                    consumed += 1
                    j += 1
                }
                else -> Unit
            }
        }

        val limit = when {
            value >= 10000L -> 10000L
            value >= 1000L -> 1000L
            else -> null
        }

        if (limit != null) {
            var skippedZero = false
            while (j < tokens.size) {
                if (tokens[j].type == ChineseItnTokenType.ZERO) {
                    skippedZero = true
                    consumed += 1
                    j += 1
                    continue
                }
                val chunk = parseAtomic(tokens, j) ?: break
                if (chunk.value >= limit) break
                value += if (!skippedZero && isSingleDigitChunk(tokens, j, chunk)) {
                    chunk.value * (limit / 10L)
                } else {
                    chunk.value
                }
                consumed += chunk.consumed
                j += chunk.consumed
                skippedZero = false
            }
        }

        if (j < tokens.size) {
            when (tokens[j].type) {
                ChineseItnTokenType.TEN_THOUSAND -> if (value in 1 until 10000) {
                    value *= 10000L
                    consumed += 1
                }
                ChineseItnTokenType.HUNDRED_MILLION -> if (value in 1 until 100000000) {
                    value *= 100000000L
                    consumed += 1
                }
                else -> Unit
            }
        }

        return ParseResult(value, consumed)
    }

    private fun isSingleDigitChunk(
        tokens: List<ChineseItnToken>,
        index: Int,
        chunk: ParseResult
    ): Boolean = chunk.consumed == 1 && tokens[index].type == ChineseItnTokenType.DIGIT
}
