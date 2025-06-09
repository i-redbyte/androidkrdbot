package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.types.TelegramBotResult
import kotlinx.coroutines.delay
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.utils.candidateName
import su.redbyte.androidkrdbot.utils.deleteMessagesFromBot
import su.redbyte.androidkrdbot.utils.deleteMessagesFromUser
import su.redbyte.androidkrdbot.utils.sendAndCacheMessage

class CheckAnswerUseCase(
    private val verificationRepository: VerificationRepository
) {
    operator suspend fun invoke(
        userId: Long,
        answer: String,
        bot: Bot
    ) {
        val verification = verificationRepository.get(userId) ?: return
        val chatId = verification.chatId
        val user = verification.user

        verificationRepository.remove(userId)

        if (verification.question.isCorrect(answer)) {
            bot.sendAndCacheMessage(chatId, "${user.candidateName()} успешно прошёл проверку! Добро пожаловать.")
            println("✅ ${user.candidateName()} прошёл проверку")
        } else {
            when (val result = bot.getChatMember(chatId, userId)) {
                is TelegramBotResult.Success -> {
                    val status = result.value.status
                    println("👁️ [ANSWER] Статус ${user.firstName}: $status")

                    if (status != "left" && status != "kicked") {
                        bot.banChatMember(chatId, userId)
                        bot.unbanChatMember(chatId, userId)
                        deleteMessagesFromUser(bot, chatId, userId)
                        bot.sendAndCacheMessage(chatId, "Товарищ ${user.candidateName()} дал неправильный ответ и был удалён.")
                        println("✅ ${user.firstName} удалён за неправильный ответ")
                        delay(5_000)
                        deleteMessagesFromBot(bot, chatId)
                    } else {
                        println("👻 ${user.firstName} уже не в чате")
                    }
                }

                is TelegramBotResult.Error -> {
                    println("❌ [ANSWER] Ошибка при getChatMember:")
                }
            }
        }
    }
}

