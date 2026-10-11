// Modified for Sky Wuji Edition on 2026-10-11; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.asr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FunAsrNanoAudioTest {
    @Test
    fun exactDigitalSilenceHasNoSignal() {
        assertFalse(hasFunAsrNanoAudioSignal(ByteArray(16000 * 2 * 8)))
        assertFalse(hasFunAsrNanoAudioSignal(ByteArray(0)))
    }

    @Test
    fun evenOneLowestAmplitudeSampleIsKept() {
        // No energy threshold: even one +/-1 PCM16 sample must survive.
        for (sample in listOf(byteArrayOf(1, 0), byteArrayOf(-1, -1), byteArrayOf(0, 1))) {
            for (offset in listOf(0, 8000, 15998)) {
                val quietAudio = ByteArray(16000)
                sample.copyInto(quietAudio, offset)
                assertTrue(hasFunAsrNanoAudioSignal(quietAudio))
            }
        }
    }
}
