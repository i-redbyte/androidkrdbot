package su.redbyte.androidkrdbot.cli.message

import com.github.kotlintelegrambot.entities.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.domain.usecase.CheckBanUseCase
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase
import su.redbyte.androidkrdbot.infra.utils.banUser

class CacheComradeListener(
    scope: CoroutineScope,
    private val fetchComrades: FetchComradesUseCase,
    private val checkBan: CheckBanUseCase
) : MessageListener {

    init {
        scope.launch {
            runCatching { fetchComrades() }
                .onSuccess { list ->
                    known.clear()
                    known.addAll(list.map { it.id })
                }
        }
    }

    override suspend fun handle(ctx: MessageContext) {
        val message = ctx.message
        val from: User = message.from ?: return
        val krdBotId = ctx.bot.getMe().get().id
        val userId = from.id
        if (known.contains(userId)) return
        if (userId == krdBotId) return
        println("[CacheComradeListener]: check user [$userId] on ban")
        if (checkBan(userId)) {
            ctx.bot.banUser(ctx.chatId, userId)
            println("[CacheComradeListener]: user [$userId] is banned")
            return
        }
    }

    companion object {
        val known: MutableSet<Long> = mutableSetOf()
    }

}