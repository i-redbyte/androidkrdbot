package su.redbyte.androidkrdbot.cli.message

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import su.redbyte.androidkrdbot.infra.utils.sendAndCacheMessage

class MessageContext(
    val bot: Bot,
    val message: Message
) {
    val chatId: ChatId = ChatId.fromId(message.chat.id)
    val rawChatId: Long = message.chat.id
    fun reply(text: String) =
        bot.sendAndCacheMessage(chatId, text)
}