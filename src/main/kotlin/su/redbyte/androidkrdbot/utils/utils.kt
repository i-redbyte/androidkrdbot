package su.redbyte.androidkrdbot.utils

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import su.redbyte.androidkrdbot.data.repository.MessageCache

fun User.candidateName(): String = username?.let { "@$it" } ?: firstName

fun ChatId.rawChatId(): Long = when (this) {
    is ChatId.Id -> this.id
    else -> error("❌ ChatId не содержит числового ID")
}

private fun deleteMessages(
    bot: Bot,
    chatId: ChatId,
    messageIds: List<Long>,
    source: String = ""
) {
    val rawChatId = chatId.rawChatId()
    messageIds.forEach { messageId ->
        runCatching {
            bot.deleteMessage(chatId, messageId)
            MessageCache.removeMessage(rawChatId, messageId)
        }.onFailure { e ->
            val errorSource = if (source.isNotEmpty()) "[$source] " else ""
            println("❌ ${errorSource}Не удалось удалить сообщение $messageId: ${e.message}")
        }
    }
}

fun deleteMessagesFromUser(bot: Bot, chatId: ChatId, userId: Long) {
    val messageIds = MessageCache.getMessagesFromUser(chatId.rawChatId(), userId)
    deleteMessages(bot, chatId, messageIds, "USER")
}

fun deleteMessagesFromBot(bot: Bot, chatId: ChatId, n: Int = 2) {
    val botUserId = bot.getMe().get().id
    val allMessageIds = MessageCache.getMessagesFromUser(chatId.rawChatId(), botUserId)
    val messageIds = allMessageIds.takeLast(n)
    deleteMessages(bot, chatId, messageIds, "BOT")
}

fun Bot.sendAndCacheMessage(
    chatId: ChatId,
    text: String
): TelegramBotResult<Message> {
    //todo: for test
    println("[Bot]: $text")
    val response = sendMessage(chatId, text)
    val botId = this.getMe().get().id
    response.getOrNull()?.let {
        MessageCache.add(chatId.rawChatId(), botId, it.messageId)
    }
    return response
}

fun Message.containsBotMention(botUserName: String): Boolean =
    entities
        ?.filter { it.type == MessageEntity.Type.MENTION }
        ?.any { text?.substring(it.offset, it.offset + it.length) == "@$botUserName" }
        ?: false

