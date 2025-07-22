package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.model.Question
import su.redbyte.androidkrdbot.domain.model.VerificationRecord
import su.redbyte.androidkrdbot.utils.candidateName
import su.redbyte.androidkrdbot.utils.deleteMessagesFromBot
import su.redbyte.androidkrdbot.utils.deleteMessagesFromUser
import su.redbyte.androidkrdbot.utils.sendAndCacheMessage

class ScheduleVerificationUseCase(
    private val repository: VerificationRepository,
    private val scope: CoroutineScope
) {

    operator fun invoke(
        user: User,
        chatId: ChatId,
        question: Question,
        bot: Bot
    ) {
        val job = scope.launch {
            delay(TIMEOUT_MILLIS)
            repository.cancelTimer(user.id)
            repository.remove(user.id)
            when (val result = bot.getChatMember(chatId, user.id)) {
                is TelegramBotResult.Success -> {
                    val status = result.value.status
                    if (status != "left" && status != "kicked") {
                        bot.banChatMember(chatId, user.id)
                        bot.unbanChatMember(chatId, user.id)
                        deleteMessagesFromUser(bot, chatId, user.id)
                        bot.sendAndCacheMessage(
                            chatId,
                            "Товарищ ${user.candidateName()} не прошёл проверку и был удалён."
                        )
                        deleteMessagesFromBot(bot, chatId)
                    }
                }

                is TelegramBotResult.Error -> println("❌ [TIMER] Ошибка getChatMember для ${user.id}")
            }
        }
        repository.add(user.id, VerificationRecord(user, chatId, question, job))
    }

    companion object {
        private const val TIMEOUT_MILLIS = 90_000L // 90 секунд
    }
}
