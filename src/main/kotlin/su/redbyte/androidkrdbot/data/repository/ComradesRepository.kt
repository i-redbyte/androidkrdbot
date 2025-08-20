package su.redbyte.androidkrdbot.data.repository

import su.redbyte.androidkrdbot.domain.model.Comrade
import su.redbyte.androidkrdbot.infra.utils.fetchComrades

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ComradesRepository(
    private val apiId: String,
    private val apiHash: String
) {
    private val comradesCache = mutableMapOf<Long, Comrade>()
    private val mutex = Mutex()

    suspend fun getAll(): Result<List<Comrade>> = mutex.withLock {
        try {
            if (comradesCache.isEmpty()) {
                val fetched = fetchComrades(apiId, apiHash)
                comradesCache.putAll(fetched.associateBy { it.id })
            }
            Result.success(comradesCache.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findById(id: Long): Comrade? = mutex.withLock {
        comradesCache[id]
    }

    suspend fun findByUsername(username: String): Comrade? = mutex.withLock {
        comradesCache.values.firstOrNull {
            it.userName.equals(username.trimStart('@'), ignoreCase = true)
        }
    }

    suspend fun ensureCached(id: Long): Comrade? = mutex.withLock {
        comradesCache[id] ?: run {
            val refreshed = fetchComrades(apiId, apiHash)
            comradesCache.putAll(refreshed.associateBy { it.id })
            comradesCache[id]
        }
    }

    suspend fun put(comrade: Comrade) = mutex.withLock {
        comradesCache[comrade.id] = comrade
    }

    suspend fun remove(id: Long) = mutex.withLock {
        comradesCache.remove(id)
    }

    suspend fun refreshCache(): Result<List<Comrade>> = mutex.withLock {
        try {
            comradesCache.clear()
            val fetched = fetchComrades(apiId, apiHash)
            comradesCache.putAll(fetched.associateBy { it.id })
            Result.success(comradesCache.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
