package su.redbyte.androidkrdbot.presentation

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import io.github.cdimascio.dotenv.dotenv
import su.redbyte.androidkrdbot.data.repository.ChatAdminRepository
import su.redbyte.androidkrdbot.data.repository.InterrogationRepository
import su.redbyte.androidkrdbot.data.repository.QuestionRepository
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.factory.QuestionFactory
import su.redbyte.androidkrdbot.domain.model.BotCommands
import su.redbyte.androidkrdbot.domain.usecase.*

fun startBeriaGatekeeper() {
    val dotenv = dotenv()
    val token = dotenv["TELEGRAM_BOT_TOKEN"] ?: error("TELEGRAM_BOT_TOKEN is not set")
    val apiId = dotenv["API_ID"] ?: error("API_ID is not set")
    val apiHash = dotenv["API_HASH"] ?: error("API_HASH is not set")

    val questionRepository = QuestionRepository()
    val verificationRepository = VerificationRepository()
    val chatAdminRepository = ChatAdminRepository()
    val interrogationRepository = InterrogationRepository()
    val checkComrades = CheckBanUseCase(interrogationRepository)
    val getQuestion = GetRandomQuestionUseCase(questionRepository)
    val scheduleVerification = ScheduleVerificationUseCase(verificationRepository)
    val checkAnswer = CheckAnswerUseCase(verificationRepository)
    val checkAdminRights = CheckAdminRightsUseCase(chatAdminRepository)
    val fetchMembersUseCase = FetchMembersUseCase()
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
            command(BotCommands.INTERROGATION.commandName) {
                val comrades = fetchMembersUseCase(apiId, apiHash) //TODO: fix logic
                val chatId = ChatId.fromId(message.chat.id)
                val comrad = comrades.random()
                val username = if (comrad.userName.isNotEmpty()) "он же ${comrad.userName}" else ""
                bot.sendMessage(chatId, "🔍 Проверяю товарища ${comrad.name} $username ...")

                val banned = checkComrades(comrad.id)

                val resultText = if (banned) {
                    "🚫 Товарищ ${comrad.name} занесён в чёрный список!"
                } else {
                    "✅ Товарищ ${comrad.name} чист перед партией."
                }

                bot.sendMessage(chatId, resultText)
            }
            message {
                val newMembers = message.newChatMembers
                if (newMembers != null) {
                    val chatId = ChatId.fromId(message.chat.id)
                    println("👤 Обнаружены новые участники: ${newMembers.joinToString { "${it.firstName} (${it.id})" }}")
                    println("🛡 Статус режима верификации: ${VerificationState.enabled}")

                    if (VerificationState.enabled) {
                        val botId = bot.getMe().get().id
                        newMembers.forEach { user ->
                            if (user.id == botId) {
                                println("🤖 Бот добавлен в чат, игнорируем.")
                                return@forEach
                            }
                            val question = getQuestion()
                            val introText = "Привет, ${user.candidateName()}! Ответь на вопрос:\n${question.text}"
                            bot.sendMessage(chatId, introText)
                            println("📨 Вопрос задан ${user.firstName} (${user.id}) в чате $chatId")
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

fun User.candidateName(): String = username?.let { "@$it" } ?: firstName
