// Modified for Sky Wuji Edition on 2026-10-11; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.asr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalOfflinePcmChunkingTest {
    @Test
    fun audioAtSafeLimitStaysSinglePass() {
        val pcm = ByteArray(HARD_CHUNK_BYTES)

        val chunks = splitLocalOfflinePcm16(pcm, SAMPLE_RATE)

        assertEquals(1, chunks.size)
        assertSame(pcm, chunks.single())
    }

    @Test
    fun audioOverSafeLimitIsSplitWithoutLossOrOverlap() {
        val pcm = ByteArray(HARD_CHUNK_BYTES * 2 + 2) { (it % 251).toByte() }

        val chunks = splitLocalOfflinePcm16(pcm, SAMPLE_RATE)

        assertEquals(listOf(HARD_CHUNK_BYTES, HARD_CHUNK_BYTES, 2), chunks.map { it.size })
        assertArrayEquals(pcm, chunks.fold(ByteArray(0)) { all, chunk -> all + chunk })
    }

    @Test
    fun longestSilenceNearTargetIsPreferredOverHardCut() {
        val pcm = ByteArray(bytesForMs(45_000)) {
            (it % 251).toByte()
        }
        val firstSilence = byteRange(startMs = 18_000, endMs = 19_000)
        val secondSilence = byteRange(startMs = 37_000, endMs = 38_000)

        val chunks = splitLocalOfflinePcm16(
            pcm = pcm,
            sampleRate = SAMPLE_RATE,
            silenceRanges = listOf(firstSilence, secondSilence)
        )

        assertEquals(
            listOf(bytesForMs(18_500), bytesForMs(19_000), bytesForMs(7_500)),
            chunks.map { it.size }
        )
        assertArrayEquals(pcm, chunks.fold(ByteArray(0)) { all, chunk -> all + chunk })
    }

    @Test
    fun silenceAfterTwentySecondsCanBeSelected() {
        val pcm = ByteArray(bytesForMs(45_000))

        val chunks = splitLocalOfflinePcm16(
            pcm = pcm,
            sampleRate = SAMPLE_RATE,
            silenceRanges = listOf(byteRange(startMs = 21_000, endMs = 21_500))
        )

        assertEquals(bytesForMs(21_250), chunks.first().size)
    }

    @Test
    fun longestSilenceInWindowWins() {
        val pcm = ByteArray(bytesForMs(45_000))

        val chunks = splitLocalOfflinePcm16(
            pcm = pcm,
            sampleRate = SAMPLE_RATE,
            silenceRanges = listOf(
                byteRange(startMs = 18_000, endMs = 18_400),
                byteRange(startMs = 20_500, endMs = 21_500)
            )
        )

        assertEquals(bytesForMs(21_000), chunks.first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompletePcm16FrameIsRejected() {
        splitLocalOfflinePcm16(ByteArray(3), SAMPLE_RATE)
    }

    @Test
    fun funAsrNanoLongUtterancesKeepEverySampleWithinModelBudget() {
        // Includes the two failing device recordings, the generic 24s chunk,
        // and a two-minute utterance with a partial final chunk.
        for (durationMs in listOf(8_000, 8_001, 18_400, 18_600, 24_000, 121_337)) {
            val pcm = ByteArray(bytesForMs(durationMs)) { (it % 251).toByte() }
            val chunks = splitLocalOfflinePcm16(
                pcm, SAMPLE_RATE,
                maxChunkMs = FUNASR_NANO_MAX_CHUNK_MS,
                minChunkMs = FUNASR_NANO_MIN_CHUNK_MS
            )
            assertTrue(chunks.all { it.isNotEmpty() && it.size <= bytesForMs(8_000) && it.size % 2 == 0 })
            assertArrayEquals(pcm, chunks.fold(ByteArray(0)) { all, chunk -> all + chunk })
        }
    }

    @Test
    fun funAsrNanoShorterBudgetAlsoTriggersSilenceAnalysis() {
        val pcm = ByteArray(bytesForMs(18_400))
        assertFalse(localOfflinePcmNeedsChunking(pcm, SAMPLE_RATE))
        assertTrue(localOfflinePcmNeedsChunking(pcm, SAMPLE_RATE, FUNASR_NANO_MAX_CHUNK_MS))
        val chunks = splitLocalOfflinePcm16(
            pcm, SAMPLE_RATE,
            maxChunkMs = FUNASR_NANO_MAX_CHUNK_MS,
            silenceRanges = listOf(byteRange(6_200, 6_600), byteRange(13_000, 13_400)),
            minChunkMs = FUNASR_NANO_MIN_CHUNK_MS
        )
        assertEquals(listOf(6_400, 6_800, 5_200).map(::bytesForMs), chunks.map { it.size })
        assertArrayEquals(pcm, chunks.fold(ByteArray(0)) { all, chunk -> all + chunk })
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val HARD_CHUNK_BYTES = SAMPLE_RATE * 2 * 24

        fun bytesForMs(durationMs: Int): Int = (SAMPLE_RATE.toLong() * 2 * durationMs / 1_000).toInt()

        fun byteRange(startMs: Int, endMs: Int): IntRange = bytesForMs(startMs) until bytesForMs(endMs)
    }
}
