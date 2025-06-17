package su.redbyte.androidkrdbot.data.source

import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.model.LootSource

interface Source {
    val source: LootSource
    suspend fun search(query: String, n: Int): List<ExpropriationResult>
}