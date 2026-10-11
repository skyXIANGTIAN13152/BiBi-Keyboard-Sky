// 中文 ITN 的语法规约流水线。
package com.brycewg.asrkb.asr

internal object ChineseItnReducer {
    private val digitTypes = setOf(ChineseItnTokenType.DIGIT, ChineseItnTokenType.ZERO)
    private val unitTypes = setOf(
        ChineseItnTokenType.TEN,
        ChineseItnTokenType.HUNDRED,
        ChineseItnTokenType.THOUSAND,
        ChineseItnTokenType.TEN_THOUSAND,
        ChineseItnTokenType.HUNDRED_MILLION
    )

    fun replace(original: String): String {
        if (ChineseItnMappings.containsIdiom(original) || ChineseItnMappings.isFuzzy(original)) return original

        val tokens = ChineseItnLexer.tokenize(original)
        if (tokens.isEmpty()) return original
        if (tokens.all { it.type in unitTypes } &&
            tokens.size >= 2 &&
            tokens.none { it.type in digitTypes }
        ) {
            return original
        }

        val reducers = listOf(
            ::tryReducePercent,
            ::tryReduceFraction,
            ::tryReduceRatio,
            ::tryReduceDateTime,
            ::tryReduceRange,
            ::tryReduceNumerical
        )
        for (reducer in reducers) {
            val result = reducer(tokens, original)
            if (result != null) return result
        }
        return original
    }

    private fun tryReducePercent(tokens: List<ChineseItnToken>, original: String): String? {
        if (tokens.isEmpty() ||
            tokens.first().type !in setOf(ChineseItnTokenType.PERCENT_PREFIX, ChineseItnTokenType.PERMILLE_PREFIX)
        ) {
            return null
        }
        val valueTokens = tokens.drop(1)
        if (valueTokens.isEmpty() || !allNumeric(valueTokens)) return null
        val values = ChineseItnSequenceParser.parseTokens(valueTokens)
        if (values != null && values.size == 1) {
            val suffix = if (tokens.first().type == ChineseItnTokenType.PERCENT_PREFIX) "%" else "‰"
            return values[0] + suffix
        }
        return null
    }

    private fun tryReduceFraction(tokens: List<ChineseItnToken>, original: String): String? = reduceBinaryOp(tokens, ChineseItnTokenType.FRACTION_SEP) { left, right -> "$right/$left" }

    private fun tryReduceRatio(tokens: List<ChineseItnToken>, original: String): String? = reduceBinaryOp(tokens, ChineseItnTokenType.RATIO_SEP) { left, right -> "$left:$right" }

    private fun tryReduceDateTime(tokens: List<ChineseItnToken>, original: String): String? {
        val splitIndex = tokens.indexOfLast {
            it.type in setOf(
                ChineseItnTokenType.DAY_SUFFIX,
                ChineseItnTokenType.MONTH_SUFFIX,
                ChineseItnTokenType.YEAR_SUFFIX
            )
        }

        if (splitIndex == -1) {
            return tryReduceTime(tokens, original) ?: tryReduceDate(tokens, original)
        }

        val dateResult = tryReduceDate(tokens.take(splitIndex + 1), original) ?: return null
        val timeResult = if (splitIndex + 1 < tokens.size) {
            tryReduceTime(tokens.drop(splitIndex + 1), original) ?: return null
        } else {
            ""
        }
        return dateResult + timeResult
    }

    private fun tryReduceRange(tokens: List<ChineseItnToken>, original: String): String? = ChineseItnRanges.parseRange(original)

    private fun tryReduceNumerical(tokens: List<ChineseItnToken>, original: String): String? {
        var (strippedText, unit) = ChineseItnText.stripUnit(original)
        if (unit == "万" || unit == "亿") {
            strippedText = original
        }
        if (strippedText.isEmpty() || strippedText.endsWith("点")) return null

        val strippedTokens = ChineseItnLexer.tokenize(strippedText)
        if (strippedTokens.isEmpty() || !allNumeric(strippedTokens)) return null

        if (strippedTokens.all { it.type in setOf(ChineseItnTokenType.DIGIT, ChineseItnTokenType.ZERO, ChineseItnTokenType.DOT) }) {
            return ChineseItnText.convertPureNum(original)
        }

        return ChineseItnSequenceParser.parseSequence(original)
    }

