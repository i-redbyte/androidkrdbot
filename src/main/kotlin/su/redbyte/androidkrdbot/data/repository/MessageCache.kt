package su.redbyte.androidkrdbot.data.repository

import java.util.LinkedHashMap

object MessageCache {
    private val cache = object : LinkedHashMap<Long, MutableList<MessageEntry>>(
        256,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Long, MutableList<MessageEntry>>
        ): Boolean {
            return size > 100
        }
    }

    data class MessageEntry(val messageId: Long, val userId: Long)

    @Synchronized
    fun add(chatId: Long, userId: Long, messageId: Long) {
        //todo: for logs
        //println("[MessageCache] Add new message: <userId = $userId | messageId = $messageId>")
        val messages = cache.getOrPut(chatId) { mutableListOf() }
        messages.add(MessageEntry(messageId, userId))
        if (messages.size > 100) messages.removeFirst()
    }

    @Synchronized
    fun getMessagesFromUser(chatId: Long, userId: Long): List<Long> {
        return cache[chatId]?.filter { it.userId == userId }?.map { it.messageId } ?: emptyList()
    }

    @Synchronized
    fun removeMessagesFromUser(chatId: Long, userId: Long) {
        cache[chatId]?.removeIf { it.userId == userId }
    }

    @Synchronized
    fun removeMessage(chatId: Long, messageId: Long) {
        cache[chatId]?.removeIf { it.messageId == messageId }
    }

    @Synchronized
    fun clearChat(chatId: Long) {
        cache.remove(chatId)
    }

    @Synchronized
    fun clearAll() {
        cache.clear()
    }
}