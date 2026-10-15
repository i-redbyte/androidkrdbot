package su.redbyte.androidkrdbot.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookReplyRepositoryTest {
    private val first = MarkovRepository.fromSentences(
        listOf("Государство отражает борьбу между общественными классами в обществе.")
    )
    private val second = MarkovRepository.fromSentences(
        listOf("Чистый код помогает программистам уверенно менять большие системы.")
    )
    private val replies = BookReplyRepository(
        listOf(BookCorpus("Государство и революция", first), BookCorpus("Чистый код", second))
    )
    private val lenin = "Государство отражает борьбу между общественными классами в обществе."
    private val cleanCode = "Чистый код помогает программистам уверенно менять большие системы."

    @Test
    fun contextTakesPriorityOverAlternationInBothModes() {
        assertEquals(lenin, replies.quote("Привет")) // no match: first fallback book
        assertEquals(lenin, replies.quote("Что такое государство?"))
        assertEquals(cleanCode, replies.quote("Что такое чистый код?"))
        assertEquals(cleanCode, replies.quote("Привет")) // contextual replies did not advance fallback

        assertEquals(cleanCode, replies.markov("Что такое чистый код?"))
        assertEquals(lenin, replies.markov("Что такое государство?"))
        assertEquals(lenin, replies.markov("Привет"))
        assertEquals(cleanCode, replies.markov("Привет"))
    }

    @Test
    fun realCorporaPreferTheRelevantBook() = runBlocking {
        assumeTrue(javaClass.classLoader.getResource("markov/clean_code_sentences.txt") != null)
        val leninModel = MarkovRepository.load("lenin_sentences.txt")
        val cleanCodeModel = MarkovRepository.load("clean_code_sentences.txt")
        assertTrue(
            leninModel.relevance("Что такое государство?") > cleanCodeModel.relevance("Что такое государство?")
        )
        assertTrue(
            cleanCodeModel.relevance("Как писать чистый код?") > leninModel.relevance("Как писать чистый код?")
        )
    }
}
