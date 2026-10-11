// 中文 ITN 的候选片段匹配规则。
package com.brycewg.asrkb.asr

internal object ChineseItnPatterns {
    private const val NUMERIC_START_CHARS = "0-9几零幺一二两三四五六七八九十百千万亿点比"
    private const val NUMERIC_CONTINUE_CHARS = "0-9几零幺一二两三四五六七八九十百千万亿点比年月日号分秒"
    private val unitSuffix = "(?:${ChineseItnMappings.commonUnits}|[A-Za-z]+)"
    private val numericStart = "(?:百分之|千分之|[$NUMERIC_START_CHARS])"
    private val numericPart = "(?:(?:分之)|[$NUMERIC_CONTINUE_CHARS]|[零一二三四五六七八九十]\\s)"
    private val numericCore = "$numericStart$numericPart*"

    val candidateRegex: Regex = Regex("([A-Za-z]\\s*)?($numericCore(?:$unitSuffix)?)")
}
