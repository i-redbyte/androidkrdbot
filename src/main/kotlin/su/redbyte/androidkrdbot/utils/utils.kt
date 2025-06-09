package su.redbyte.androidkrdbot.utils

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import su.redbyte.androidkrdbot.data.repository.MessageCache

fun User.candidateName(): String = username?.let { "@$it" } ?: firstName
fun ChatId.rawChatId(): Long = when (this) {
    is ChatId.Id -> this.id
    else -> error("❌ ChatId не содержит числового ID")
}

//todo refactoring
fun deleteMessagesFromUser(bot: Bot, chatId: ChatId, userId: Long) {
    val rawChatId = chatId.rawChatId()
    val messageIds = MessageCache.getMessagesFromUser(rawChatId, userId)
    messageIds.forEach {
        runCatching {
            bot.deleteMessage(chatId, it)
            MessageCache.removeMessage(rawChatId, it)
        }.onFailure {
            println("❌ Не удалось удалить сообщение $it: ${it.message}")
        }
    }
}

//todo refactoring
fun deleteMessagesFromBot(bot: Bot, chatId: ChatId, n: Int = 2) {
    val rawChatId = chatId.rawChatId()
    val messageIds = MessageCache.getMessagesFromUser(rawChatId, bot.getMe().get().id)
    messageIds.takeLast(n).forEach {
        runCatching {
            bot.deleteMessage(chatId, it)
            MessageCache.removeMessage(rawChatId, it)
        }.onFailure {
            println("❌ [BOT] Не удалось удалить сообщение $it: ${it.message}")
        }
    }
}

fun Bot.sendAndCacheMessage(
    chatId: ChatId,
    text: String
): TelegramBotResult<Message> {
    //todo: for test
    println("[Bot] :$text")
    val response = sendMessage(chatId, text)
    val botId = this.getMe().get().id
    response.getOrNull()?.let {
        MessageCache.add(chatId.rawChatId(), botId, it.messageId)
    }

    return response

}