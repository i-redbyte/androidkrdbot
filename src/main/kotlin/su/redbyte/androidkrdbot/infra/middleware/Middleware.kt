package su.redbyte.androidkrdbot.infra.middleware

import su.redbyte.androidkrdbot.cli.command.CommandContext

fun interface Middleware {
    suspend fun intercept(ctx: CommandContext, next: suspend (CommandContext) -> Unit)
}