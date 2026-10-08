package su.redbyte.androidkrdbot.infra.schedulers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase
import su.redbyte.androidkrdbot.infra.utils.sendAndCacheMessage
import java.time.*

class DailyTaskScheduler(
    private val scope: CoroutineScope,
    private val fetchDigest: FetchDigestUseCase,
    private val bot: Bot,
    private val chatId: ChatId,
    var hour: Int = 9,
    var minute: Int = 30,
    private val zoneId: ZoneId = resolveDigestZoneId(),
) {
    val isRunning: Boolean get() = job?.isActive == true

    private var job: Job? = null
    fun start() {
        if (job?.isActive == true) {
            println("[DailyTaskScheduler]: Already running")
            return
        }

        println("[DailyTaskScheduler]: Start with chatId = $chatId at $hour:$minute (zone=$zoneId)")

        job = scope.launch {
            while (isActive) {
                val delayMillis = calculateDelayToNextRun(LocalTime.of(hour, minute))
                println("[DailyTaskScheduler]: Sleeping for ${delayMillis / 1000}s until next run")
                delay(delayMillis)

                try {
                    println("[DailyTaskScheduler]: Running digest at ${ZonedDateTime.now(zoneId)}")
                    val text = fetchDigest().trim()
                    val message = if (text.isEmpty() || text == FetchDigestUseCase.NO_NEW) {
                        "В нашей агентурной сети пока нет новой информации. Продолжаем вести наблюдение \uD83D\uDC40"
                    } else {
                        text
                    }
                    sendDigestMessage(message)
                    println("[DailyTaskScheduler]: Digest sent at ${ZonedDateTime.now(zoneId)}")
                } catch (e: Exception) {
                    println("[DailyTaskScheduler]: Ошибка при отправке дайджеста: ${e.message}")
                    e.printStackTrace()
                    sendDigestMessage("\uD83D\uDEA8 Ошибка при отправке дайджеста: ${e.message ?: "неизвестная ошибка"}")
                }
            }
        }
    }

    private fun sendDigestMessage(message: String) {
        when (val result = bot.sendAndCacheMessage(chatId, message, ParseMode.MARKDOWN)) {
            is TelegramBotResult.Success -> return
            is TelegramBotResult.Error -> {
                println("[DailyTaskScheduler]: Markdown send failed: $result")
                when (val fallback = bot.sendMessage(chatId, message)) {
                    is TelegramBotResult.Success -> return
                    is TelegramBotResult.Error ->
                        error("Telegram отклонил сообщение дайджеста: $fallback")
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

    private fun calculateDelayToNextRun(targetTime: LocalTime): Long {
        val now = ZonedDateTime.now(zoneId)
        val nextRun = now.toLocalDate()
            .atTime(targetTime)
            .atZone(zoneId)
            .let { scheduled ->
                if (scheduled.isBefore(now)) scheduled.plusDays(1) else scheduled
            }

        return Duration.between(now, nextRun).toMillis()
    }

    companion object {
        private fun resolveDigestZoneId(): ZoneId {
            val fromEnv = System.getenv("DIGEST_TIMEZONE")?.trim()?.takeIf { it.isNotEmpty() }
                ?: runCatching { dotenv()["DIGEST_TIMEZONE"]?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()
            if (fromEnv != null) {
                return runCatching { ZoneId.of(fromEnv) }.getOrElse {
                    println("[DailyTaskScheduler]: Invalid DIGEST_TIMEZONE=$fromEnv, using Europe/Moscow")
                    ZoneId.of("Europe/Moscow")
                }
            }
            return ZoneId.of("Europe/Moscow")
        }
    }
}

