package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.VerificationState

@RequireAdmin
class StopVerificationCmd(
    private val verification: VerificationState,
) : BotCommand {

    override val name = Commands.STOP_VERIFICATION.commandName

    override suspend fun handle(ctx: CommandContext) {
        verification.enabled = false
        ctx.reply(
            """
                        🟡 Наблюдение временно приостановлено.
                        Товарищ Берия убрал блокнот, но не покидает свой пост.
                        Следите за порядком.
                    """.trimIndent()
        )
    }
}