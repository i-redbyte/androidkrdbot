package su.redbyte.androidkrdbot

import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.cli.command.*
import su.redbyte.androidkrdbot.cli.comrade.CleanupLeftMemberListener
import su.redbyte.androidkrdbot.cli.comrade.VerificationNewComradeListener
import su.redbyte.androidkrdbot.infra.engine.BotEngine
import su.redbyte.androidkrdbot.cli.message.*
import su.redbyte.androidkrdbot.infra.middleware.AdminOnly
import su.redbyte.androidkrdbot.infra.middleware.Middleware
import su.redbyte.androidkrdbot.infra.middleware.RateLimit
import su.redbyte.androidkrdbot.data.repository.*
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.*
import su.redbyte.androidkrdbot.infra.hooks.CacheHooks

fun main() = runBlocking {
    val env = dotenv()
    val token = env["TELEGRAM_BOT_TOKEN"] ?: error("TELEGRAM_BOT_TOKEN is not set")
    val apiId = env["API_ID"] ?: error("API_ID is not set")
    val apiHash = env["API_HASH"] ?: error("API_HASH is not set")
    val appScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Default +
            CoroutineExceptionHandler { _, e ->
                println("⚠️ Ошибка в фоновой задаче: ${e.message}")
                e.printStackTrace()
            }
    )

    val questionRepo = QuestionRepository()
    val verificationRepo = VerificationRepository()
    val chatAdminRepo = ChatAdminRepository()
    val interrogationRepo = InterrogationRepository()
    val comradesRepo = ComradesRepository(apiId, apiHash)
    val digestRepo = DigestRepository(apiId, apiHash)
    val markovRepo = MarkovRepository.load()
    val libraryRepo = LibraryRepository()

    val getRandomQuestion = GetRandomQuestionUseCase(questionRepo)
    val scheduleVerification = ScheduleVerificationUseCase(verificationRepo, appScope, comradesRepo)
    val checkAnswer = CheckAnswerUseCase(verificationRepo, comradesRepo)
    val checkAdminRights = IsUserAdminUseCase(chatAdminRepo)
    val getAdmins = GetAdminsUseCase(chatAdminRepo)
    val checkBan = CheckBanUseCase(interrogationRepo)
    val fetchComrades = FetchComradesUseCase(comradesRepo)
    val searchArticles = SearchArticlesUseCase()
    val fetchLibraryUpdates = FetchLibraryUpdatesUseCase(libraryRepo)
    val fetchDigest = FetchDigestUseCase(digestRepo)
    val verificationState = VerificationState

    appScope.launch {
        runCatching { fetchComrades() }
            .onSuccess { comrades ->
                if (comrades.isEmpty()) {
                    println(
                        "📦 Загрузили 0 товарищей. Проверьте сессию Pyrogram (файлы bot_auth*.session в корне проекта) " +
                            "и что аккаунт API_ID/API_HASH состоит в @androidkrd."
                    )
                } else {
                    println("📦 Загрузили ${comrades.size} товарищей. ${comrades.random()}!!!")
                }
            }
            .onFailure { e ->
                println("📦 Не удалось загрузить товарищей: ${e.message}")
            }
    }
    CacheHooks.onUserBanned = { userId ->
        fetchComrades.invalidateFromCache(userId)
    }
    val commands = listOf(
        StartVerificationCmd(verificationState),
        StopVerificationCmd(verificationState),
        VerificationStatusCmd(verificationState),
        ReloadQuestionsCmd(),
        InterrogationCmd(appScope, fetchComrades, checkBan),
        CommandListCmd(),
        LootInfoCmd(searchArticles),
        FetchDigestCmd(appScope, fetchDigest),
        LibUpdatesCmd(fetchLibraryUpdates),
        ShowPolitburoMembersCmd(getAdmins),
        GameCmd(),
        KillDeadCmd(appScope, fetchComrades)
    )

    val messageListeners = listOf(
        CacheMessageListener(checkBan),
        CacheComradeListener(
            appScope,
            fetchComrades,
        ),
        AnswerListener(
            checkAnswer,
            getRandomQuestion,
            scheduleVerification,
            verificationRepo,
            comradesRepo,
            checkBan
        ),
        ReplyToMessageListener(markovRepo)
    )
    val newComradeListener = listOf(
        CleanupLeftMemberListener(verificationRepo),
        VerificationNewComradeListener(
            getRandomQuestion,
            scheduleVerification,
            checkAdminRights,
            appScope,
            checkBan
        ),
    )
    val adminOnly = AdminOnly(checkAdminRights)
    val rateLimit = RateLimit()
    val globalMW = listOf<Middleware>(rateLimit)

    val engine = BotEngine(
        token = token,
        scope = appScope,
        commands = commands,
        globalMiddlewares = globalMW,
        adminOnly = adminOnly,
        messageListeners = messageListeners,
        newComradeListeners = newComradeListener
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Товарищ Берия завершает дежурство."); appScope.cancel()
    })

    println("🕵️ Товарищ Берия приступил к работе.")

    engine.start()
}