    private fun tryReduceTime(tokens: List<ChineseItnToken>, original: String): String? {
        val dotIndices = tokens.indices.filter { tokens[it].type == ChineseItnTokenType.DOT }
        val minuteIndices = tokens.indices.filter { tokens[it].type == ChineseItnTokenType.MINUTE_SUFFIX }
        if (dotIndices.size != 1 || minuteIndices.size != 1) return null

        val dotIndex = dotIndices[0]
        val minuteIndex = minuteIndices[0]
        if (dotIndex >= minuteIndex) return null

        val hourTokens = tokens.take(dotIndex)
        val minuteTokens = tokens.subList(dotIndex + 1, minuteIndex)
        val secondTokens = tokens.drop(minuteIndex + 1)
        if (hourTokens.isEmpty() ||
            minuteTokens.isEmpty() ||
            !allNumeric(hourTokens) ||
            !allNumeric(minuteTokens)
        ) {
            return null
        }

        var secondText = ""
        if (secondTokens.isNotEmpty()) {
            if (secondTokens.last().type != ChineseItnTokenType.SECOND_SUFFIX) return null
            val secondValueTokens = secondTokens.dropLast(1)
            if (secondValueTokens.isEmpty() || !allNumeric(secondValueTokens)) return null
            var secondValues = parseNumericValues(secondValueTokens) ?: return null
            if (secondValues.size == 2 && secondValues[0] == "0") {
                secondValues = listOf(secondValues[1])
            }
            if (secondValues.size != 1) return null
            secondText = padTimeValue(secondValues[0])
        }

        val hourValues = parseNumericValues(hourTokens) ?: return null
        var minuteValues = parseNumericValues(minuteTokens) ?: return null
        if (hourValues.size != 1) return null
        if (minuteValues.size == 2 && minuteValues[0] == "0") {
            minuteValues = listOf(minuteValues[1])
        }
        if (minuteValues.size != 1) return null

        val base = "${padTimeValue(hourValues[0])}:${padTimeValue(minuteValues[0])}"
        return if (secondText.isNotEmpty()) "$base:$secondText" else base
    }

    private fun tryReduceDate(tokens: List<ChineseItnToken>, original: String): String? {
        val yearIndices = tokens.indices.filter { tokens[it].type == ChineseItnTokenType.YEAR_SUFFIX }
        val monthIndices = tokens.indices.filter { tokens[it].type == ChineseItnTokenType.MONTH_SUFFIX }
        val dayIndices = tokens.indices.filter { tokens[it].type == ChineseItnTokenType.DAY_SUFFIX }
        if (yearIndices.size > 1 || monthIndices.size > 1 || dayIndices.size > 1) return null
        if (yearIndices.isEmpty() && monthIndices.isEmpty() && dayIndices.isEmpty()) return null

        val yearIndex = yearIndices.firstOrNull() ?: -1
        val monthIndex = monthIndices.firstOrNull() ?: -1
        val dayIndex = dayIndices.firstOrNull() ?: -1
        val indices = listOf(yearIndex, monthIndex, dayIndex).filter { it != -1 }
        if (indices != indices.sorted()) return null

        var lastIndex = 0
        val result = StringBuilder()

        if (yearIndex != -1) {
            val yearTokens = tokens.subList(lastIndex, yearIndex)
            if (yearTokens.isEmpty() || !allNumeric(yearTokens)) return null
            val year = if (yearTokens.all { it.type in digitTypes }) {
                if (yearTokens.all { it.text.all(Char::isDigit) }) {
                    yearTokens.joinToString("") { it.text }
                } else {
                    ChineseItnText.convertPureNum(yearTokens.joinToString("") { it.text }, strict = true)
                }
            } else {
                parseSingleNumeric(yearTokens)
            } ?: return null
            result.append(year).append("年")
            lastIndex = yearIndex + 1
        }

        if (monthIndex != -1) {
            val month = parseSingleNumeric(tokens.subList(lastIndex, monthIndex)) ?: return null
            result.append(month).append("月")
            lastIndex = monthIndex + 1
        }

        if (dayIndex != -1) {
            val day = parseSingleNumeric(tokens.subList(lastIndex, dayIndex)) ?: return null
            result.append(day).append(tokens[dayIndex].text)
            lastIndex = dayIndex + 1
        }

        if (lastIndex != tokens.size) return null
        return result.toString().takeIf { it.isNotEmpty() }
    }

    private fun reduceBinaryOp(
        tokens: List<ChineseItnToken>,
        separatorType: ChineseItnTokenType,
        format: (left: String, right: String) -> String
    ): String? {
        val indices = tokens.indices.filter { tokens[it].type == separatorType }
        if (indices.size != 1) return null
        val index = indices[0]
        val leftTokens = tokens.take(index)
        val rightTokens = tokens.drop(index + 1)
        if (leftTokens.isEmpty() ||
            rightTokens.isEmpty() ||
            !allNumeric(leftTokens) ||
            !allNumeric(rightTokens)
        ) {
            return null
        }
        val leftValues = ChineseItnSequenceParser.parseTokens(leftTokens)
        val rightValues = ChineseItnSequenceParser.parseTokens(rightTokens)
        if (leftValues != null && leftValues.size == 1 && rightValues != null && rightValues.size == 1) {
            return format(leftValues[0], rightValues[0])
        }
        return null
    }

    private fun allNumeric(tokens: List<ChineseItnToken>): Boolean = tokens.all { it.type in ChineseItnSequenceParser.basicNumericTypes }

    private fun parseSingleNumeric(tokens: List<ChineseItnToken>): String? {
        if (tokens.isEmpty() || !allNumeric(tokens)) return null
        return parseNumericValues(tokens)?.singleOrNull()
    }

    private fun parseNumericValues(tokens: List<ChineseItnToken>): List<String>? {
        if (tokens.isEmpty() || !allNumeric(tokens)) return null
        if (tokens.all { it.text.all(Char::isDigit) }) {
            return listOf(tokens.joinToString("") { it.text })
        }
        return ChineseItnSequenceParser.parseTokens(tokens)
    }

    private fun padTimeValue(value: String): String {
        val dotIndex = value.indexOf('.')
        return if (dotIndex >= 0) {
            value.substring(0, dotIndex).padStart(2, '0') + value.substring(dotIndex)
        } else {
            value.padStart(2, '0')
        }
    }
}
