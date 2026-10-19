// Modified for Sky Wuji Edition on 2026-10-20; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.asr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.brycewg.asrkb.store.Prefs
import java.io.ByteArrayOutputStream
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenericPushFileAsrAdapterTest {
    @Before
    fun setUp() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("asr_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun realNanoEngineProvidesTheEarlyChunkingPolicyWithoutStartingMicrophone() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = SupervisorJob()
        try {
            val engine = FunAsrNanoFileAsrEngine(
                context, CoroutineScope(parent), Prefs(context), RecordingListener()
            )
            val chunker = engine.createProgressiveChunker()

            assertTrue(chunker.append(pcm(7_800), isSpeech = true).isEmpty())
            assertTrue(chunker.append(pcm(200), isSpeech = true).isEmpty())
            assertEquals(
                listOf(bytesForMs(8_000)),
                chunker.append(pcm(200), isSpeech = true).map { it.size }
            )
            assertEquals(bytesForMs(200), chunker.finish()!!.size)
            assertFalse(engine.isRunning)
        } finally {
            parent.cancel()
        }
    }

    @Test
    fun realNanoEngineKeepsShortUtteranceWholeDespitePauseAfterSixSeconds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = SupervisorJob()
        try {
            val engine = FunAsrNanoFileAsrEngine(
                context, CoroutineScope(parent), Prefs(context), RecordingListener()
            )
            val chunker = engine.createProgressiveChunker()
            val input = pcm(7_800)

            assertTrue(chunker.append(input.copyOfRange(0, bytesForMs(6_000)), isSpeech = true).isEmpty())
            assertTrue(
                chunker.append(input.copyOfRange(bytesForMs(6_000), bytesForMs(6_400)), isSpeech = false).isEmpty()
            )
            assertTrue(
                chunker.append(input.copyOfRange(bytesForMs(6_400), input.size), isSpeech = true).isEmpty()
            )
            assertArrayEquals(input, chunker.finish())
            assertFalse(engine.isRunning)
        } finally {
            parent.cancel()
        }
    }

    @Test
    fun decodesBeforeStopWithoutBlockingCaptureThenDrainsEverySampleOnce() = runBlocking {
        withFixture {
            val input = pcm(17_000)
            val worker = start()
            adapter.appendPcm(input.copyOfRange(0, bytesForMs(8_000)), SAMPLE_RATE, 1)
            val first = recognizer.nextRequest()

            assertTrue("The first decode must start while recording is active", adapter.isRunning)
            assertEquals(0, listener.stopped.get())
            assertTrue(listener.finals.isEmpty())

            // The first decode is suspended at a gate; capture must still accept the rest.
            adapter.appendPcm(input.copyOfRange(bytesForMs(8_000), input.size), SAMPLE_RATE, 1)
            assertFalse(recognizer.hasQueuedRequest())
            adapter.stop()
            adapter.stop()
            assertEquals(1, listener.stopped.get())
            assertFalse(adapter.isRunning)

            first.reply.complete(Reply.Final("第一段"))
            val second = recognizer.nextRequest()
            second.reply.complete(Reply.Final("第二段"))
            val tail = recognizer.nextRequest()
            assertTrue("No final may precede the tail decode", listener.finals.isEmpty())
            tail.reply.complete(Reply.Final("末尾"))
            worker.join()

            assertEquals(listOf(bytesForMs(8_000), bytesForMs(8_000), bytesForMs(1_000)), recognizer.pcmSizes())
            assertArrayEquals(input, recognizer.allPcm())
            assertEquals(1, recognizer.maxConcurrent.get())
            assertEquals(listOf("第一段第二段末尾"), listener.finals.toList())
            assertTrue(listener.errors.isEmpty())
            assertEquals(1, listener.stopped.get())
        }
    }

    @Test
    fun shortRecordingIsFlushedOnStopAndRepeatedStopCannotDuplicateIt() = runBlocking {
        withFixture {
            val input = pcm(1_337)
            val worker = start()
            adapter.appendPcm(input, SAMPLE_RATE, 1)
            assertFalse(recognizer.hasQueuedRequest())

            adapter.stop()
            adapter.stop()
            val tail = recognizer.nextRequest()
            tail.reply.complete(Reply.Final("短尾句"))
            worker.join()

            assertEquals(listOf(input.size), recognizer.pcmSizes())
            assertArrayEquals(input, recognizer.allPcm())
            assertEquals(listOf("短尾句"), listener.finals.toList())
            assertEquals(1, listener.stopped.get())
            assertTrue(listener.errors.isEmpty())
        }
    }

    @Test
    fun cancellationDiscardsQueuedAudioAndSuppressesUninterruptibleLateDecode() = runBlocking {
        withFixture(ignoreDecodeCancellation = true) {
            val worker = start()
            adapter.appendPcm(pcm(8_000), SAMPLE_RATE, 1)
            val first = recognizer.nextRequest()
            adapter.appendPcm(pcm(9_000), SAMPLE_RATE, 1)

            adapter.cancel()
            assertFalse(adapter.isRunning)
            // Model/native calls can finish after cancellation; their result must stay discarded.
            first.reply.complete(Reply.Final("迟到的旧结果"))
            worker.join()

            assertEquals(listOf(bytesForMs(8_000)), recognizer.pcmSizes())
            assertTrue(listener.finals.isEmpty())
            assertTrue(listener.errors.isEmpty())
            assertEquals(0, listener.stopped.get())
        }
    }

    @Test
    fun emptyMiddleChunkDoesNotReplaceEarlierTextOrLoseTail() = runBlocking {
        withFixture {
            val input = pcm(17_000)
            val worker = start()
            adapter.appendPcm(input, SAMPLE_RATE, 1)
            adapter.stop()

            recognizer.nextRequest().reply.complete(Reply.Final("第一段"))
            recognizer.nextRequest().reply.complete(Reply.Error("empty chunk"))
            recognizer.nextRequest().reply.complete(Reply.Final("最后一段"))
            worker.join()

            assertArrayEquals(input, recognizer.allPcm())
            assertEquals(listOf("第一段最后一段"), listener.finals.toList())
            assertTrue(listener.errors.isEmpty())
            assertEquals(1, listener.stopped.get())
        }
    }

    @Test
    fun fatalChunkErrorStopsRecordingAndEmitsOneErrorWithoutPartialSuccess() = runBlocking {
        withFixture {
            val worker = start()
            adapter.appendPcm(pcm(17_000), SAMPLE_RATE, 1)
            recognizer.nextRequest().reply.complete(Reply.Final("已经识别的前段"))
            recognizer.nextRequest().reply.complete(Reply.Error("decode failed"))
            worker.join()
            adapter.stop()

            assertFalse(adapter.isRunning)
            assertEquals(2, recognizer.pcmSizes().size)
            assertTrue(listener.finals.isEmpty())
            assertEquals(listOf("decode failed"), listener.errors.toList())
            assertEquals(1, listener.stopped.get())
        }
    }

    private suspend fun withFixture(
        ignoreDecodeCancellation: Boolean = false,
        block: suspend Fixture.() -> Unit
    ) = withTimeout(10_000L) {
        val fixture = Fixture(ignoreDecodeCancellation)
        try {
            fixture.block()
        } finally {
            withContext(NonCancellable) { fixture.close() }
        }
    }

    private class Fixture(ignoreDecodeCancellation: Boolean) {
        private val parent = SupervisorJob()
        private val scope = CoroutineScope(parent + Dispatchers.Default)
        val listener = RecordingListener()
        private val collector = NonStreamingChunkResultCollector(
            delegate = listener,
            emptyResultMessage = "empty result",
            ignorableEmptyErrors = setOf("empty chunk")
        )
        val recognizer = ControlledRecognizer(collector, ignoreDecodeCancellation)
        private val context = ApplicationProvider.getApplicationContext<Context>()
        val adapter = GenericPushFileAsrAdapter(
            context = context,
            scope = scope,
            prefs = Prefs(context),
            listener = listener,
            recognizer = recognizer,
            applyAudioPreprocess = false,
            progressiveResults = collector,
            // Queue tests fix the cut at 8s so native VAD availability cannot alter boundaries.
            // NanoProgressiveChunkingTest separately verifies the production 6s/8s VAD policy.
            progressiveChunkerFactory = {
                NonStreamingPcmChunker(SAMPLE_RATE, minChunkMs = 8_000, maxChunkMs = 8_000)
            }
        )

        fun start(): Job {
            adapter.start()
            return parent.children.single()
        }

        suspend fun close() {
            adapter.cancel()
            recognizer.releasePending()
            parent.cancelAndJoin()
        }
    }

    private class ControlledRecognizer(
        private val results: StreamingAsrEngine.Listener,
        private val ignoreCancellation: Boolean
    ) : PcmBatchRecognizer {
        private val requests = Channel<DecodeRequest>(Channel.UNLIMITED)
        private val seen = Collections.synchronizedList(mutableListOf<DecodeRequest>())
        private val concurrent = AtomicInteger()
        val maxConcurrent = AtomicInteger()

        override suspend fun recognizeFromPcm(pcm: ByteArray) {
            val active = concurrent.incrementAndGet()
            maxConcurrent.updateAndGet { maxOf(it, active) }
            val request = DecodeRequest(pcm.copyOf())
            seen += request
            requests.send(request)
            try {
                if (ignoreCancellation) {
                    withContext(NonCancellable) { deliver(request.reply.await()) }
                } else {
                    deliver(request.reply.await())
                }
            } finally {
                concurrent.decrementAndGet()
            }
        }

        private fun deliver(reply: Reply) {
            when (reply) {
                is Reply.Final -> results.onFinal(reply.text)
                is Reply.Error -> results.onError(reply.message)
            }
        }

        suspend fun nextRequest(): DecodeRequest = requests.receive()

        fun hasQueuedRequest(): Boolean = requests.tryReceive().isSuccess

        fun pcmSizes(): List<Int> = synchronized(seen) { seen.map { it.pcm.size } }

        fun allPcm(): ByteArray = synchronized(seen) {
            ByteArrayOutputStream().apply { seen.forEach { write(it.pcm) } }.toByteArray()
        }

        fun releasePending() = synchronized(seen) {
            seen.forEach { it.reply.complete(Reply.Final("cleanup")) }
        }
    }

    private data class DecodeRequest(
        val pcm: ByteArray,
        val reply: CompletableDeferred<Reply> = CompletableDeferred()
    )

    private sealed interface Reply {
        data class Final(val text: String) : Reply
        data class Error(val message: String) : Reply
    }

    private class RecordingListener : StreamingAsrEngine.Listener {
        val finals = Collections.synchronizedList(mutableListOf<String>())
        val errors = Collections.synchronizedList(mutableListOf<String>())
        val stopped = AtomicInteger()
        override fun onFinal(text: String) {
            finals += text
        }

        override fun onError(message: String) {
            errors += message
        }

        override fun onStopped() {
            stopped.incrementAndGet()
        }
    }

    private fun pcm(durationMs: Int) = ByteArray(bytesForMs(durationMs)) { ((it * 31 + 7) % 251).toByte() }

    private fun bytesForMs(durationMs: Int) = (SAMPLE_RATE.toLong() * 2 * durationMs / 1_000).toInt()

    private companion object {
        const val SAMPLE_RATE = 16_000
    }
}
