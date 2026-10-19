// Modified for Sky Wuji Edition on 2026-10-19; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.ime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.brycewg.asrkb.asr.AsrParallelEngineDecision
import com.brycewg.asrkb.asr.AsrVendor
import com.brycewg.asrkb.asr.BackupAwareAsrEngine
import com.brycewg.asrkb.store.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProcessingTimeoutControllerTest {
    private lateinit var prefs: Prefs
    private var timeoutCalls = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("asr_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = Prefs(context)
    }

    @Test
    fun nanoWaitsWithoutCreatingTimerEvenAfterOneHour() = runTest {
        prefs.asrVendor = AsrVendor.FunAsrNano
        val controller = controller(this)

        controller.schedule()

        assertNull(timeoutJob(controller))
        advanceTimeBy(3_600_000L)
        runCurrent()
        assertEquals(0, timeoutCalls)
    }

    @Test
    fun cancelUnboundedWaitIsSafeAndDoesNotFireTimeout() = runTest {
        prefs.asrVendor = AsrVendor.FunAsrNano
        val controller = controller(this)
        controller.schedule()

        controller.cancel()
        controller.cancel()
        advanceTimeBy(3_600_000L)
        runCurrent()

        assertNull(timeoutJob(controller))
        assertEquals(0, timeoutCalls)
    }

    @Test
    fun switchingToNanoCancelsPreviousBoundedTimer() = runTest {
        prefs.asrVendor = AsrVendor.OpenAI
        val controller = controller(this)
        controller.schedule()
        val previous = timeoutJob(controller)
        assertNotNull(previous)
        runCurrent()

        prefs.asrVendor = AsrVendor.FunAsrNano
        controller.schedule()

        assertTrue(previous!!.isCancelled)
        assertNull(timeoutJob(controller))
        advanceTimeBy(3_600_000L)
        runCurrent()
        assertEquals(0, timeoutCalls)
    }

    @Test
    fun switchingAwayFromNanoStillSchedulesOtherVendorsTimeout() = runTest {
        prefs.asrVendor = AsrVendor.FunAsrNano
        val controller = controller(this)
        controller.schedule()
        assertNull(timeoutJob(controller))

        prefs.asrVendor = AsrVendor.OpenAI
        controller.schedule()
        assertNotNull(timeoutJob(controller))
        runCurrent()
        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(1, timeoutCalls)
    }

    @Test
    fun activeNanoBackupAlsoWaitsWithoutTimer() = runTest {
        prefs.asrVendor = AsrVendor.OpenAI
        val controller = controller(this, NanoBackupEngine())

        controller.schedule()

        assertNull(timeoutJob(controller))
        advanceTimeBy(3_600_000L)
        runCurrent()
        assertEquals(0, timeoutCalls)
    }

    private fun controller(
        scope: CoroutineScope,
        backupEngine: BackupAwareAsrEngine? = null
    ) = ProcessingTimeoutController(
        scope = scope,
        prefs = prefs,
        logTag = "ProcessingTimeoutControllerTest",
        currentStateProvider = { KeyboardState.Processing },
        opSeqProvider = { 1L },
        audioMsProvider = { 1_000L },
        backupEngineProvider = { backupEngine },
        onTimeout = { timeoutCalls++ }
    )

    private fun timeoutJob(controller: ProcessingTimeoutController): Job? =
        ProcessingTimeoutController::class.java.getDeclaredField("job").apply {
            isAccessible = true
        }.get(controller) as Job?

    private class NanoBackupEngine : BackupAwareAsrEngine {
        override val primaryVendor = AsrVendor.OpenAI
        override val backupVendor = AsrVendor.FunAsrNano
        override val backupStrategy = AsrParallelEngineDecision.UseLazyLocalBackup
        override val isRunning = false
        override fun wasLastResultFromBackup() = false
        override fun start() = Unit
        override fun stop() = Unit
    }
}
