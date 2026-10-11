// Modified for Sky Wuji Edition on 2026-10-11; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.asr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FunAsrNanoTextTest {
    @Test
    fun silenceOnlyAndRepeatedMarkersProduceNoText() {
        for (text in listOf("/sil", " /sil ", "/sil/sil", "/sil /sil")) {
            assertEquals("", cleanFunAsrNanoText(text))
        }
    }

    @Test
    fun markersInsideChineseAndAtTheEndKeepAllSpokenText() {
        assertEquals("前半句，后半句。", cleanFunAsrNanoText("前半句，/sil后半句。/sil"))
        assertEquals("完整句子", cleanFunAsrNanoText("/sil完整/sil句子/sil"))
    }

    @Test
    fun englishWordSpacingAndPunctuationArePreserved() {
        assertEquals("hello world.", cleanFunAsrNanoText("hello /sil world. /sil"))
        assertEquals("Hello, world!", cleanFunAsrNanoText("Hello, /sil world!"))
        assertEquals("first  second third", cleanFunAsrNanoText("first  second /sil third"))
    }

    @Test
    fun ordinaryContentAndSimilarPathsStayIntact() {
        for (text in listOf("中文，English 123。", "第一行\n第二行", "/silver", "/silicon", "/sil/model", "/sil.txt", "/sil-data", "https://example.com/sil", "sil", "SIL")) {
            assertEquals(text, cleanFunAsrNanoText(text))
        }
    }

    @Test
    fun silentChunksDoNotReplaceOrInterruptLongDictation() = runTest {
        val events = mutableListOf<String>()
        val collector = NonStreamingChunkResultCollector(
            delegate = object : StreamingAsrEngine.Listener {
                override fun onFinal(text: String) { events += "final:$text" }
                override fun onError(message: String) { events += "error:$message" }
            },
            emptyResultMessage = "empty",
            ignorableEmptyErrors = setOf("empty")
        )
        collector.start()
        for (raw in listOf("第一段。", "/sil", "第二段。/sil", "/sil第三段。", "/sil")) {
            val text = cleanFunAsrNanoText(raw)
            if (text.isBlank()) collector.onError("empty") else collector.onFinal(text)
        }
        collector.finish()
        assertEquals(listOf("final:第一段。第二段。第三段。"), events)
    }

    @Test
    fun allSilentChunksFinishWithoutACommittedTranscript() = runTest {
        val events = mutableListOf<String>()
        val collector = NonStreamingChunkResultCollector(
            delegate = object : StreamingAsrEngine.Listener {
                override fun onFinal(text: String) { events += "final:$text" }
                override fun onError(message: String) { events += "error:$message" }
            },
            emptyResultMessage = "empty",
            ignorableEmptyErrors = setOf("empty")
        )
        collector.start()
        repeat(3) {
            val text = cleanFunAsrNanoText("/sil")
            if (text.isBlank()) collector.onError("empty") else collector.onFinal(text)
        }
        collector.finish()
        assertEquals(listOf("error:empty"), events)
    }
}
