// 中文 ITN 的静态映射表与成语保护表。
package com.brycewg.asrkb.asr

internal object ChineseItnMappings {
    val unitMapping: Map<String, String?> = linkedMapOf(
        "千米每小时" to "km/h",
        "百分点" to "百分点",
        "千克" to "kg",
        "公斤" to "公斤",
        "公分" to "公分",
        "毫米" to "毫米",
        "毫升" to "毫升",
        "毫克" to "毫克",
        "千米" to "千米",
        "公里" to "公里",
        "克" to "g",
        "元" to "元",
        "吨" to "吨",
        "斤" to "斤",
        "两" to "两",
        "米" to "米",
        "个" to null,
        "只" to null,
        "分" to null,
        "万" to null,
        "亿" to null,
        "秒" to null,
        "年" to null,
        "月" to null,
        "日" to null,
        "号" to null,
        "天" to null,
        "时" to null,
        "钟" to null,
        "人" to null,
        "层" to null,
        "楼" to null,
        "倍" to null,
        "块" to null,
        "次" to null,
        "台" to null,
        "辆" to null,
        "岁" to null,
        "本" to null,
        "张" to null,
        "条" to null,
        "件" to null,
        "位" to null,
        "名" to null,
        "家" to null,
        "套" to null,
        "部" to null,
        "支" to null,
        "瓶" to null,
        "杯" to null,
        "盒" to null,
        "份" to null,
        "根" to null,
        "颗" to null,
        "座" to null,
        "间" to null,
        "架" to null,
        "艘" to null,
        "篇" to null,
        "字" to null,
        "度" to null
    )

    val commonUnits: String = unitMapping.keys
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    val numMapper: Map<Char, Char> = mapOf(
        '零' to '0',
        '一' to '1',
        '幺' to '1',
        '二' to '2',
        '两' to '2',
        '三' to '3',
        '四' to '4',
        '五' to '5',
        '六' to '6',
        '七' to '7',
        '八' to '8',
        '九' to '9',
        '0' to '0',
        '1' to '1',
        '2' to '2',
        '3' to '3',
        '4' to '4',
        '5' to '5',
        '6' to '6',
        '7' to '7',
        '8' to '8',
        '9' to '9',
        '点' to '.'
    )

    val valueMapper: Map<Char, Long> = mapOf(
        '零' to 0L,
        '一' to 1L,
        '幺' to 1L,
        '二' to 2L,
        '两' to 2L,
        '三' to 3L,
        '四' to 4L,
        '五' to 5L,
        '六' to 6L,
        '七' to 7L,
        '八' to 8L,
        '九' to 9L,
        '十' to 10L,
        '百' to 100L,
        '千' to 1000L,
        '万' to 10000L,
        '亿' to 100000000L
    )

    private val idioms: Set<String> = setOf(
        "正经八百", "五零二落", "五零四散", "五十步笑百步", "乌七八糟", "污七八糟", "四百四病", "思绪万千",
        "十有八九", "十之八九", "三十而立", "三十六策", "三十六计", "三十六行", "三五成群", "三百六十行",
        "三六九等", "七老八十", "七零八落", "七零八碎", "七七八八", "乱七八遭", "乱七八糟", "略知一二",
        "零零星星", "零七八碎", "二三其德", "二三其意", "无银三百两", "八九不离十", "百分之百", "年三十",
        "烂七八糟", "一点一滴", "路易十六", "九三学社", "五四运动", "入木三分", "九九八十一", "三七二十一",
        "百思不得其解", "千问", "十二五", "十三五", "十四五", "十五五", "十六五", "十七五", "十八五"
    )

    private val idiomRegex = Regex(
        idioms.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    )

    fun isFuzzy(text: String): Boolean = text.contains('几')

    fun containsIdiom(text: String): Boolean = idioms.any { text.contains(it) }

    fun startsWithKnownUnit(input: String, startIndex: Int): Boolean {
        if (startIndex !in input.indices) return false
        return unitMapping.keys.any { input.startsWith(it, startIndex) }
    }

    fun findIdiomRanges(input: String): List<IntRange> {
        val ranges = idiomRegex.findAll(input).map { it.range }.toList()
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val merged = ArrayList<IntRange>(sorted.size)
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.last + 1) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    fun rangesOverlap(a: IntRange, b: IntRange): Boolean = a.first <= b.last && b.first <= a.last
}
