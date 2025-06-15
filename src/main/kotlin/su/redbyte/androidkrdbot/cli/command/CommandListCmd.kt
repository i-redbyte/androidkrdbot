package su.redbyte.androidkrdbot.cli.command

class CommandListCmd : BotCommand {
    override val name: String = Commands.COMMAND_LIST.commandName

    override suspend fun handle(ctx: CommandContext) {
        val message = Commands
            .entries
            .joinToString("\n") {
                "Команда: /${it.commandName} \n-\t${it.description}"
            }
        ctx.reply(message)
    }
}