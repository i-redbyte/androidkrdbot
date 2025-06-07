package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase

class NewMembersListener(
    private val getQuestion: GetRandomQuestionUseCase,
    private val scheduleVerification: ScheduleVerificationUseCase
) : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        val newMembers = ctx.message.newChatMembers ?: return
        val chatId = ctx.chatId
        if (!VerificationState.enabled) return

        val botId = ctx.bot.getMe().get().id
        newMembers.forEach { user ->
            if (user.id == botId) return@forEach
            val question = getQuestion()
            val introText = "Привет, ${user.username?:user.firstName}! Ответь на вопрос:\n${question.text}"
            ctx.bot.sendMessage(chatId, introText)
            scheduleVerification(user, chatId, question, ctx.bot)
        }
    }
}
