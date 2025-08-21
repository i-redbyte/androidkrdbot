package su.redbyte.androidkrdbot.cli.message

import com.github.kotlintelegrambot.entities.User
import su.redbyte.androidkrdbot.data.repository.ComradesRepository
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.model.Question
import su.redbyte.androidkrdbot.domain.usecase.CheckAnswerUseCase
import su.redbyte.androidkrdbot.domain.usecase.CheckBanUseCase
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase

class AnswerListener(
    private val checkAnswerUseCase: CheckAnswerUseCase,
    private val getRandomQuestion: GetRandomQuestionUseCase,
    private val scheduleVerification: ScheduleVerificationUseCase,
    private val verificationRepository: VerificationRepository,
    private val comradesRepository: ComradesRepository,
    private val checkBan: CheckBanUseCase
) : MessageListener {

    override suspend fun handle(ctx: MessageContext) {
        val message = ctx.message
        val from: User = message.from ?: return
        val myId = ctx.bot.getMe().get().id
        if (from.id == myId) return
        val text = message.text ?: return
        val userId = from.id
        if (checkBan(userId)) return
        val chatId = ctx.chatId
        if (verificationRepository.get(userId) != null) {
            checkAnswerUseCase(userId, text, ctx.bot)
            return
        }
        if (!VerificationState.enabled) return
        val known = comradesRepository.getAll().getOrThrow()
        if (known.none { it.id == userId }) {
            val q: Question = getRandomQuestion()
            val intro = "Привет, ${from.username ?: from.firstName}! Ответь на вопрос:\n${q.text}"
            ctx.reply(intro)
            scheduleVerification(from, chatId, q, ctx.bot)
        }
    }
}
