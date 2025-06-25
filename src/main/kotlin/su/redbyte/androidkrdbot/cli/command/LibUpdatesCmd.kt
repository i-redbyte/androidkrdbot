package su.redbyte.androidkrdbot.cli.command

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import su.redbyte.androidkrdbot.domain.usecase.FetchLibraryUpdatesUseCase

class LibUpdatesCmd(
    private val fetchUpdates: FetchLibraryUpdatesUseCase
) : BotCommand {

    override val name: String = Commands.LIB_UPDATES.commandName

    override suspend fun handle(ctx: CommandContext) {
        val request = if (ctx.args.isEmpty())
            DEFAULT_LIBS
        else
            ctx.args.map { it.lowercase() }

        val updates = fetchUpdates(request)

        if (updates.isEmpty()) {
            ctx.reply("Не удалось найти свежих релизов по запросу.")
            return
        }

        val dateFormatter = DateTimeFormatter.ISO_DATE
        val msg = buildString {
            appendLine("📦 *Актуальные релизы:*")
            updates.forEach { u ->
                val date = u.publishedAt
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter)

                appendLine("▫️ [ ${u.name} ] — ${u.version} `вышел: $date`")
                appendLine("➡\uFE0F [Смотреть релиз](${u.url})\n")
            }
        }

        ctx.reply(msg)
    }

    companion object {
        private val DEFAULT_LIBS = listOf("compose", "retrofit", "coil", "kotlin", "gradle")
    }
}
