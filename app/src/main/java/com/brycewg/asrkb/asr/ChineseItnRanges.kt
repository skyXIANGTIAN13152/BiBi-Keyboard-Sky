// 中文 ITN 的口语范围表达式解析。
package com.brycewg.asrkb.asr

internal object ChineseItnRanges {
    private val numericUnits = setOf("万", "亿", "千", "百", "十")
    private val allowedRangeTypes = setOf(
        ChineseItnTokenType.DIGIT,
        ChineseItnTokenType.TEN,
        ChineseItnTokenType.HUNDRED,
        ChineseItnTokenType.THOUSAND,
        ChineseItnTokenType.TEN_THOUSAND,
        ChineseItnTokenType.HUNDRED_MILLION,
        ChineseItnTokenType.ZERO
    )

    fun parseRange(text: String): String? {
        if (text.contains('点')) return null
        val (strippedText, mappedUnit) = stripPhysicalUnit(text)
        if (strippedText.isEmpty()) return null
        parseTensWithTwoUnitDigits(strippedText)?.let { return it + mappedUnit }

        val tokens = ChineseItnLexer.tokenize(strippedText)
        if (tokens.isEmpty() || !tokens.all { it.type in allowedRangeTypes }) return null

        val runs = ArrayList<Triple<Int, Int, List<ChineseItnToken>>>()
        var currentRun = ArrayList<ChineseItnToken>()
        var startIndex = -1
        for ((index, token) in tokens.withIndex()) {
            if (token.type == ChineseItnTokenType.DIGIT) {
                if (currentRun.isEmpty()) startIndex = index
                currentRun.add(token)
            } else if (currentRun.isNotEmpty()) {
                runs.add(Triple(startIndex, index, currentRun.toList()))
                currentRun = ArrayList()
            }
        }
        if (currentRun.isNotEmpty()) {
            runs.add(Triple(startIndex, tokens.size, currentRun.toList()))
        }

        val len2Runs = runs.filter { it.third.size == 2 }
        val largeRuns = runs.filter { it.third.size > 2 }
        if (len2Runs.size != 1 || largeRuns.isNotEmpty()) return null

        val (coreStart, coreEnd, coreTokens) = len2Runs[0]
        val d1 = coreTokens[0]
        val d2 = coreTokens[1]
        val v1 = d1.value
        val v2 = d2.value
        if (!(v1 < v2 && (v2 - v1 == 1L || (v1 == 3L && v2 == 5L)))) return null

        val baseTokens = tokens.subList(0, coreStart)
        val suffixTokens = tokens.subList(coreEnd, tokens.size)

        if (baseTokens.isEmpty()) {
            if (suffixTokens.isEmpty()) return "$v1~$v2$mappedUnit"
            val unitToken = suffixTokens.first()
            if (unitToken.type !in setOf(
                    ChineseItnTokenType.TEN,
                    ChineseItnTokenType.HUNDRED,
                    ChineseItnTokenType.THOUSAND,
                    ChineseItnTokenType.TEN_THOUSAND,
                    ChineseItnTokenType.HUNDRED_MILLION
                )
            ) {
                return null
            }

            val suffixUnitTokens = suffixTokens.drop(1)
            if (!suffixUnitTokens.all {
                    it.type in setOf(ChineseItnTokenType.TEN_THOUSAND, ChineseItnTokenType.HUNDRED_MILLION)
                }
            ) {
                return null
            }

            val unit = unitToken.text
            val suffixUnit = suffixUnitTokens.joinToString("") { it.text }
            return when (unit) {
                "十" -> "${v1 * 10L}~${v2 * 10L}$suffixUnit$mappedUnit"
                "万", "亿" -> "$v1~$v2$unit$suffixUnit$mappedUnit"
                "千" -> if (suffixUnit.isNotEmpty()) {
                    "$v1~$v2$unit$suffixUnit$mappedUnit"
                } else {
                    "${v1 * 1000L}~${v2 * 1000L}$mappedUnit"
                }
                else -> "${v1 * unitToken.value}~${v2 * unitToken.value}$suffixUnit$mappedUnit"
            }
        }

        val baseValue = ChineseItnSequenceParser.parseTokens(baseTokens)?.singleOrNull()?.toLongOrNull() ?: return null
        val baseString = baseTokens.joinToString("") { it.text }
        val multiplier: Long
        val suffixString: String

        if (suffixTokens.firstOrNull()?.type == ChineseItnTokenType.TEN) {
            val suffixUnitTokens = suffixTokens.drop(1)
            if (!suffixUnitTokens.all {
                    it.type in setOf(ChineseItnTokenType.TEN_THOUSAND, ChineseItnTokenType.HUNDRED_MILLION)
                }
            ) {
                return null
            }
            multiplier = 10L
            suffixString = suffixUnitTokens.joinToString("") { it.text }
        } else {
            if (!suffixTokens.all {
                    it.type in setOf(ChineseItnTokenType.TEN_THOUSAND, ChineseItnTokenType.HUNDRED_MILLION)
                }
            ) {
                return null
            }
            val lastChar = baseString.lastOrNull() ?: return null
            multiplier = (ChineseItnMappings.valueMapper[lastChar] ?: 10L) / 10L
            suffixString = suffixTokens.joinToString("") { it.text }
        }

        val left = baseValue + v1 * multiplier
        val right = baseValue + v2 * multiplier
        return "$left~$right$suffixString$mappedUnit"
    }

    private fun parseTensWithTwoUnitDigits(text: String): String? {
        val match = Regex("^([一二三四五六七八九])十([一二三四五六七八九])([一二三四五六七八九])$").matchEntire(text)
            ?: return null
        val tens = ChineseItnMappings.valueMapper[match.groupValues[1].first()] ?: return null
        val leftDigit = ChineseItnMappings.valueMapper[match.groupValues[2].first()] ?: return null
        val rightDigit = ChineseItnMappings.valueMapper[match.groupValues[3].first()] ?: return null
        if (leftDigit >= rightDigit || rightDigit - leftDigit != 1L) return null
        val base = tens * 10L
        return "${base + leftDigit}~${base + rightDigit}"
    }

    private fun stripPhysicalUnit(text: String): Pair<String, String> {
        for (unitCn in ChineseItnMappings.unitMapping.keys.sortedByDescending { it.length }) {
            if (unitCn in numericUnits) continue
            if (text.endsWith(unitCn)) {
                val mapped = ChineseItnMappings.unitMapping[unitCn] ?: unitCn
                return text.substring(0, text.length - unitCn.length) to mapped
            }
        }
        return text to ""
    }
}
