package su.redbyte.androidkrdbot.data.repository

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ChatMember
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ChatAdminRepository {
    private data class CachedAdmins(
        val admins: List<ChatMember>,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<Long, CachedAdmins>()
    private val cacheTtlSec = 3600L // 1 час

    fun getAdmins(bot: Bot, chatId: Long): List<ChatMember> {
        val now = Instant.now().epochSecond

        val cached = cache[chatId]
        if (cached != null && now - cached.timestamp < cacheTtlSec) {
            return cached.admins
        }

        return updateAdmins(bot, chatId)
    }

    fun updateAdmins(bot: Bot, rawChatId: Long): List<ChatMember> {
        val chatId = ChatId.fromId(rawChatId)
        val admins = bot.getChatAdministrators(chatId).getOrNull().orEmpty()
        cache[rawChatId] = CachedAdmins(admins, Instant.now().epochSecond)
        return admins
    }
}
