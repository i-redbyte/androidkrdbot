package su.redbyte.androidkrdbot.infra.utils

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.*
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

fun deleteMessagesFromBot(bot: Bot, chatId: ChatId, n: Int = 3) {
    val botUserId = bot.getMe().get().id
    val allMessageIds = MessageCache.getMessagesFromUser(chatId.rawChatId(), botUserId)
    val messageIds = allMessageIds.takeLast(n)
    deleteMessages(bot, chatId, messageIds, "BOT")
}

fun Bot.sendAndCacheMessage(
    chatId: ChatId,
    text: String
): TelegramBotResult<Message> {
    val safe = escapeMarkdownV2(text)
    val response = sendMessage(chatId, safe, parseMode = ParseMode.MARKDOWN_V2)
    val botId = this.getMe().get().id
    response.getOrNull()?.let {
        MessageCache.add(chatId.rawChatId(), botId, it.messageId)
    }
    return response
}

fun Bot.banUser(chatId: ChatId, userId: Long, deleteUserMessages: Boolean = true) {
    banChatMember(chatId, userId)
    unbanChatMember(chatId, userId)
    if (deleteUserMessages) deleteMessagesFromUser(this, chatId, userId)
}

fun Message.containsBotMention(botUserName: String): Boolean =
    entities
        ?.filter { it.type == MessageEntity.Type.MENTION }
        ?.any { text?.substring(it.offset, it.offset + it.length) == "@$botUserName" }
        ?: false

private fun escapeMarkdownV2(text: String): String {
    return text.replace(Regex("""([_*\[\]()~`>#+\-=|{}.!\\])""")) { "\\${it.value}" }
}