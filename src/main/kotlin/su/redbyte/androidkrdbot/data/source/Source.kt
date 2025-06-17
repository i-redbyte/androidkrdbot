package su.redbyte.androidkrdbot.data.source

import su.redbyte.androidkrdbot.data.model.ExpropriationResult

interface Source {
    val sourceName: String
    suspend fun search(query: String, n: Int = 10): List<ExpropriationResult>
}