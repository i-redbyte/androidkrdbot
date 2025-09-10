package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.domain.model.InterrogationState.*
import su.redbyte.androidkrdbot.domain.model.Comrade
import su.redbyte.androidkrdbot.domain.model.InterrogationState
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase
import su.redbyte.androidkrdbot.domain.usecase.CheckBanUseCase
import su.redbyte.androidkrdbot.infra.utils.banUser
import su.redbyte.androidkrdbot.infra.utils.deleteMessagesFromBot

class InterrogationCmd(
    private val scope: CoroutineScope,
    private val fetchComrades: FetchComradesUseCase,
    private val checkBan: CheckBanUseCase
) : BotCommand {
    override val name: String = Commands.INTERROGATION.commandName

    override suspend fun handle(ctx: CommandContext) {
        val chatId = ctx.chatId
        scope.launch {
            val comrades = fetchComrades()
            when {
                ctx.args.isEmpty() -> {
                    val comrade = comrades.randomOrNull()
                    if (comrade != null) checkAndRespond(ctx, chatId, comrade, scope)
                }

                ctx.args[0] == "all" -> {
                    ctx.userId ?: return@launch
                    ctx.reply("🔍 Началась проверка всех товарищей...")
                    comrades.forEach { checkAndRespond(ctx, chatId, it, scope, ALL) }
                    ctx.reply("🔍 Проверка окончена")
                }

                ctx.args[0].startsWith("@") -> {
                    val username = ctx.args[0].trimStart('@')
                    val comrade = fetchComrades.findByUsername(username)
                    comrade?.let { checkAndRespond(ctx, chatId, it, scope) }
                }

                ctx.args[0] == "id" -> {
                    try {
                        val userId = ctx.args[1].filter { it.isDigit() }.toLong()
                        checkAndRespond(ctx, chatId, Comrade(userId, "$userId", ""), scope)
                    } catch (e: TypeCastException) {
                        ctx.reply("⚠️ Используйте только числовой id!")
                    }
                }

                else -> ctx.reply("⚠️ Используйте: /interrogation, /interrogation id, /interrogation all или /interrogation @username")
            }
        }
    }

    private suspend fun checkAndRespond(
        ctx: CommandContext,
        chatId: ChatId,
        comrade: Comrade,
        scope: CoroutineScope,
        state: InterrogationState = SINGLE
    ) {
        val usernamePart = if (comrade.userName.isNotEmpty()) "он же @${comrade.userName}" else ""
        if (state == SINGLE) ctx.reply("🔍 Проверяю товарища ${comrade.name} $usernamePart ...")
        val banned = checkBan(comrade.id)
        val resultText = if (banned) {
            """
📣 По данным Службы Внешней Разведки, товарищ ${comrade.name.takeIf { it.isNotEmpty() } ?: comrade.id} признан врагом народа!
Он приговаривается к высшей мере наказания.
        """
        } else {
            "✅ Товарищ ${comrade.name} чист перед партией."
        }
        when (state) {
            SINGLE -> ctx.reply(resultText)
            ALL -> if (!banned) println(resultText) else ctx.reply(resultText)
        }
        if (banned) {
            ctx.bot.banUser(chatId, comrade.id, scope)
            delay(5_000)
            deleteMessagesFromBot(ctx.bot, chatId)
        }
    }

}