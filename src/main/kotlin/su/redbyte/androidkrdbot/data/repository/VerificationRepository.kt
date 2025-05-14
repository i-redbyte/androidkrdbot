package su.redbyte.androidkrdbot.data.repository

import su.redbyte.androidkrdbot.domain.model.Verification
import java.util.concurrent.ConcurrentHashMap

class VerificationRepository {
    private val pending = ConcurrentHashMap<Long, Verification>()

    fun add(verification: Verification) {
        pending[verification.user.id] = verification
    }

    fun remove(userId: Long) {
        pending.remove(userId)
    }

    fun get(userId: Long): Verification? = pending[userId]

    fun contains(userId: Long): Boolean = pending.containsKey(userId)
}