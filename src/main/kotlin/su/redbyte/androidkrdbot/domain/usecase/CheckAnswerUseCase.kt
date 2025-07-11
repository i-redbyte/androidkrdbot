package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.types.TelegramBotResult.Error
import com.github.kotlintelegrambot.types.TelegramBotResult.Success
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.utils.candidateName
import su.redbyte.androidkrdbot.utils.deleteMessagesFromBot
import su.redbyte.androidkrdbot.utils.deleteMessagesFromUser
import su.redbyte.androidkrdbot.utils.sendAndCacheMessage

class CheckAnswerUseCase(
    private val verificationRepository: VerificationRepository
) {
    operator fun invoke(
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
            bot.sendAndCacheMessage(
                chatId,
                "✔️ ${user.candidateName()} успешно прошёл проверку! Добро пожаловать."
            )
            return
        }

        when (val result = bot.getChatMember(chatId, userId)) {
            is Success -> {
                val status = result.value.status
                if (status != "left" && status != "kicked") {
                    bot.banChatMember(chatId, userId)
                    bot.unbanChatMember(chatId, userId)
                    deleteMessagesFromUser(bot, chatId, userId)
                    bot.sendAndCacheMessage(
                        chatId,
                        "❌ ${user.candidateName()} дал неправильный ответ и был удалён."
                    )
                    deleteMessagesFromBot(bot, chatId)
                }
            }

            is Error -> {
                println("❌ [ANSWER] Ошибка getChatMember для $userId")
            }
        }
    }
}
