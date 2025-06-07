package su.redbyte.androidkrdbot.cli.middleware

import su.redbyte.androidkrdbot.cli.command.CommandContext
import su.redbyte.androidkrdbot.domain.usecase.CheckAdminRightsUseCase

class AdminOnly(
    private val checkAdmin: CheckAdminRightsUseCase,
    private val denyText: String = "🚫 Только члены политбюро могут отдать такой приказ."
) : Middleware {

    override suspend fun intercept(ctx: CommandContext, next: suspend (CommandContext) -> Unit) {
        val uid = ctx.userId
        val isAdmin = uid != null && checkAdmin(ctx.bot, ctx.rawChatId, uid)
        if (isAdmin) next(ctx) else ctx.reply(denyText)
    }
}