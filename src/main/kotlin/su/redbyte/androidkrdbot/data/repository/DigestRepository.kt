package su.redbyte.androidkrdbot.data.repository

import kotlinx.coroutines.withTimeout
import su.redbyte.androidkrdbot.infra.utils.fetchDigest

class DigestRepository(
    private val apiId: String,
    private val apiHash: String
) {
    suspend fun getDigest(): Result<String> {
        return try {
            Result.success(
                withTimeout(DIGEST_TIMEOUT_MS) {
                    fetchDigest(apiId, apiHash)
                }
            )
        } catch (e: Exception) {
            println("[DigestRepository]: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val DIGEST_TIMEOUT_MS = 16 * 60 * 1000L
    }
}