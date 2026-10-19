// Modified for Sky Wuji Edition on 2026-10-19; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.ime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.brycewg.asrkb.asr.AsrFailReasonCodes
import com.brycewg.asrkb.store.AsrHistoryStore
import com.brycewg.asrkb.store.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AsrSessionManagerSessionIsolationTest {
    private val scope = CoroutineScope(SupervisorJob())
    private lateinit var manager: AsrSessionManager
    private val listener = RecordingListener()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("asr_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        manager = AsrSessionManager(context, scope, Prefs(context))
        manager.setListener(listener)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun timeoutThenNewSessionIgnoresOldFinal() {
        assertOldCallbackIgnored(timedOut = true, callback = "onFinal")
    }

    @Test
    fun timeoutThenNewSessionIgnoresOldError() {
        assertOldCallbackIgnored(timedOut = true, callback = "onError")
    }

    @Test
    fun cancelThenNewSessionIgnoresOldFinal() {
        assertOldCallbackIgnored(timedOut = false, callback = "onFinal")
    }

    @Test
    fun cancelThenNewSessionIgnoresOldError() {
        assertOldCallbackIgnored(timedOut = false, callback = "onError")
    }

    @Test
    fun clearingCompletedSessionDoesNotReuseItsSequence() {
        val first = nextSession()
        clearSession(first)
        assertFalse(isActive(first))

        val second = nextSession()

        assertNotEquals(first, second)
        assertTrue(isActive(second))
        assertFalse(isActive(first))
        assertFalse(isActive(0L))
    }

    @Test
    fun lateCleanupCannotClearNewSession() {
        val first = nextSession()
        clearSession(first)
        val second = nextSession()

        clearSession(first)

        assertTrue(isActive(second))
    }

    @Test
    fun oldRequestDurationCannotOverwriteNewSessionTiming() {
        val first = nextSession()
        manager.abandonPendingRecognition()
        val second = nextSession()
        val durationCallback = method(
            "onRequestDuration",
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!
        )
        durationCallback.invoke(manager, second, 1200L)

        durationCallback.invoke(manager, first, 90000L)

        assertEquals(1200L, manager.getLastRequestDuration())
        assertTrue(isActive(second))
    }

    @Test
    fun oldStoppedCannotConsumeNewSessionStoppedDelivery() {
        val first = nextSession()
        manager.abandonPendingRecognition()
        val second = nextSession()
        val stoppedCallback = method("onStopped", Long::class.javaPrimitiveType!!)

        stoppedCallback.invoke(manager, first)

        assertEquals(0, listener.stoppedCount)

        stoppedCallback.invoke(manager, second)
        stoppedCallback.invoke(manager, second)

        assertEquals(1, listener.stoppedCount)
        assertTrue(isActive(second))
    }

    private fun assertOldCallbackIgnored(timedOut: Boolean, callback: String) {
        // Enter the same lifecycle used by startRecording without opening a microphone.
        val first = nextSession()
        if (timedOut) {
            manager.abandonPendingRecognition(
                status = AsrHistoryStore.AsrHistoryStatus.FAILED,
                failStage = AsrHistoryStore.AsrHistoryFailStage.RECOGNITION,
                failReasonCode = AsrFailReasonCodes.TIMEOUT
            )
        } else {
            manager.abandonPendingRecognition()
        }
        assertFalse(isActive(first))
        val second = nextSession()

        deliver(callback, first, "old session")

        assertTrue("A stale $callback must not end the new session", isActive(second))
        assertEquals(emptyList<String>(), listener.finals)
        assertEquals(emptyList<String>(), listener.errors)

        deliver("onFinal", second, "new session")

        assertEquals(listOf("new session"), listener.finals)
        assertFalse(isActive(second))
    }

    private fun nextSession(): Long = method("nextSessionSeq").invoke(manager) as Long

    private fun clearSession(sequence: Long) {
        method("clearActiveSession", java.lang.Long::class.java).invoke(manager, sequence)
    }

    private fun isActive(sequence: Long): Boolean =
        method("isSessionActive", Long::class.javaPrimitiveType!!).invoke(manager, sequence) as Boolean

    private fun deliver(callback: String, sequence: Long, text: String) {
        method(callback, Long::class.javaPrimitiveType!!, String::class.java)
            .invoke(manager, sequence, text)
    }

    private fun method(name: String, vararg parameterTypes: Class<*>) =
        AsrSessionManager::class.java.getDeclaredMethod(name, *parameterTypes).apply {
            isAccessible = true
        }

    private class RecordingListener : AsrSessionManager.Listener {
        val finals = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var stoppedCount = 0

        override fun onAsrFinal(text: String, currentState: KeyboardState) {
            finals += text
        }

        override fun onAsrError(message: String) {
            errors += message
        }

        override fun onAsrPartial(text: String) = Unit
        override fun onAsrStopped() {
            stoppedCount++
        }
        override fun onLocalModelLoadStart() = Unit
        override fun onLocalModelLoadDone() = Unit
    }
}
