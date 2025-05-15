package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.model.Question
import su.redbyte.androidkrdbot.domain.model.Verification
import java.time.Instant
import kotlin.concurrent.timer

class ScheduleVerificationUseCase(
    private val verificationRepository: VerificationRepository
) {
    operator fun invoke(
        user: User,
        chatId: ChatId,
        question: Question,
        bot: Bot
    ) {
        val verification = Verification(
            user,
            chatId,
            question,
            Instant.now().epochSecond
        )
        verificationRepository.add(verification)

        timer(
            name = "verify-${user.id}",
            daemon = true,
            initialDelay = TIMEOUT_MILLIS,
            period = 0
        ) {
            if (verificationRepository.contains(user.id)) {
                bot.sendMessage(chatId, "${user.firstName} не прошёл проверку и был удалён.")
                bot.banChatMember(chatId, user.id)
                bot.unbanChatMember(chatId, user.id)
                verificationRepository.remove(user.id)
            }
            cancel()
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 90L * 1000
    }
}