package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import su.redbyte.androidkrdbot.utils.sendAndCacheMessage

class CommandContext(
    val bot: Bot,
    val message: Message,
    val args: List<String>
) {
    val chatId: ChatId = ChatId.fromId(message.chat.id)
    val rawChatId: Long = message.chat.id
    val userId: Long? = message.from?.id

    fun reply(text: String) =
        bot.sendAndCacheMessage(chatId, text)

}

fun buildContext(bot: Bot, message: Message, args: List<String>) =
    CommandContext(bot = bot, message = message, args = args)
