package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.VerificationState

class VerificationStatusCmd(private val verification: VerificationState) : BotCommand {
    override val name: String = Commands.VERIFICATION_STATUS.commandName
    override suspend fun handle(ctx: CommandContext) {
        val status = if (verification.enabled)
            "🟥 Активен — Берия следит лично 👁️"
        else
            "🟡 Неактивен — Берия выжидает 🕶️"
        ctx.reply("📋 Статус режима верификации: $status")
    }
}