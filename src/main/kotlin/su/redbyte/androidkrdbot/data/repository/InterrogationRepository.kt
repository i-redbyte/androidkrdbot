package su.redbyte.androidkrdbot.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TEST_SPAM_USER = 7963413498L

class InterrogationRepository(
    private val ttlMillis: Long = TimeUnit.MINUTES.toMillis(30),
    private val maxSize: Int = 10_000
) {
    private data class CacheEntry(val banned: Boolean, val expiresAt: Long)

    private val cache = ConcurrentHashMap<Long, CacheEntry>()
    private val lockByUser = ConcurrentHashMap<Long, Mutex>()

//    init {
//        //todo:for test ban user
//        cache[TEST_SPAM_USER] = CacheEntry(true, System.currentTimeMillis() + ttlMillis)
//    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
        }
    }

    suspend fun checkInLolsBot(userId: Long): Boolean {
        val now = System.currentTimeMillis()
        val cached = cache[userId]
        if (cached != null && cached.expiresAt > now) return cached.banned

        val m = lockByUser.computeIfAbsent(userId) { Mutex() }
        return m.withLock {
            val fresh = cache[userId]
            if (fresh != null && fresh.expiresAt > System.currentTimeMillis()) return@withLock fresh.banned
            val banned = fetchFromApi(userId)
            val expiresAt = System.currentTimeMillis() + ttlMillis
            if (maxSize > 0 && cache.size >= maxSize) trim()
            cache[userId] = CacheEntry(banned, expiresAt)
            banned
        }
    }

    fun invalidate(userId: Long) {
        cache.remove(userId)
    }

    fun clear() {
        cache.clear()
    }

    private fun trim() {
        val toRemove = cache.entries
            .sortedBy { it.value.expiresAt }
            .take((cache.size - maxSize + 1).coerceAtLeast(1))
        toRemove.forEach { cache.remove(it.key) }
    }

    private suspend fun fetchFromApi(userId: Long): Boolean {
        return try {
            val response: HttpResponse = client.get("https://api.lols.bot/account") {
                parameter("id", userId)
            }
            if (!response.status.isSuccess()) {
                false
            } else {
                val data: LolsBotResponse = response.body()
                data.banned
            }
        } catch (e: Exception) {
            false
        }
    }
}

@Serializable
data class LolsBotResponse(
    val banned: Boolean = false
)
