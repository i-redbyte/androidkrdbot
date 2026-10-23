package su.redbyte.androidkrdbot.infra.schedulers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase
import su.redbyte.androidkrdbot.infra.utils.sendAndCacheMessage
import java.time.*

class DailyTaskScheduler(
    private val scope: CoroutineScope,
    private val fetchDigest: FetchDigestUseCase,
    private val bot: Bot,
    private val chatId: ChatId,
    var hour: Int = 11,
    var minute: Int = 30,
    private val clock: Clock = Clock.system(ZoneId.of("Europe/Moscow")),
) {
    val isRunning: Boolean get() = job?.isActive == true

    private var job: Job? = null
    fun start() {
        if (job?.isActive == true) {
            println("[DailyTaskScheduler]: Already running")
            return
        }

        println("[DailyTaskScheduler]: Start with chatId = $chatId at $hour:$minute")

        job = scope.launch {
            while (isActive) {
                val delayMillis = millisUntilNext(LocalTime.of(hour, minute), clock)
                println("[DailyTaskScheduler]: Sleeping for ${delayMillis / 1000}s until next run")
                delay(delayMillis)

                try {
                    val text = fetchDigest().trim()
                    val message = if (text.isEmpty() || text == FetchDigestUseCase.NO_NEW) {
                        "В нашей агентурной сети пока нет новой информации. Продолжаем вести наблюдение \uD83D\uDC40"
                    } else {
                        text
                    }
                    bot.sendAndCacheMessage(chatId, message, ParseMode.MARKDOWN)
                    println("[DailyTaskScheduler]: Digest fetch started at ${ZonedDateTime.now(clock)} with result:\n$message")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    bot.sendAndCacheMessage(chatId, "\uD83D\uDEA8 Ошибка при отправке дайджеста")
                    println("[DailyTaskScheduler]: Ошибка при отправке дайджеста: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stop() {
        if (job?.isActive == true) {
            println("[DailyTaskScheduler]: Stopping scheduler")
            job?.cancel()
            job = null
        } else {
            println("[DailyTaskScheduler]: Scheduler is not running")
        }
    }

    fun changeTime(newHour: Int, newMinute: Int) {
        println("[DailyTaskScheduler]: Changing time to $newHour:$newMinute")
        hour = newHour
        minute = newMinute
        stop()
        start()
    }
}

internal fun millisUntilNext(targetTime: LocalTime, clock: Clock): Long {
    val now = ZonedDateTime.now(clock)
    val today = now.toLocalDate().atTime(targetTime).atZone(clock.zone)
    val nextRun = if (today.isAfter(now)) today else today.plusDays(1)
    return Duration.between(now, nextRun).toMillis()
}
