package su.redbyte.androidkrdbot.data.repository

import su.redbyte.androidkrdbot.utils.fetchDigest

class DigestRepository(
    private val apiId: String,
    private val apiHash: String
) {
    suspend fun getDigest(): Result<String> {
        return try {
            Result.success(fetchDigest(apiId, apiHash))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}