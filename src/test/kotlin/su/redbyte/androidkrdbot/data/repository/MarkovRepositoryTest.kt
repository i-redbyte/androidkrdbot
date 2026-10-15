package su.redbyte.androidkrdbot.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkovRepositoryTest {
    private val model = MarkovRepository.fromSentences(
        listOf(
            "Государство отражает борьбу между общественными классами в обществе.",
            "Революция меняет старую систему государственной власти во всей стране.",
            "Рабочие создают новые органы политической власти в городе."
        )
    )

    @Test
    fun relevantSentence() {
        val answer = model.generate("Что такое государство?", random = Random(1))
        assertEquals("Государство отражает борьбу между общественными классами в обществе.", answer)
    }

    @Test
    fun sentenceBoundariesAndLength() {
        repeat(30) { seed ->
            val answer = model.generateMarkov(maxWords = 12, random = Random(seed))
            assertTrue(answer.endsWith("."))
            assertTrue(answer.length < 900)
            assertTrue(answer.split(" ").size <= 12)
            assertTrue(!answer.contains("<END>"))
        }
    }

    @Test
    fun realCorpusProducesCompleteReplies() = runBlocking {
        val corpus = requireNotNull(
            javaClass.classLoader.getResourceAsStream("markov/lenin_sentences.txt")
        ).bufferedReader(Charsets.UTF_8).use { it.readLines() }
        assertTrue(corpus.size >= 1000)
        assertTrue("Явно нарастает международная пролетарская революция." in corpus)
        val realModel = MarkovRepository.load()
        val definition = realModel.generate("Что такое государство?", random = Random(7))
        assertTrue(definition.startsWith("Государство есть"), definition)
        for (question in listOf("Что такое государство?", "Зачем нужна революция?", "Как дела?")) {
            val answer = realModel.generate(question, random = Random(7))
            assertTrue(answer.endsWith(".") || answer.endsWith("!") || answer.endsWith("?"))
            assertTrue(answer.length <= 900)
            val playfulAnswer = realModel.generateMarkov(question, random = Random(7))
            assertTrue(playfulAnswer.endsWith(".") || playfulAnswer.endsWith("!") || playfulAnswer.endsWith("?"))
            assertTrue(playfulAnswer.length <= 900)
        }
    }

    @Test
    fun localCleanCodeCorpusProducesRelevantReply() = runBlocking {
        val resource = javaClass.classLoader.getResourceAsStream("markov/clean_code_sentences.txt")
        assumeTrue(resource != null, "The local Clean Code corpus has not been generated")
        val lines = requireNotNull(resource).bufferedReader(Charsets.UTF_8).use { it.readLines() }
        assertTrue(lines.size >= 2000)
        val model = MarkovRepository.load("clean_code_sentences.txt")
        val answer = model.generate("Что такое чистый код?", random = Random(7))
        assertTrue("код" in answer.lowercase() || "чист" in answer.lowercase(), answer)
    }
}
