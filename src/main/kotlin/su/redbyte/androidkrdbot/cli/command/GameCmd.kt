package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

class GameCmd : BotCommand {
    override val name: String = Commands.GAME.commandName

    override suspend fun handle(ctx: CommandContext) {
        val botUsername = "androidkrd_beria_bot"
        val shortName = "android_krd_game"
        val deepLink = "https://t.me/$botUsername/$shortName?startapp=from_group"

        val kb = InlineKeyboardMarkup.create(
            listOf(listOf(InlineKeyboardButton.Url("🎮 Играть", deepLink)))
        )

        ctx.bot.sendMessage(
            chatId = ctx.chatId,
            text = "Поработал — отдохни!",
            replyMarkup = kb,
            messageThreadId = ctx.message.messageThreadId
        )
    }
}