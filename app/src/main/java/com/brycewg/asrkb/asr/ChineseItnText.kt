// 中文 ITN 的文本辅助函数。
package com.brycewg.asrkb.asr

internal object ChineseItnText {
    private val unitSuffixRegex = Regex("(${ChineseItnMappings.commonUnits})$")
    private val letterSuffixRegex = Regex("[A-Za-z]+$")

    fun stripUnit(original: String): Pair<String, String> {
        val unitMatch = unitSuffixRegex.find(original)
        if (unitMatch != null) {
            val unitCn = unitMatch.value
            val mapped = ChineseItnMappings.unitMapping[unitCn]
            val unit = mapped ?: unitCn
            return original.substring(0, unitMatch.range.first).trimAnyWhitespace() to unit
        }

        val letterMatch = letterSuffixRegex.find(original)
        if (letterMatch != null) {
            return original.substring(0, letterMatch.range.first).trimAnyWhitespace() to letterMatch.value
        }

        return original.trimAnyWhitespace() to ""
    }

    fun stripWhitespace(input: String): String = input.filterNot { isAnyWhitespace(it) }

    fun String.trimAnyWhitespace(): String {
        if (isEmpty()) return this
        var start = 0
        while (start < length && isAnyWhitespace(this[start])) start++
        var end = length
        while (end > start && isAnyWhitespace(this[end - 1])) end--
        return substring(start, end)
    }

    fun convertPureNum(original: String, strict: Boolean = false): String? {
        val (stripped, unit) = stripUnit(original)
        if (stripped == "一" && !strict) return original
        if (stripped.isEmpty()) return null
        val converted = StringBuilder(stripped.length)
        for (ch in stripped) {
            converted.append(ChineseItnMappings.numMapper[ch] ?: return null)
        }
        return converted.toString() + unit
    }

    fun normalizeArabicDecimalDotSpacing(input: String): String {
        if (input.isEmpty() || (!input.contains('.') && !input.contains('．'))) return input

        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch in '0'..'9') {
                out.append(ch)
                val afterDigit = i + 1
                var dotIndex = afterDigit
                while (dotIndex < input.length && isAnyWhitespace(input[dotIndex])) dotIndex++
                if (dotIndex < input.length && (input[dotIndex] == '.' || input[dotIndex] == '．')) {
                    var nextDigit = dotIndex + 1
                    while (nextDigit < input.length && isAnyWhitespace(input[nextDigit])) nextDigit++
                    if (nextDigit < input.length && input[nextDigit] in '0'..'9') {
                        out.append('.')
                        out.append(input[nextDigit])
                        i = nextDigit + 1
                        continue
                    }
                }
                i = afterDigit
                continue
            }
            out.append(ch)
            i++
        }
        return out.toString()
    }

    private fun isAnyWhitespace(ch: Char): Boolean = ch.isWhitespace() || Character.isSpaceChar(ch)
}
