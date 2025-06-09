package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.model.Question
import su.redbyte.androidkrdbot.domain.model.Verification
import su.redbyte.androidkrdbot.utils.candidateName
import java.util.Timer
import java.util.TimerTask

class ScheduleVerificationUseCase(
    private val verificationRepository: VerificationRepository
) {
    operator fun invoke(
        user: User,
        chatId: ChatId,
        question: Question,
        bot: Bot
    ) {
        println("🕓 [VERIFICATION] Пользователь ${user.firstName} (${user.id}) добавлен на проверку")
        verificationRepository.add(Verification(user, chatId, question))

        Timer("verify-${user.id}", true).schedule(object : TimerTask() {
            override fun run() {
                println("⏰ [TIMER] Сработал таймер для ${user.firstName} (${user.id})")

                if (!verificationRepository.contains(user.id)) {
                    println("✅ [TIMER] ${user.firstName} уже прошёл/провалил проверку — пропуск удаления")
                    return
                }

                verificationRepository.remove(user.id)

                when (val result = bot.getChatMember(chatId, user.id)) {
                    is TelegramBotResult.Success -> {
                        val status = result.value.status
                        println("👁️ [TIMER] Статус ${user.firstName}: $status")

                        if (status != "left" && status != "kicked") {
                            bot.banChatMember(chatId, user.id)
                            bot.unbanChatMember(chatId, user.id)
                            bot.sendMessage(chatId, "Товарищ ${user.candidateName()} не прошёл проверку и был удалён.")
                            println("✅ ${user.firstName} удалён по таймеру")
                        } else {
                            println("👻 ${user.firstName} покинул чат до проверки")
                        }
                    }

                    is TelegramBotResult.Error -> {
                        println("❌ [TIMER] Ошибка при getChatMember")
                    }
                }
            }
        }, TIMEOUT_MILLIS)
    }

    companion object {
        private const val TIMEOUT_MILLIS = 90_000L // 90 секунд
    }
}
