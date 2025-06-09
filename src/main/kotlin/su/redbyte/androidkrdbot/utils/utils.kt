package su.redbyte.androidkrdbot.utils

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import su.redbyte.androidkrdbot.data.repository.MessageCache

fun User.candidateName(): String = username?.let { "@$it" } ?: firstName

fun deleteMessagesFromUser(bot: Bot, chatId: ChatId, userId: Long) {
    val rawChatId = when (chatId) {
        is ChatId.Id -> chatId.id
        else -> error("❌ ChatId не содержит числового ID")
    }
    val messageIds = MessageCache.getMessagesFromUser(rawChatId, userId)
    println("messageIds =${messageIds.size}")
    messageIds.forEach {
        runCatching {
            bot.deleteMessage(chatId, it)
            MessageCache.removeMessage(rawChatId, it)
        }.onFailure {
            println("❌ Не удалось удалить сообщение $it: ${it.message}")
        }
    }
}