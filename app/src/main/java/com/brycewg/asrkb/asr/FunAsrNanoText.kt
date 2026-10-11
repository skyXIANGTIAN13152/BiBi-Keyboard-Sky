// Modified for Sky Wuji Edition on 2026-10-11; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.asr

// Nano may emit /sil beside Chinese text or by itself for a silent chunk.
// Keep similar words and paths (e.g. /silver or https://example.com/sil).
private val nanoSilenceMarker = Regex(
    """([ \t]*)(?<![A-Za-z0-9_/:\\])(?:/sil)+(?![A-Za-z0-9_/\\-]|\.[A-Za-z0-9])([ \t]*)"""
)

/** Remove decoder silence markers before logging, joining, or delivering text. */
internal fun cleanFunAsrNanoText(text: String): String = nanoSilenceMarker.replace(text) { match ->
    // If a marker separated English words, retain one side's spacing. Leave
    // unrelated whitespace and line breaks alone.
    match.groupValues[1].ifEmpty { match.groupValues[2] }
}.trim()
