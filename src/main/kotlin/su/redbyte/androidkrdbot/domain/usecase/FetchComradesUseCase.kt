package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.data.repository.ComradesRepository
import su.redbyte.androidkrdbot.domain.model.Comrade

class FetchComradesUseCase (
    private val repository: ComradesRepository
){
    suspend operator fun invoke(): List<Comrade> {
        return repository.getAll().getOrThrow()
    }

    suspend fun findById(id: Long): Comrade? = repository.findById(id)

    suspend fun findByUsername(username: String): Comrade? = repository.findByUsername(username)

    suspend fun ensureCached(id: Long): Comrade? = repository.ensureCached(id)

    suspend fun put(comrade: Comrade) = repository.put(comrade)
}