package su.redbyte.androidkrdbot.data.repository

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.source.*

object SearchEngine {
    private val sources: MutableList<Source> = mutableListOf(
        HabrSource(),
    )

    fun register(source: Source) {
        sources += source
    }

    suspend fun search(query: String): List<ExpropriationResult> = coroutineScope {
        println("🔍  Start search: $query in ${sources.size} sources")
        val start = System.currentTimeMillis()

        val jobs = sources.map { src ->
            async {
                runCatching { src.search(query) }
                    .onSuccess { println("✓  ${it.size} results") }
                    .onFailure {
                        println("⨯ [${src.sourceName}] search failed: ${it.message}")
                        it.printStackTrace()
                    }.getOrElse { emptyList() }
            }
        }

        val results = jobs.awaitAll().flatten()
        println("✅  Done: ${results.size} results in ${System.currentTimeMillis() - start} ms")
        results
    }
}