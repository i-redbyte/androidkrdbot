package su.redbyte.androidkrdbot.cli.command

import kotlinx.coroutines.CoroutineScope
import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase
import su.redbyte.androidkrdbot.infra.schedulers.DailyTaskScheduler

@RequireAdmin
class FetchDigestCmd(
    private val scope: CoroutineScope,
    private val fetchDigest: FetchDigestUseCase
) : BotCommand {

    override val name: String = Commands.DIGEST.commandName

    private var dailyTaskScheduler: DailyTaskScheduler? = null

    override suspend fun handle(ctx: CommandContext) {
        if (ctx.args.isEmpty()) {
            ctx.reply(buildHelpText())
            return
        }

        when (ctx.args.firstOrNull()?.lowercase()) {
            "start" -> {
                val (hour, minute) = parseTimeArgs(ctx.args.drop(1)) ?: (9 to 30)

                if (dailyTaskScheduler?.isRunning == true) {
                    ctx.reply("Дайджест уже запущен на $hour:$minute")
                    return
                }

                dailyTaskScheduler = DailyTaskScheduler(
                    scope = scope,
                    fetchDigest = fetchDigest,
                    bot = ctx.bot,
                    chatId = ctx.chatId,
                    hour = hour,
                    minute = minute
                ).also { it.start() }

                ctx.reply("Дайджест запущен. Время отправки: %02d:%02d".format(hour, minute))
            }

            "stop" -> {
                if (dailyTaskScheduler?.isRunning == true) {
                    dailyTaskScheduler?.stop()
                    ctx.reply("Дайджест остановлен.")
                } else {
                    ctx.reply("Дайджест не был запущен.")
                }
            }

            "time" -> {
                val (hour, minute) = parseTimeArgs(ctx.args.drop(1)) ?: run {
                    ctx.reply("Неверный формат. Используй: /digest time HH mm")
                    return
                }

                if (dailyTaskScheduler != null) {
                    dailyTaskScheduler?.changeTime(hour, minute)
                    ctx.reply("Время дайджеста изменено на %02d:%02d".format(hour, minute))
                } else {
                    ctx.reply("Дайджест ещё не был запущен. Используй /digest start")
                }
            }

            "status" -> {
                if (dailyTaskScheduler?.isRunning == true) {
                    ctx.reply(
                        "Дайджест запущен. Время: %02d:%02d".format(
                            dailyTaskScheduler!!.hour,
                            dailyTaskScheduler!!.minute
                        )
                    )
                } else {
                    ctx.reply("Дайджест остановлен.")
                }
            }
            "help" -> {
                ctx.reply(buildHelpText())
            }
            else -> {
                ctx.reply("Неизвестная подкоманда. Используй /digest help для справки.")
            }
        }
    }
    private fun buildHelpText(): String {
        return """
        📡 Управление ежедневным дайджестом:
        
        /digest start [HH mm] – запустить дайджест (по умолчанию в 09:30)
        /digest stop – остановить отправку дайджеста
        /digest time HH mm – изменить время рассылки
        /digest status – показать текущий статус
        /digest help – показать эту справку
        
        Пример: /digest start 10 15
    """.trimIndent()
    }

    private fun parseTimeArgs(args: List<String>): Pair<Int, Int>? {
        if (args.size < 2) return null
        val hour = args[0].toIntOrNull() ?: return null
        val minute = args[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
