package su.redbyte.androidkrdbot.data.repository

import java.util.concurrent.atomic.AtomicInteger

data class BookCorpus(val title: String, val model: MarkovRepository)

class BookReplyRepository(private val books: List<BookCorpus>) {
    init {
        require(books.size >= 2) { "At least two books are required" }
    }

    private val nextQuote = AtomicInteger()
    private val nextMarkov = AtomicInteger()

    fun quote(prompt: String): String = choose(nextQuote).model.generate(prompt)

    fun markov(prompt: String): String = choose(nextMarkov).model.generateMarkov(prompt)

    private fun choose(counter: AtomicInteger): BookCorpus =
        books[Math.floorMod(counter.getAndIncrement(), books.size)]
}
