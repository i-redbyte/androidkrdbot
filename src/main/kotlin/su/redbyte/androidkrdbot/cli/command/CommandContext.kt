package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

class CommandContext(
    val bot: Bot,
    val message: Message,
    val args: List<String>
) {
    val chatId: ChatId = ChatId.fromId(message.chat.id)
    val rawChatId: Long = message.chat.id
    val userId: Long? = message.from?.id

    suspend fun reply(text: String) =
        bot.sendMessage(chatId, text)

}

fun buildContext(bot: Bot, message: Message, args: List<String>) =
    CommandContext(bot = bot, message = message, args = args)
