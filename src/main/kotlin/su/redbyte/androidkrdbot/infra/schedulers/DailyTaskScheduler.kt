package su.redbyte.androidkrdbot.infra.schedulers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase
import su.redbyte.androidkrdbot.infra.utils.sendAndCacheMessage
import java.time.*
import java.util.concurrent.TimeUnit

class DailyTaskScheduler(
    private val scope: CoroutineScope,
    private val fetchDigest: FetchDigestUseCase,
    private val bot: Bot,
    private val chatId: ChatId,
    var hour: Int = 9,
    var minute: Int = 30,
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
                val delayMillis = calculateDelayToNextRun(LocalTime.of(hour, minute))
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
                    println("[DailyTaskScheduler]: Digest fetch started at ${LocalDateTime.now()} with result:\n$message")
                } catch (e: Exception) {
                    bot.sendAndCacheMessage(chatId, "\uD83D\uDEA8 Ошибка при отправке дайджеста")
                    println("[DailyTaskScheduler]: Ошибка при отправке дайджеста: ${e.localizedMessage}")
                }

                delay(TimeUnit.DAYS.toMillis(1))
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

    private fun calculateDelayToNextRun(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val nextRun = now.toLocalDate()
            .atTime(targetTime)
            .let {
                if (it.isBefore(now)) it.plusDays(1) else it
            }

        return Duration.between(now, nextRun).toMillis()
    }
}

