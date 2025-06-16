package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.usecase.SearchArticlesUseCase

class LootInfoCmd(
    private val lootInfo: SearchArticlesUseCase
) : BotCommand {
    override val name: String = Commands.LOOT_THE_LOOTED.commandName

    override suspend fun handle(ctx: CommandContext) {
        println("ARG: ${ctx.args.size}")
        if (ctx.args.isEmpty()) return
        val query = ctx.args.joinToString(" ")
        println("query = $query")
        try {
            val results = lootInfo(query)
            if (results.isEmpty()) {
                ctx.reply("Не удалось добыть информацию. Попробуйте изменить запрос.")
            } else {
                ctx.reply("Результат экспроприации:\n${results.joinToString("\n") { it.pretty() }}")
            }
        } catch (e: Exception) {
            println("[LOOT ERROR]: ${e.message}")
        }
    }
}