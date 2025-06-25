package su.redbyte.androidkrdbot.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import su.redbyte.androidkrdbot.data.repository.LibraryRepository
import su.redbyte.androidkrdbot.domain.model.LibraryUpdate

class FetchLibraryUpdatesUseCase(
    private val repo: LibraryRepository
) {
    suspend operator fun invoke(names: List<String>): List<LibraryUpdate> =
        coroutineScope {
            names.map { n -> async { repo.latestFor(n) } }
                .awaitAll()
                .filterNotNull()
                .sortedByDescending { it.publishedAt }
        }
}
