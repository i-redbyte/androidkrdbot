package su.redbyte.androidkrdbot.presentation

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import io.github.cdimascio.dotenv.dotenv
import su.redbyte.androidkrdbot.data.repository.ChatAdminRepository
import su.redbyte.androidkrdbot.data.repository.QuestionRepository
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.factory.QuestionFactory
import su.redbyte.androidkrdbot.domain.model.BotCommands
import su.redbyte.androidkrdbot.domain.usecase.CheckAdminRightsUseCase
import su.redbyte.androidkrdbot.domain.usecase.CheckAnswerUseCase
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase

fun startBeriaGatekeeper() {
    val dotenv = dotenv()
    val token = dotenv["TELEGRAM_BOT_TOKEN"] ?: error("TELEGRAM_BOT_TOKEN is not set")

    val questionRepo = QuestionRepository()
    val verificationRepo = VerificationRepository()
    val getQuestion = GetRandomQuestionUseCase(questionRepo)
    val scheduleVerification = ScheduleVerificationUseCase(verificationRepo)
    val checkAnswer = CheckAnswerUseCase(verificationRepo)
    val adminRepo = ChatAdminRepository()
    val checkAdminRights = CheckAdminRightsUseCase(adminRepo)

    val bot = bot {
        this.token = token

        dispatch {
            command(BotCommands.START_VERIFICATION.commandName) {
                VerificationState.enabled = true
                bot.sendMessage(
                    ChatId.fromId(message.chat.id), """
                        🟥 Режим наблюдения активирован.
                        Товарищ Берия лично приступил к проверке новичков.
                        Каждый входящий будет допрошен. Ответы — зафиксированы.
                    """.trimIndent()
                )
            }

            command(BotCommands.STOP_VERIFICATION.commandName) {
                val rawChatId = message.chat.id
                val chatId = ChatId.fromId(rawChatId)
                val fromId = message.from?.id ?: return@command

                if (!checkAdminRights(bot, rawChatId, fromId)) {
                    bot.sendMessage(
                        chatId,
                        "🚫 Только администрация может отдавать приказы товарищу Берии. Ваше поведение записано в досье."
                    )
                    return@command
                }

                VerificationState.enabled = false
                bot.sendMessage(
                    chatId, """
                        🟡 Наблюдение временно приостановлено.
                        Товарищ Берия убрал блокнот, но продолжает поглядывать одним глазом.
                        Следите за порядком.
                    """.trimIndent()
                )
            }

            command(BotCommands.VERIFICATION_STATUS.commandName) {
                val status = if (VerificationState.enabled)
                    "🟥 Активен — Берия следит лично 👁️"
                else
                    "🟡 Неактивен — Берия выжидает 🕶️"

                bot.sendMessage(
                    ChatId.fromId(message.chat.id),
                    "📋 Статус режима верификации: $status"
                )
            }

            command(BotCommands.RELOAD_QUESTIONS.commandName) {
                val chatId = ChatId.fromId(message.chat.id)
                val userId = message.from?.id ?: return@command

                if (!checkAdminRights(bot, chatId.id, userId)) {
                    bot.sendMessage(
                        chatId,
                        "🚫 Только администрация может вмешиваться в арсенал товарища Берии."
                    )
                    return@command
                }

                val success = QuestionFactory.reload()
                if (success) {
                    bot.sendMessage(chatId, "✅ Вопросы успешно перезагружены. Товарищ Берия принял новые директивы.")
                } else {
                    bot.sendMessage(chatId, "❌ Ошибка при перезагрузке вопросов. Проверка остановлена.")
                }
            }

            message {
                val newMembers = message.newChatMembers
                if (newMembers != null) {
                    val chatId = ChatId.fromId(message.chat.id)
                    println("👤 Обнаружены новые участники: ${newMembers.joinToString { "${it.firstName} (${it.id})" }}")
                    println("🛡 Статус режима верификации: ${VerificationState.enabled}")

                    if (VerificationState.enabled) {
                        newMembers.forEach { user ->
                            val question = getQuestion()
                            val introText = "Привет, ${user.firstName}! Ответь на вопрос:\n${question.text}"

                            bot.sendMessage(chatId, introText) // ✅ теперь сообщение в группе

                            println("📨 Вопрос задан ${user.firstName} (${user.id}) в чате ${chatId}")
                            scheduleVerification(user, chatId, question, bot)
                        }
                    } else {
                        println("⚠️ Верификация отключена — пользователи не проверяются.")
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
