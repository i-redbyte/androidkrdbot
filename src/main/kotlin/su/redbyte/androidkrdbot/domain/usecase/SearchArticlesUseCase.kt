package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.repository.SearchEngine

class SearchArticlesUseCase(
    private val searchEngine: SearchEngine = SearchEngine
) {
    suspend operator fun invoke(query: String): List<ExpropriationResult> =
        searchEngine.search(query)
}