/**
 * 设置搜索的查询匹配与结果格式化逻辑。
 *
 * 归属模块：ui/settings/search
 */
package com.brycewg.asrkb.ui.settings.search

import java.util.Locale
import kotlin.math.abs

object SettingsSearchMatcher {

    data class Row(
        val entry: SettingsSearchEntry,
        val titleNormalized: String,
        val searchNormalized: String
    )

    fun buildRows(
        entries: List<SettingsSearchEntry>,
        screenTitleProvider: (Int) -> String
    ): List<Row> = entries.map { entry ->
        val screenTitle = runCatching {
            screenTitleProvider(entry.screenTitleResId)
        }.getOrNull().orEmpty()
        val sectionPath = entry.sectionPath.filter { it.isNotBlank() }
        val searchText = buildString {
            append(entry.title)
            for (path in sectionPath) {
                append(' ')
                append(path)
            }
            if (screenTitle.isNotBlank()) {
                append(' ')
                append(screenTitle)
            }
            for (keyword in entry.keywords) {
                if (keyword.isNotBlank()) {
                    append(' ')
                    append(keyword)
                }
            }
        }
        Row(
            entry = entry,
            titleNormalized = normalizeForSearch(entry.title),
            searchNormalized = normalizeForSearch(searchText)
        )
    }

    fun filter(rows: List<Row>, query: String): List<SettingsSearchEntry> {
        val queryParts = QueryParts.from(query.trim())
        return if (queryParts.normalizedAll.isBlank()) {
            rows.map { it.entry }
        } else {
            rows
                .asSequence()
                .mapNotNull { row ->
                    val score = matchScore(row, queryParts) ?: return@mapNotNull null
                    row.entry to score
                }
                .sortedWith(
                    compareBy<Pair<SettingsSearchEntry, Int>>(
                        { it.second },
                        { it.first.sectionPath.size },
                        { it.first.title }
                    )
                )
                .map { it.first }
                .toList()
        }
    }

    fun subtitle(entry: SettingsSearchEntry, screenTitleProvider: (Int) -> String): String {
        val screenTitle = screenTitleProvider(entry.screenTitleResId)
        return if (entry.sectionPath.isEmpty()) {
            screenTitle
        } else {
            buildString {
                append(screenTitle)
                for (path in entry.sectionPath) {
                    if (path.isBlank()) continue
                    append(" \u2192 ")
                    append(path)
                }
            }
        }
    }

    private fun normalizeForSearch(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

    private data class QueryParts(
        val normalizedAll: String,
        val normalizedTerms: List<String>
    ) {
        companion object {
            fun from(raw: String): QueryParts {
                val terms = raw
                    .split(Regex("\\s+"))
                    .asSequence()
                    .map { normalizeForSearch(it) }
                    .filter { it.isNotBlank() }
                    .toList()
                val all = normalizeForSearch(raw)
                return QueryParts(
                    normalizedAll = all,
                    normalizedTerms = terms.ifEmpty {
                        listOf(all)
                    }.filter { it.isNotBlank() }
                )
            }
        }
    }

    private fun matchScore(row: Row, query: QueryParts): Int? {
        if (query.normalizedAll.isBlank()) return 0

        var score = 0
        for (term in query.normalizedTerms) {
            val termScore = matchTermScore(row, term) ?: return null
            score += termScore
        }

        if (row.titleNormalized == query.normalizedAll) {
            score -= 6
        } else if (row.titleNormalized.startsWith(query.normalizedAll)) {
            score -= 4
        } else if (row.titleNormalized.contains(query.normalizedAll)) {
            score -= 2
        }

        return score.coerceAtLeast(0)
    }

    private fun matchTermScore(row: Row, term: String): Int? {
        if (term.isBlank()) return 0

        val title = row.titleNormalized
        val all = row.searchNormalized

        if (title == term) return 0
        if (title.startsWith(term)) return 1
        if (title.contains(term)) return 2
        if (all.contains(term)) return 6

        if (term.length >= 2) {
            if (containsEditDistanceAtMostOne(title, term)) return 7
            if (containsEditDistanceAtMostOne(all, term)) return 11
        }

        if (term.length >= 3 && isSubsequence(term, title)) {
            return 14 + (title.length - term.length).coerceAtLeast(0)
        }
        if (term.length >= 3 && isSubsequence(term, all)) {
            return 18 + (all.length - term.length).coerceAtLeast(0)
        }
        return null
    }

    private fun containsEditDistanceAtMostOne(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return true

        val needleLength = needle.length
        val lengths = intArrayOf(needleLength - 1, needleLength, needleLength + 1)
        for (len in lengths) {
            if (len <= 0 || len > haystack.length) continue
            for (start in 0..(haystack.length - len)) {
                if (isEditDistanceAtMostOne(needle, haystack, start, len)) return true
            }
        }
        return false
    }

    private fun isEditDistanceAtMostOne(
        needle: String,
        haystack: String,
        start: Int,
        len: Int
    ): Boolean {
        if (abs(needle.length - len) > 1) return false
        if (needle.length == len) {
            var diff = 0
            for (i in 0 until len) {
                if (needle[i] != haystack[start + i]) {
                    diff++
                    if (diff > 1) return false
                }
            }
            return true
        }

        if (needle.length + 1 == len) {
            var i = 0
            var j = 0
            var edits = 0
            while (i < needle.length && j < len) {
                val cNeedle = needle[i]
                val cHaystack = haystack[start + j]
                if (cNeedle == cHaystack) {
                    i++
                    j++
                    continue
                }
                edits++
                if (edits > 1) return false
                j++
            }
            return true
        }

        if (needle.length - 1 == len) {
            var i = 0
            var j = 0
            var edits = 0
            while (i < needle.length && j < len) {
                val cNeedle = needle[i]
                val cHaystack = haystack[start + j]
                if (cNeedle == cHaystack) {
                    i++
                    j++
                    continue
                }
                edits++
                if (edits > 1) return false
                i++
            }
            return true
        }

        return false
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        if (needle.isEmpty()) return true
        var index = 0
        for (char in haystack) {
            if (index < needle.length && needle[index] == char) {
                index++
                if (index == needle.length) return true
            }
        }
        return false
    }
}
