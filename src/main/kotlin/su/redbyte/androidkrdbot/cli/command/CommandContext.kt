package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import su.redbyte.androidkrdbot.infra.utils.sendAndCacheMessage

class CommandContext(
    val bot: Bot,
    val message: Message,
    val args: List<String>
) {
    val chatId: ChatId = ChatId.fromId(message.chat.id)
    val rawChatId: Long = message.chat.id
    val userId: Long? = message.from?.id

    fun reply(text: String, parseMode: ParseMode = ParseMode.MARKDOWN_V2) =
        bot.sendAndCacheMessage(chatId, text, parseMode)

}

fun buildContext(bot: Bot, message: Message, args: List<String>) =
    CommandContext(bot = bot, message = message, args = args)
