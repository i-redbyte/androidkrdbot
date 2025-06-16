package su.redbyte.androidkrdbot.data.repository

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.source.ApptractorSource
import su.redbyte.androidkrdbot.data.source.HabrSource
import su.redbyte.androidkrdbot.data.source.KotlinLangSource
import su.redbyte.androidkrdbot.data.source.Source

object SearchEngine {
    private val client = HttpClient(CIO) {
        expectSuccess = true
        engine {
            requestTimeout = 15_000
        }
    }

    private val sources: MutableList<Source> = mutableListOf(
        HabrSource(client),
        ApptractorSource(client),
        KotlinLangSource(client)
    )

    fun register(source: Source) {
        sources += source
    }

    suspend fun search(query: String): List<ExpropriationResult> = coroutineScope {
        val jobs = sources.map { src ->
            async {
                runCatching { src.search(query) }
                    .onFailure { println("[WARN] ${src.baseUrl}: ${it.message}") }
                    .getOrElse { emptyList() }
            }
        }
        jobs.awaitAll().flatten()
    }
}