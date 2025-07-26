package su.redbyte.androidkrdbot.cli.command

import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.data.repository.MessageCache
import su.redbyte.androidkrdbot.domain.model.InterrogationState.*
import su.redbyte.androidkrdbot.domain.model.Comrade
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase
import su.redbyte.androidkrdbot.domain.usecase.CheckBanUseCase
import su.redbyte.androidkrdbot.infra.utils.deleteMessagesFromBot
import su.redbyte.androidkrdbot.infra.utils.deleteMessagesFromUser
import su.redbyte.androidkrdbot.infra.utils.sendAndCacheMessage

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
                    if (comrade != null) checkAndRespond(ctx, chatId, comrade, SINGLE)
                }

                ctx.args[0] == "all" -> {
                    val uid = ctx.userId ?: return@launch
                    ctx.reply("🔍 Началась проверка всех товарищей...")
                    comrades.forEach { checkAndRespond(ctx, chatId, it, ALL) }
                    ctx.reply("🔍 Проверка окончена")
                }

                ctx.args[0].startsWith("@") -> {
                    val username = ctx.args[0].trimStart('@')
                    val comrade = fetchComrades.findByUsername(username)
                    comrade?.let { checkAndRespond(ctx, chatId, it, SINGLE) }
                }

                else -> ctx.reply("⚠️ Используйте: /interrogation, /interrogation all или /interrogation @username")
            }
        }
    }

    private suspend fun checkAndRespond(
        ctx: CommandContext,
        chatId: ChatId,
        comrade: Comrade,
        state: su.redbyte.androidkrdbot.domain.model.InterrogationState
    ) {
        val usernamePart = if (comrade.userName.isNotEmpty()) "он же @${comrade.userName}" else ""
        if (state == SINGLE) ctx.reply("🔍 Проверяю товарища ${comrade.name} $usernamePart ...")
        val banned = checkBan(comrade.id)
        val resultText = if (banned) {
            """
📣 По данным Службы Внешней Разведки, товарищ ${comrade.name} признан врагом народа!
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
            ctx.bot.banChatMember(chatId, comrade.id)
            ctx.bot.unbanChatMember(chatId, comrade.id)
            deleteMessagesFromUser(ctx.bot, chatId, comrade.id)
            delay(5_000)
            deleteMessagesFromBot(ctx.bot, chatId)
        }
    }

}