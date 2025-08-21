package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.WebAppInfo

class GameCmd : BotCommand {
    override val name: String = Commands.GAME.commandName

    override suspend fun handle(ctx: CommandContext) {
        val rows = listOf(
            listOf(
                InlineKeyboardButton.WebApp(
                    text = "\uD83D\uDD79\uFE0F Играть",
                    webApp = WebAppInfo(url = "https://game.red-byte.ru")
                )
            )
        )

        val kb = InlineKeyboardMarkup.create(rows)

        ctx.bot.sendMessage(
            chatId = ctx.chatId,
            text = "Поработал, отдохни!",
            replyMarkup = kb,
            messageThreadId = ctx.message.messageThreadId
        )
    }
}