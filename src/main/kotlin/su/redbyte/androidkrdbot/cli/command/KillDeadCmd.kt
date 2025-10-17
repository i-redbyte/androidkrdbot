package su.redbyte.androidkrdbot.cli.command

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.domain.model.Comrade
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase
import su.redbyte.androidkrdbot.infra.utils.banUser

class KillDeadCmd(
    private val scope: CoroutineScope,
    private val fetchComrades: FetchComradesUseCase,
) : BotCommand {
    override val name: String = Commands.KILL_DEAD.commandName

    override suspend fun handle(ctx: CommandContext) {
        val chatId = ctx.chatId
        val deadSouls = mutableListOf<Long>()
        scope.launch {
            val comrades = fetchComrades()
            comrades.forEach { comrade: Comrade ->
                if (comrade.name.isEmpty() && comrade.userName.isEmpty()) {
                    deadSouls.add(comrade.id)
                    ctx.bot.banUser(chatId, comrade.id, scope)
                }
            }
            if (deadSouls.isNotEmpty()) {
                ctx.reply("Очищено мертвых душ: ${deadSouls.size}")
                println("Мертвые души:${deadSouls.joinToString(" ")}")
            } else {
                ctx.reply("Мертвых душ в партии не обнаружено")
            }
        }
    }
}