package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.domain.usecase.CheckAnswerUseCase

class AnswerListener(
    private val checkAnswer: CheckAnswerUseCase
) : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        val userId = ctx.message.from?.id ?: return
        val answer = ctx.message.text ?: return
        checkAnswer(userId, answer, ctx.bot)
    }
}
