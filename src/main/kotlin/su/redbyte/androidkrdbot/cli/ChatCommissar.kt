package su.redbyte.androidkrdbot.cli

import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.cli.command.*
import su.redbyte.androidkrdbot.cli.engine.BotEngine
import su.redbyte.androidkrdbot.cli.message.*
import su.redbyte.androidkrdbot.cli.middleware.AdminOnly
import su.redbyte.androidkrdbot.data.repository.*
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.*

fun main() {
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

    val getRandomQuestion = GetRandomQuestionUseCase(questionRepo)
    val scheduleVerification = ScheduleVerificationUseCase(verificationRepo)
    val checkAnswer = CheckAnswerUseCase(verificationRepo)
    val checkAdminRights = CheckAdminRightsUseCase(chatAdminRepo)
    val getAdmins = GetAdminsUseCase(chatAdminRepo)
    val checkBan = CheckBanUseCase(interrogationRepo)
    val fetchComrades = FetchComradesUseCase(comradesRepo)
    val verificationState = VerificationState
    runBlocking {
        val preloaded = fetchComrades()
        println("📦 Загрузили ${preloaded.size} товарищей. ${preloaded.random()}!!!")
    }

    val commands = listOf(
        StartVerificationCmd(verificationState),
        StopVerificationCmd(verificationState),
        VerificationStatusCmd(verificationState),
        ReloadQuestionsCmd(),
        InterrogationCmd(appScope, fetchComrades, checkBan),
        ShowPolitburoMembersCmd(getAdmins)
    )

    val listeners = listOf(
        NewMembersListener(getRandomQuestion, scheduleVerification),
        AnswerListener(checkAnswer),
        CacheComradeListener(appScope, fetchComrades)
    )

    val adminOnly = AdminOnly(checkAdminRights)
    val globalMW = emptyList<su.redbyte.androidkrdbot.cli.middleware.Middleware>()

    val engine = BotEngine(
        token = token,
        scope = appScope,
        commands = commands,
        globalMiddlewares = globalMW,
        adminOnly = adminOnly,
        messageListeners = listeners
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Товарищ Берия завершает дежурство."); appScope.cancel()
    })

    println("🕵️ Товарищ Берия приступил к работе.")
    engine.start()
}
