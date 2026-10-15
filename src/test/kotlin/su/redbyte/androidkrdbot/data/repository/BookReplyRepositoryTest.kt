package su.redbyte.androidkrdbot.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun quoteBooksAlternateIndependentlyOfMarkovBooks() {
        val lenin = "Государство отражает борьбу между общественными классами в обществе."
        val cleanCode = "Чистый код помогает программистам уверенно менять большие системы."
        assertEquals(lenin, replies.quote(""))
        assertEquals(lenin, replies.markov(""))
        assertEquals(cleanCode, replies.quote(""))
        assertEquals(cleanCode, replies.markov(""))
        assertEquals(lenin, replies.quote(""))
    }
}
