package su.redbyte.androidkrdbot.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.source.*

object SearchEngine {
    private val sources: MutableList<Source> = mutableListOf(
        HabrSource(),
        AppTractorSource()
    )

    fun register(source: Source) {
        sources += source
    }

    suspend fun search(query: String, n: Int): List<ExpropriationResult> = coroutineScope {
        println("🔍  Start search: $query in ${sources.size} sources")
        val start = System.currentTimeMillis()

        val jobs = sources.map { src ->
            async {
                runCatching { src.search(query, n) }
                    .onSuccess { println("✓  ${it.size} results") }
                    .onFailure {
                        println("⨯ [${src.source.sourceName}] search failed: ${it.message}")
                        it.printStackTrace()
                    }.getOrElse { emptyList() }
            }
        }

        val results = jobs.awaitAll().flatten()
        println("✅  Done: ${results.size} results in ${System.currentTimeMillis() - start} ms")
        results
    }
}