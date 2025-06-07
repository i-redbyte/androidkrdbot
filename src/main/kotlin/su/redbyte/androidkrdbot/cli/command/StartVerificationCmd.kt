package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.model.BotCommands

class StartVerificationCmd(private val verification: VerificationState) : BotCommand {
    override val name: String = BotCommands.START_VERIFICATION.commandName
    override suspend fun handle(ctx: CommandContext) {
        verification.enabled = true
        ctx.reply(
            """
                        🟥 Режим наблюдения активирован.
                        Товарищ Берия лично приступил к проверке новичков.
                        Каждый входящий будет допрошен. Ответы — зафиксированы.
                    """.trimIndent()
        )
    }
}