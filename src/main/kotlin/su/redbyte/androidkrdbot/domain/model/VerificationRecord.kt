package su.redbyte.androidkrdbot.domain.model

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import kotlinx.coroutines.Job

data class VerificationRecord(
    val user: User,
    val chatId: ChatId,
    val question: Question,
    val job: Job
)
