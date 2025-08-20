package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.types.TelegramBotResult.Error
import com.github.kotlintelegrambot.types.TelegramBotResult.Success
import su.redbyte.androidkrdbot.data.repository.ComradesRepository
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.model.Comrade
import su.redbyte.androidkrdbot.infra.utils.*

class CheckAnswerUseCase(
    private val verificationRepository: VerificationRepository,
    private val comradesRepository: ComradesRepository
) {
    suspend operator fun invoke(
        userId: Long,
        rawAnswer: String,
        bot: Bot
    ) {
        val record = verificationRepository.get(userId) ?: return
        verificationRepository.cancelTimer(userId)
        verificationRepository.remove(userId)
        val answer = rawAnswer.trim().lowercase()
        val ok = record.question.correctAnswers.any { answer == it.trim().lowercase() }
        val chatId = record.chatId
        val user = record.user
        if (ok) {
            comradesRepository.put(Comrade(userId, user.firstName, user.candidateName()))
            bot.sendAndCacheMessage(
                chatId,
                "✔️ ${user.candidateName()} успешно прошёл проверку! Добро пожаловать."
            )
            deleteMessagesFromUser(bot, chatId, userId)
            deleteMessagesFromBot(bot, chatId,2)
            return
        }
        when (val result = bot.getChatMember(chatId, userId)) {
            is Success -> {
                val status = result.value.status
                if (status != "left" && status != "kicked") {
                    bot.banUser(chatId, userId)
                    bot.sendAndCacheMessage(
                        chatId,
                        "❌ ${user.candidateName()} дал неправильный ответ и был удалён."
                    )
                    deleteMessagesFromBot(bot, chatId)
                    comradesRepository.remove(userId)
                }
            }

            is Error -> {}
        }
    }
}

