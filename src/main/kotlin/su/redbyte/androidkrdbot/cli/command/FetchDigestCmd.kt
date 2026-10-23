package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.redbyte.androidkrdbot.data.repository.DigestSchedule
import su.redbyte.androidkrdbot.data.repository.DigestScheduleStore
import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase
import su.redbyte.androidkrdbot.infra.schedulers.DailyTaskScheduler

private const val DEFAULT_HOUR = 11
private const val DEFAULT_MINUTE = 30
private const val DEFAULT_DESTINATION = "@androidkrd"

@RequireAdmin
class FetchDigestCmd(
    private val scope: CoroutineScope,
    private val fetchDigest: FetchDigestUseCase,
    private val store: DigestScheduleStore = DigestScheduleStore()
) : BotCommand {
    override val name: String = Commands.DIGEST.commandName

    private val mutex = Mutex()
    private var schedule: DigestSchedule? = null
    private var dailyTaskScheduler: DailyTaskScheduler? = null

    fun startOnStartup(bot: Bot) {
        val saved = try {
            store.load()
        } catch (error: Exception) {
            println("[FetchDigestCmd]: Не удалось прочитать расписание: ${error.message}")
            return
        }
        schedule = saved ?: DigestSchedule(
            DEFAULT_DESTINATION, DEFAULT_HOUR, DEFAULT_MINUTE, enabled = true
        ).also { initial ->
            runCatching { store.save(initial) }.onFailure { error ->
                println("[FetchDigestCmd]: Не удалось сохранить расписание: ${error.message}")
            }
        }
        val active = schedule
        if (active?.enabled == true) {
            startScheduler(bot, active)
        } else {
            println("[FetchDigestCmd]: Дайджест остановлен в сохранённых настройках")
        }
    }

    override suspend fun handle(ctx: CommandContext) = mutex.withLock {
        when (ctx.args.firstOrNull()?.lowercase()) {
            "start" -> start(ctx)
            "stop" -> stop(ctx)
            "time" -> changeTime(ctx)
            "status" -> status(ctx)
            null, "help" -> ctx.reply(buildHelpText())
            else -> ctx.reply("Неизвестная подкоманда. Используй /digest help для справки.")
        }
        Unit
    }

    private fun start(ctx: CommandContext) {
        val timeArgs = ctx.args.drop(1)
        val time = if (timeArgs.isEmpty()) {
            schedule?.let { it.hour to it.minute } ?: (DEFAULT_HOUR to DEFAULT_MINUTE)
        } else {
            parseTimeArgs(timeArgs) ?: run {
                ctx.reply("Неверный формат. Используй: /digest start [HH mm]")
                return
            }
        }
        if (dailyTaskScheduler?.isRunning == true) {
            val active = requireNotNull(schedule)
            ctx.reply("Дайджест уже запущен в %02d:%02d".format(active.hour, active.minute))
            return
        }
        val active = DigestSchedule(ctx.rawChatId.toString(), time.first, time.second, enabled = true)
        store.save(active)
        schedule = active
        startScheduler(ctx.bot, active)
        ctx.reply("Дайджест запущен. Время отправки: %02d:%02d".format(active.hour, active.minute))
    }

    private fun stop(ctx: CommandContext) {
        val active = schedule
        if (active?.enabled != true || dailyTaskScheduler?.isRunning != true) {
            ctx.reply("Дайджест не был запущен.")
            return
        }
        val stopped = active.copy(enabled = false)
        store.save(stopped)
        schedule = stopped
        dailyTaskScheduler?.stop()
        dailyTaskScheduler = null
        ctx.reply("Дайджест остановлен.")
    }

    private fun changeTime(ctx: CommandContext) {
        val (hour, minute) = parseTimeArgs(ctx.args.drop(1)) ?: run {
            ctx.reply("Неверный формат. Используй: /digest time HH mm")
            return
        }
        val previous = schedule ?: run {
            ctx.reply("Дайджест ещё не был настроен. Используй /digest start")
            return
        }
        val updated = previous.copy(hour = hour, minute = minute)
        store.save(updated)
        schedule = updated
        if (dailyTaskScheduler?.isRunning == true) dailyTaskScheduler?.changeTime(hour, minute)
        ctx.reply("Время дайджеста изменено на %02d:%02d".format(hour, minute))
    }

    private fun status(ctx: CommandContext) {
        val active = schedule
        if (active?.enabled == true && dailyTaskScheduler?.isRunning == true) {
            ctx.reply("Дайджест запущен. Время: %02d:%02d".format(active.hour, active.minute))
        } else {
            ctx.reply("Дайджест остановлен.")
        }
    }

    private fun startScheduler(bot: Bot, active: DigestSchedule) {
        dailyTaskScheduler = DailyTaskScheduler(
            scope = scope,
            fetchDigest = fetchDigest,
            bot = bot,
            chatId = active.destination.toLongOrNull()?.let(ChatId::fromId)
                ?: ChatId.fromChannelUsername(active.destination),
            hour = active.hour,
            minute = active.minute
        ).also { it.start() }
    }

    private fun buildHelpText(): String = """
        📡 Управление ежедневным дайджестом:

        /digest start [HH mm] – запустить дайджест (по умолчанию в 11:30)
        /digest stop – остановить отправку дайджеста
        /digest time HH mm – изменить время рассылки
        /digest status – показать текущий статус
        /digest help – показать эту справку

        Время указано по Москве. Пример: /digest start 10 15
    """.trimIndent()

    private fun parseTimeArgs(args: List<String>): Pair<Int, Int>? {
        if (args.size != 2) return null
        val hour = args[0].toIntOrNull() ?: return null
        val minute = args[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
