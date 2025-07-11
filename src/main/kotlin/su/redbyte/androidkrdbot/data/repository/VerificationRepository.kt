package su.redbyte.androidkrdbot.data.repository

import su.redbyte.androidkrdbot.domain.model.VerificationRecord
import java.util.concurrent.ConcurrentHashMap

class VerificationRepository {
    private val map = ConcurrentHashMap<Long, VerificationRecord>()

    fun add(userId: Long, record: VerificationRecord) = map.put(userId, record)

    fun get(userId: Long): VerificationRecord? = map[userId]

    fun remove(userId: Long) = map.remove(userId)

    fun contains(userId: Long): Boolean = map.containsKey(userId)

    fun cancelTimer(userId: Long) = map[userId]?.job?.cancel()
}