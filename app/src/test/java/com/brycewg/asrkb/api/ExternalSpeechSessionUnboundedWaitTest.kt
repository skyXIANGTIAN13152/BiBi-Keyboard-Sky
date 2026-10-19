// Modified for Sky Wuji Edition on 2026-10-19; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.brycewg.asrkb.asr.AsrVendor
import com.brycewg.asrkb.asr.StreamingAsrEngine
import com.brycewg.asrkb.store.Prefs
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalSpeechSessionUnboundedWaitTest {
    private lateinit var session: ExternalSpeechSession
    private val callbacks = RecordingCallbacks()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("asr_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val prefs = Prefs(context).apply { asrVendor = AsrVendor.FunAsrNano }
        session = ExternalSpeechSession(1, context, prefs, callbacks)
        field("vendor").set(session, AsrVendor.FunAsrNano)
    }

    @After
    fun tearDown() {
        session.cancel()
    }

    @Test
    fun unboundedWaitClearsAnyExistingTimeoutJob() {
        val previous = Job()
        field("processingTimeoutJob").set(session, previous)

        schedule()

        assertTrue(previous.isCancelled)
        assertNull(field("processingTimeoutJob").get(session))
        assertTrue((field("sessionJob").get(session) as Job).children.none())
        assertEquals(emptyList<String>(), callbacks.errors)
    }

    @Test
    fun manualCancelStillStopsEngineAndRejectsLateFinalWithoutTimer() {
        val engine = RecordingEngine()
        session.engine = engine
        schedule()
        assertNull(field("processingTimeoutJob").get(session))

        session.cancel()
        session.onFinal("late result")

        assertEquals(1, engine.stopCalls)
        assertTrue((field("sessionJob").get(session) as Job).isCancelled)
        assertEquals(listOf("canceled"), callbacks.states)
        assertEquals(emptyList<String>(), callbacks.finals)
        assertEquals(emptyList<String>(), callbacks.errors)
    }

    private fun schedule() {
        ExternalSpeechSession::class.java.getDeclaredMethod("scheduleProcessingTimeoutIfNeeded")
            .apply { isAccessible = true }
            .invoke(session)
    }

    private fun field(name: String) = ExternalSpeechSession::class.java.getDeclaredField(name).apply {
        isAccessible = true
    }

    private class RecordingEngine : StreamingAsrEngine {
        var stopCalls = 0
        override val isRunning = true
        override fun start() = Unit
        override fun stop() {
            stopCalls++
        }
    }

    private class RecordingCallbacks : ExternalSpeechCallbacks {
        val states = mutableListOf<String>()
        val finals = mutableListOf<String>()
        val errors = mutableListOf<String>()
        override fun onState(sessionId: Int, state: Int, message: String) {
            states += message
        }

        override fun onFinal(sessionId: Int, text: String) {
            finals += text
        }

        override fun onError(sessionId: Int, code: Int, message: String) {
            errors += message
        }

        override fun onPartial(sessionId: Int, text: String) = Unit
        override fun onAmplitude(sessionId: Int, amplitude: Float) = Unit
        override fun onSessionDone(sessionId: Int) = Unit
    }
}
