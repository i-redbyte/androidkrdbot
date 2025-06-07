package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.VerificationState

class StartVerificationCmd(private val verification: VerificationState) : BotCommand {
    override val name: String = Commands.START_VERIFICATION.commandName
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