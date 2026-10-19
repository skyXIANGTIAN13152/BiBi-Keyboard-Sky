// Modified for Sky Wuji Edition on 2026-10-20; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.asr

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoProgressiveChunkingTest {
    @Test
    fun nanoRemembersSixSecondPauseButWaitsUntilOverEightSecondsBeforeEmitting() {
        val chunker = nanoChunker()

        assertTrue(chunker.append(pcm(6_000), isSpeech = true).isEmpty())
        assertTrue(chunker.append(pcm(200), isSpeech = false).isEmpty())
        assertTrue(chunker.append(pcm(200), isSpeech = false).isEmpty())
        assertTrue(chunker.append(pcm(1_400), isSpeech = true).isEmpty())
        assertTrue(chunker.append(pcm(200), isSpeech = true).isEmpty())
        val chunks = chunker.append(pcm(200), isSpeech = true)

        assertEquals(listOf(bytesForMs(6_200)), chunks.map { it.size })
        assertEquals(bytesForMs(2_000), chunker.finish()!!.size)
        assertNull(chunker.finish())
    }

    @Test
    fun silenceCrossingSixSecondsCannotCreateAnEarlyShortChunk() {
        val chunker = nanoChunker()
        assertTrue(chunker.append(pcm(5_800), isSpeech = true).isEmpty())
        assertTrue(chunker.append(pcm(400), isSpeech = false).isEmpty())

        assertTrue(chunker.append(pcm(1_800), isSpeech = true).isEmpty())
        val chunks = chunker.append(pcm(200), isSpeech = true)

        // Only the 6.0s–6.2s portion of this pause lies within the eligible cut window.
        assertEquals(listOf(bytesForMs(6_100)), chunks.map { it.size })
        assertEquals(bytesForMs(2_100), chunker.finish()!!.size)
    }

    @Test
    fun utterancesUpToEightSecondsWithPauseStayWholeOnStop() {
        for (durationMs in listOf(7_800, 8_000)) {
            val chunker = nanoChunker()
            val input = patternedPcm(bytesForMs(durationMs))

            assertTrue(chunker.append(input.copyOfRange(0, bytesForMs(6_000)), isSpeech = true).isEmpty())
            assertTrue(
                chunker.append(input.copyOfRange(bytesForMs(6_000), bytesForMs(6_400)), isSpeech = false).isEmpty()
            )
            assertTrue(
                chunker.append(input.copyOfRange(bytesForMs(6_400), input.size), isSpeech = true).isEmpty()
            )

            assertArrayEquals("Keep ${durationMs}ms whole", input, chunker.finish())
            assertNull(chunker.finish())
        }
    }

    @Test
    fun longestRememberedPauseWinsWhetherItComesFirstOrLast() {
        val scenarios = listOf(
            listOf(6_000 to true, 600 to false, 500 to true, 300 to false, 600 to true) to 6_300,
            listOf(6_000 to true, 300 to false, 300 to true, 600 to false, 800 to true) to 6_900
        )
        for ((frames, expectedCutMs) in scenarios) {
            val chunker = nanoChunker()
            val input = patternedPcm(bytesForMs(8_200))
            val chunks = ArrayList<ByteArray>()
            var offset = 0
            frames.forEach { (durationMs, isSpeech) ->
                val end = offset + bytesForMs(durationMs)
                val emitted = chunker.append(input.copyOfRange(offset, end), isSpeech)
                assertTrue(emitted.isEmpty())
                offset = end
            }
            chunks += chunker.append(input.copyOfRange(offset, input.size), isSpeech = true)

            assertEquals(listOf(bytesForMs(expectedCutMs)), chunks.map { it.size })
            val tail = chunker.finish()!!
            assertArrayEquals(input, chunks.single() + tail)
            assertNull(chunker.finish())
        }
    }

    @Test
    fun rememberedSilenceMidpointIsAlignedToCompletePcm16Samples() {
        val chunker = nanoChunker()
        val input = patternedPcm(bytesForMs(8_200))
        val silenceStart = bytesForMs(6_000)
        val silenceEnd = silenceStart + bytesForMs(300) + 2
        assertTrue(chunker.append(input.copyOfRange(0, silenceStart), isSpeech = true).isEmpty())
        assertTrue(chunker.append(input.copyOfRange(silenceStart, silenceEnd), isSpeech = false).isEmpty())

        val chunks = chunker.append(input.copyOfRange(silenceEnd, input.size), isSpeech = true)

        assertEquals(listOf(bytesForMs(6_150)), chunks.map { it.size })
        assertEquals(0, chunks.single().size % 2)
        assertArrayEquals(input, chunks.single() + chunker.finish()!!)
    }

    @Test
    fun continuousNanoSpeechWaitsAtEightSecondsThenEmitsWhenMoreAudioArrives() {
        val chunker = nanoChunker()
        assertTrue(chunker.append(pcm(7_800), isSpeech = true).isEmpty())
        assertTrue(chunker.append(pcm(200), isSpeech = true).isEmpty())

        val chunks = chunker.append(pcm(200), isSpeech = true)

        assertEquals(listOf(bytesForMs(8_000)), chunks.map { it.size })
        assertEquals(bytesForMs(200), chunker.finish()!!.size)
        assertNull(chunker.finish())
    }

    @Test
    fun oneSampleAfterHardBoundaryIsKeptExactlyOnce() {
        val chunker = nanoChunker()
        val input = patternedPcm(bytesForMs(8_000) + 2)
        val chunks = chunker.append(input, isSpeech = true)
        val tail = chunker.finish()!!

        assertEquals(listOf(bytesForMs(8_000)), chunks.map { it.size })
        assertEquals(2, tail.size)
        assertArrayEquals(input, chunks.single() + tail)
        assertNull(chunker.finish())
    }

    @Test
    fun irregularPcmFramesRetainEverySampleForLongNanoUtterances() {
        val frameSizes = intArrayOf(2, 126, 640, 3_200, 6_400, 12_346, 48_000)
        for (durationMs in listOf(8_000, 8_001, 18_400, 18_600, 24_000, 121_337)) {
            val input = patternedPcm(bytesForMs(durationMs))
            val chunker = nanoChunker()
            val output = ByteArrayOutputStream()
            var offset = 0
            var frameIndex = 0
            while (offset < input.size) {
                val end = (offset + frameSizes[frameIndex++ % frameSizes.size]).coerceAtMost(input.size)
                for (chunk in chunker.append(input.copyOfRange(offset, end), isSpeech = true)) {
                    assertEquals(bytesForMs(8_000), chunk.size)
                    output.write(chunk)
                }
                offset = end
            }
            chunker.finish()?.let { tail ->
                assertTrue(tail.isNotEmpty() && tail.size <= bytesForMs(8_000))
                assertEquals(0, tail.size % 2)
                output.write(tail)
            }

            assertArrayEquals("PCM must be lossless at ${durationMs}ms", input, output.toByteArray())
            assertNull(chunker.finish())
        }
    }

    @Test
    fun otherVendorsKeepTheirExistingFifteenToTwentyFourSecondPolicy() {
        val vendors: List<AsrVendor?> = AsrVendor.entries.filter { it != AsrVendor.FunAsrNano } + listOf(null)
        for (vendor in vendors) {
            val continuous = createNonStreamingPcmChunker(SAMPLE_RATE, vendor)
            assertTrue(continuous.append(pcm(8_000), isSpeech = true).isEmpty())
            assertTrue(continuous.append(pcm(7_000), isSpeech = true).isEmpty())
            assertEquals(
                "Hard boundary changed for $vendor",
                listOf(bytesForMs(24_000)),
                continuous.append(pcm(9_000), isSpeech = true).map { it.size }
            )
            assertNull(continuous.finish())

            val paused = createNonStreamingPcmChunker(SAMPLE_RATE, vendor)
            assertTrue(paused.append(pcm(14_800), isSpeech = true).isEmpty())
            assertEquals(
                "Minimum boundary changed for $vendor",
                listOf(bytesForMs(15_000)),
                paused.append(pcm(400), isSpeech = false).map { it.size }
            )
        }
    }

    private fun nanoChunker() = createNonStreamingPcmChunker(SAMPLE_RATE, AsrVendor.FunAsrNano)

    private fun pcm(durationMs: Int) = ByteArray(bytesForMs(durationMs))

    private fun patternedPcm(size: Int) = ByteArray(size) { ((it * 31 + 7) % 251).toByte() }

    private fun bytesForMs(durationMs: Int) = (SAMPLE_RATE.toLong() * 2 * durationMs / 1_000).toInt()

    private companion object {
        const val SAMPLE_RATE = 16_000
    }
}
