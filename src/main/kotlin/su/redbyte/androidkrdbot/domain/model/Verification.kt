package su.redbyte.androidkrdbot.domain.model

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User

data class Verification(
    val user: User,
    val chatId: ChatId,
    val question: Question,
    val timestamp: Long
)
