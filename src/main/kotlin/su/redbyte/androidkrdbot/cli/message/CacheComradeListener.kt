package su.redbyte.androidkrdbot.cli.message

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase

class CacheComradeListener(
    scope: CoroutineScope,
    private val fetchComrades: FetchComradesUseCase
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

    override suspend fun handle(ctx: MessageContext) = Unit

    companion object {
        val known: MutableSet<Long> = mutableSetOf()
    }

}