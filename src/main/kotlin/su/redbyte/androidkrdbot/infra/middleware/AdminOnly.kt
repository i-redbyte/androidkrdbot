package su.redbyte.androidkrdbot.infra.middleware

import su.redbyte.androidkrdbot.cli.command.CommandContext
import su.redbyte.androidkrdbot.domain.usecase.IsUserAdminUseCase

class AdminOnly(
    private val isAdmin: IsUserAdminUseCase,
    private val denyText: String = "🚫 Только члены политбюро могут отдать такой приказ."
) : Middleware {

    override suspend fun intercept(ctx: CommandContext, next: suspend (CommandContext) -> Unit) {
        val uid = ctx.userId
        val isAdmin = uid != null && isAdmin(ctx.bot, ctx.rawChatId, uid)
        if (isAdmin) next(ctx) else ctx.reply(denyText)
    }
}