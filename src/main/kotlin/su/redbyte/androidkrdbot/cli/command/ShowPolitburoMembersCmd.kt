package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.usecase.GetAdminsUseCase

class ShowPolitburoMembersCmd(private val getAdmins: GetAdminsUseCase) : BotCommand {
    override val name: String = Commands.POLITBURO.commandName

    override suspend fun handle(ctx: CommandContext) {
        val admins = getAdmins(ctx.bot, ctx.rawChatId).map { admin ->
            val username = "@${admin.userName}".takeIf { admin.userName.isNotEmpty() }
            "${admin.name} $username"
        }
        ctx.reply("Члены политбюро:\n${admins.joinToString("\n")}")
    }
}