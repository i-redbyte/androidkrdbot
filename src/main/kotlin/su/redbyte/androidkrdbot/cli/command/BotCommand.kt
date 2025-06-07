package su.redbyte.androidkrdbot.cli.command

interface BotCommand {
    val name: String

    suspend fun handle(ctx: CommandContext)
}