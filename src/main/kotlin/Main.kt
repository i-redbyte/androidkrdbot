package su.redbyte

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import io.github.cdimascio.dotenv.dotenv
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timer

//TODO refactoring this code!!!
fun main() {
    val dotenv = dotenv()
    val botToken = dotenv["TELEGRAM_BOT_TOKEN"] ?: error("TELEGRAM_BOT_TOKEN is not set in .env")
    println("TOKEN == $botToken")
    val verificationManager = VerificationManager()
    val state = BotState()

    val bot = bot {
        token = botToken

        dispatch {
            command("start_verification") {
                state.enabled = true
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "✅ Режим проверки включён.")
            }

            command("stop_verification") {
                state.enabled = false
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "⛔ Режим проверки выключен.")
            }

            command("verification_status") {
                val status = if (state.enabled) "включён ✅" else "выключен ⛔"
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "Статус режима проверки: $status")
            }

            message {
                val newMembers = message.newChatMembers
                if (newMembers != null && state.enabled) {
                    val chatId = ChatId.fromId(message.chat.id)
                    newMembers.forEach { user ->
                        val question = verificationManager.getQuestion()
                        val userId = user.id
                        bot.sendMessage(
                            ChatId.fromId(userId),
                            text = "Привет, ${user.firstName}! Для вступления в чат ответьте на вопрос: \n${question.text}"
                        )

                        verificationManager.scheduleVerification(user, chatId, question, bot)
                    }
                }
            }

            message {
                val userId = message.from?.id ?: return@message
                val answer = message.text ?: return@message
                verificationManager.checkAnswer(userId, answer, bot)
            }
        }
    }

    bot.startPolling()
}

class BotState {
    var enabled: Boolean = true
}

class VerificationManager {
    private val pendingVerifications = ConcurrentHashMap<Long, Verification>()
    //TODO: changes questions
    private val questions = listOf(
        Question(
            "Сколько будет шесть плюс шесть?",
            listOf("12", "двенадцать")
        ),
        Question(
            "Назови любой цвет радуги",
            listOf("красный", "оранжевый", "жёлтый", "зелёный", "голубой", "синий", "фиолетовый")
        ),
    )

    fun getQuestion(): Question = questions.random()

    fun scheduleVerification(
        user: User,
        chatId: ChatId,
        question: Question,
        bot: Bot,
        timeoutSec: Long = 60
    ) {
        val verification = Verification(user, chatId, question, Instant.now().epochSecond)
        pendingVerifications[user.id] = verification

        timer(name = "verify-${user.id}", daemon = true, initialDelay = timeoutSec * 1000, period = 0) {
            if (pendingVerifications.containsKey(user.id)) {
                bot.banChatMember(chatId, user.id)
                bot.unbanChatMember(chatId, user.id)
                bot.sendMessage(chatId, text = "${user.firstName} не прошёл проверку и был удалён.")
                pendingVerifications.remove(user.id)
            }
            cancel()
        }
    }

    fun checkAnswer(
        userId: Long,
        answer: String, bot: Bot
    ) {
        val verification = pendingVerifications[userId] ?: return
        if (verification.question.isCorrect(answer)) {
            bot.sendMessage(
                verification.chatId,
                text = "${verification.user.firstName} успешно прошёл проверку! Добро пожаловать."
            )
            pendingVerifications.remove(userId)
        } else {
            bot.banChatMember(verification.chatId, userId)
            bot.unbanChatMember(verification.chatId, userId)
            bot.sendMessage(
                verification.chatId,
                text = "${verification.user.firstName} дал неправильный ответ и был удалён."
            )
            pendingVerifications.remove(userId)
        }
    }
}

data class Question(val text: String, val correctAnswers: List<String>) {
    fun isCorrect(answer: String): Boolean =
        correctAnswers.any { it.equals(answer.trim(), ignoreCase = true) }
}

data class Verification(val user: User, val chatId: ChatId, val question: Question, val timestamp: Long)
