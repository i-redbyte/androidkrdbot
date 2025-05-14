package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import java.util.*

class CheckAnswerUseCase(
    private val verificationRepo: VerificationRepository
) {
    operator fun invoke(userId: Long, answer: String, bot: Bot) {
        val verification = verificationRepo.get(userId) ?: return
        if (verification.question.isCorrect(answer.lowercase(Locale.getDefault()))) {
            bot.sendMessage(
                verification.chatId,
                "${verification.user.firstName} успешно прошёл проверку! Добро пожаловать."
            )
        } else {
            bot.banChatMember(verification.chatId, userId)
            bot.unbanChatMember(verification.chatId, userId)
            bot.sendMessage(
                verification.chatId,
                "${verification.user.firstName} дал неправильный ответ и был удалён."
            )
        }
        verificationRepo.remove(userId)
    }
}