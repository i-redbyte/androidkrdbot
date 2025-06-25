package su.redbyte.androidkrdbot.cli

import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.cli.command.*
import su.redbyte.androidkrdbot.cli.comrade.VerificationNewComradeListener
import su.redbyte.androidkrdbot.cli.engine.BotEngine
import su.redbyte.androidkrdbot.cli.message.*
import su.redbyte.androidkrdbot.cli.middleware.AdminOnly
import su.redbyte.androidkrdbot.cli.middleware.Middleware
import su.redbyte.androidkrdbot.cli.middleware.RateLimit
import su.redbyte.androidkrdbot.data.repository.*
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.*

fun main() = runBlocking {
    val env = dotenv()
    val token = env["TELEGRAM_BOT_TOKEN"] ?: error("TELEGRAM_BOT_TOKEN is not set")
    val apiId = env["API_ID"] ?: error("API_ID is not set")
    val apiHash = env["API_HASH"] ?: error("API_HASH is not set")
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val questionRepo = QuestionRepository()
    val verificationRepo = VerificationRepository()
    val chatAdminRepo = ChatAdminRepository()
    val interrogationRepo = InterrogationRepository()
    val comradesRepo = ComradesRepository(apiId, apiHash)
    val markovRepo = MarkovRepository.load()
    val libraryRepo = LibraryRepository()

    val getRandomQuestion = GetRandomQuestionUseCase(questionRepo)
    val scheduleVerification = ScheduleVerificationUseCase(verificationRepo)
    val checkAnswer = CheckAnswerUseCase(verificationRepo)
    val checkAdminRights = CheckAdminRightsUseCase(chatAdminRepo)
    val getAdmins = GetAdminsUseCase(chatAdminRepo)
    val checkBan = CheckBanUseCase(interrogationRepo)
    val fetchComrades = FetchComradesUseCase(comradesRepo)
    val searchArticles = SearchArticlesUseCase()
    val fetchLibraryUpdates = FetchLibraryUpdatesUseCase(libraryRepo)
    val verificationState = VerificationState

    appScope.launch {
        val comrades = fetchComrades()
        println("📦 Загрузили ${comrades.size} товарищей. ${comrades.random()}!!!")
    }
    val commands = listOf(
        StartVerificationCmd(verificationState),
        StopVerificationCmd(verificationState),
        VerificationStatusCmd(verificationState),
        ReloadQuestionsCmd(),
        InterrogationCmd(appScope, fetchComrades, checkBan),
        ShowPolitburoMembersCmd(getAdmins),
        CommandListCmd(),
        LootInfoCmd(searchArticles),
        LibUpdatesCmd(fetchLibraryUpdates)
    )

    val messageListeners = listOf(
        CacheMessageListener(),
        CacheComradeListener(appScope, fetchComrades),
        AnswerListener(checkAnswer),
        ReplyToMessageListener(markovRepo)
    )
    val newComradeListener = listOf(
        VerificationNewComradeListener(getRandomQuestion, scheduleVerification, appScope)
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
