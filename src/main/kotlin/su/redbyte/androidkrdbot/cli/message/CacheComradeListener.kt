package su.redbyte.androidkrdbot.cli.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase

class CacheComradeListener(
    private val scope: CoroutineScope,
    private val fetchComrades: FetchComradesUseCase
) : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        val user = ctx.message.from ?: return
        scope.launch {
            val known = fetchComrades.findById(user.id)
            if (known == null) {
                println("🆕 Новый пользователь ${user.firstName} (${user.id}) — добавляем в кэш")
                fetchComrades.ensureCached(user.id)
            }
        }
    }
}