package su.redbyte.androidkrdbot.data.source

import su.redbyte.androidkrdbot.data.model.ExpropriationResult

interface Source {
    val baseUrl: String

    suspend fun search(query: String): List<ExpropriationResult>
}