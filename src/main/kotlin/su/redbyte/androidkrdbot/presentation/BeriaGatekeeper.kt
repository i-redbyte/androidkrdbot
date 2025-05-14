package su.redbyte.androidkrdbot.presentation

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import io.github.cdimascio.dotenv.dotenv
import su.redbyte.androidkrdbot.data.repository.QuestionRepository
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.CheckAnswerUseCase
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase


fun startBeriaGatekeeper() {
    val dotenv = dotenv()
    val token = dotenv["TELEGRAM_BOT_TOKEN"] ?: error("TELEGRAM_BOT_TOKEN not set")

    val questionRepo = QuestionRepository()
    val verificationRepo = VerificationRepository()
    val getQuestion = GetRandomQuestionUseCase(questionRepo)
    val scheduleVerification = ScheduleVerificationUseCase(verificationRepo)
    val checkAnswer = CheckAnswerUseCase(verificationRepo)

    val bot = bot {
        this.token = token
        //todo extract command to Enum
        dispatch {
            command("startVerification") {
                VerificationState.enabled = true
                bot.sendMessage(
                    ChatId.fromId(message.chat.id), """
        🟥 Режим наблюдения активирован.
        Товарищ Берия лично приступил к проверке новичков.
        Каждый входящий будет допрошен. Ответы — зафиксированы.
        """.trimIndent()
                )
            }

            command("stopVerification") {
                VerificationState.enabled = false
                bot.sendMessage(
                    ChatId.fromId(message.chat.id), """
        🟡 Наблюдение временно приостановлено.
        Товарищ Берия убрал блокнот, но продолжает поглядывать одним глазом.
        Следите за порядком.
        """.trimIndent()
                )
            }

            command("verificationStatus") {
                val status = if (VerificationState.enabled) "🟥 Активен — Берия следит лично 👁️"
                else  "🟡 Неактивен — Берия выжидает 🕶️"

                bot.sendMessage(
                    ChatId.fromId(message.chat.id), "📋 Статус режима верификации: $status"
                )
            }


            message {
                val newMembers = message.newChatMembers
                if (newMembers != null && VerificationState.enabled) {
                    val chatId = ChatId.fromId(message.chat.id)
                    newMembers.forEach { user ->
                        val question = getQuestion()
                        bot.sendMessage(
                            ChatId.fromId(user.id), "Привет, ${user.firstName}! Ответь на вопрос:\n${question.text}"
                        )
                        scheduleVerification(user, chatId, question, bot)
                    }
                }
            }

            message {
                val userId = message.from?.id ?: return@message
                val answer = message.text ?: return@message
                checkAnswer(userId, answer, bot)
            }
        }
    }
    println("🕵️ Товарищ Берия приступил к работе.")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Товарищ Берия закончил вести наблюдение.")
    })
    bot.startPolling()
}
