package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.usecase.SearchArticlesUseCase

class LootInfoCmd(
    private val lootInfo: SearchArticlesUseCase
) : BotCommand {
    override val name: String = Commands.LOOT_THE_LOOTED.commandName

    override suspend fun handle(ctx: CommandContext) {
        if (ctx.args.isEmpty()) return
        val query = ctx.args.joinToString(" ")
        ctx.reply("Запрос на экспроприацию \"$query\" принят ведомством.")
        try {
            val results = lootInfo(query, RESULT_COUNT)
            if (results.isEmpty()) {
                ctx.reply("Не удалось провести экспроприацию. Попробуйте уточнить запрос.")
            } else {
                val grouped = results.groupBy { it.source }
                grouped.forEach { (source, results) ->
                    val message =
                        "Результат экспроприации [${source.sourceName}]:\n\n${results.joinToString("\n\n") { it.prettyWithUrl() }} "
                    ctx.reply(message)
                }
            }
        } catch (e: Exception) {
            println("[LOOT ERROR]: ${e.message}")
        }
    }

    companion object {
        private const val RESULT_COUNT = 5
    }
}