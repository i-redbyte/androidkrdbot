package su.redbyte.androidkrdbot.cli.middleware

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.redbyte.androidkrdbot.cli.command.CommandContext
import java.util.concurrent.ConcurrentHashMap

class RateLimit(private val limitMs: Long = 3_000) : Middleware {

    private val lastCall = ConcurrentHashMap<Long, Long>()
    private val guard = Mutex()

    override suspend fun intercept(ctx: CommandContext, next: suspend (CommandContext) -> Unit) {
        val uid = ctx.userId ?: return next(ctx)
        val now = System.currentTimeMillis()

        val allowed = guard.withLock {
            val last = lastCall[uid] ?: 0L
            if (now - last >= limitMs) {
                lastCall[uid] = now
                true
            } else false
        }

        if (allowed) next(ctx)
       //todo: else ctx.reply("⌛️ Подождите 3 сек., прежде чем отправлять следующую команду.") or nothing?
    }
}