package su.redbyte.androidkrdbot.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.User
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import su.redbyte.androidkrdbot.data.repository.*
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.factory.QuestionFactory
import su.redbyte.androidkrdbot.domain.model.BotCommands
import su.redbyte.androidkrdbot.domain.model.Comrade
import su.redbyte.androidkrdbot.domain.model.InterrogationState
import su.redbyte.androidkrdbot.domain.model.InterrogationState.*
import su.redbyte.androidkrdbot.domain.usecase.*

@OptIn(DelicateCoroutinesApi::class)
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
    val comradesRepository = ComradesRepository(apiId, apiHash)
    val fetchComradesUseCase = FetchComradesUseCase(comradesRepository)
    runBlocking {
        val preloaded = fetchComradesUseCase()
        println("📦 Загрузили ${preloaded.size} товарищей в кэш. ${preloaded.random()}!!!")
    }
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
                        "🚫 Только партийное руководство может отдавать приказы товарищу Берии. Ваше поведение записано в досье."
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
                val chatId = ChatId.fromId(message.chat.id)
                GlobalScope.launch {
                    val comrades = fetchComradesUseCase()

                    when {
                        args.isEmpty() -> {
                            val comrade = comrades.randomOrNull()
                            if (comrade != null) {
                                checkAndRespond(bot, chatId, comrade, SINGLE, checkComrades::invoke)
                            } else {
                                println("❌ Нет товарищей для проверки.")
                            }
                        }

                        args[0] == "all" -> {
                            val rawChatId = message.chat.id
                            val fromId = message.from?.id ?: return@launch
                            if (!checkAdminRights(bot, rawChatId, fromId)) {
                                bot.sendMessage(
                                    chatId,
                                    "🚫 Только партийное руководство может отдавать приказы товарищу Берии. Ваше поведение записано в досье."
                                )
                                return@launch
                            }
                            bot.sendMessage(chatId, "🔍 Началась проверка всех товарищей...")
                            comrades.forEach {
                                checkAndRespond(bot, chatId, it, ALL, checkComrades::invoke)
                            }
                        }

                        args[0].startsWith("@") -> {
                            val username = args[0].trimStart('@')
                            val comrade = fetchComradesUseCase.findByUsername(username)
                            if (comrade != null) {
                                checkAndRespond(bot, chatId, comrade, SINGLE, checkComrades::invoke)
                            } else {
                                println("❓ Товарищ @$username[$chatId] не найден в кэше.")
                            }
                        }

                        else -> bot.sendMessage(
                            chatId,
                            "⚠️ Используйте: /interrogation, /interrogation all или /interrogation @username"
                        )
                    }
                }
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
            message {
                val user = message.from
                if (user != null) {
                    GlobalScope.launch {
                        val known = fetchComradesUseCase.findById(user.id)
                        if (known == null) {
                            println("🆕 Новый пользователь ${user.firstName} (${user.id}) — пробуем добавить в кэш.")
                            fetchComradesUseCase.ensureCached(user.id)
                        }
                    }
                }
            }
        }
    }

    println("🕵️ Товарищ Берия приступил к работе.")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Товарищ Берия закончил вести наблюдение.")
    })

    bot.startPolling()
}

private suspend fun checkAndRespond(
    bot: Bot,
    chatId: ChatId,
    comrade: Comrade,
    state: InterrogationState,
    checkComrades: suspend (Long) -> Boolean
) {
    val usernamePart = if (comrade.userName.isNotEmpty()) "он же @${comrade.userName}" else ""
    when (state) {
        SINGLE -> bot.sendMessage(chatId, "🔍 Проверяю товарища ${comrade.name} $usernamePart ...")
        ALL -> println("🔍 Проверяю товарища ${comrade.name} $usernamePart ...")
    }

    val banned = checkComrades(comrade.id)

    val resultText = if (banned) {
        """
📣 По данным Службы внешней разведки, товарищ ${comrade.name} признан врагом народа!
Он приговаривается к высшей мере наказания.
        """
    } else {
        "✅ Товарищ ${comrade.name} чист перед партией."
    }
    if (banned) {
        bot.banChatMember(chatId, comrade.id)
        bot.unbanChatMember(chatId, comrade.id)
        bot.sendMessage(chatId, resultText)
    } else when (state) {
        SINGLE -> bot.sendMessage(chatId, resultText)
        ALL -> println(resultText)
    }
}

fun User.candidateName(): String = username?.let { "@$it" } ?: firstName
