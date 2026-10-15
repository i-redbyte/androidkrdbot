package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.data.repository.MessageCache
import su.redbyte.androidkrdbot.domain.usecase.CheckBanUseCase
import su.redbyte.androidkrdbot.infra.utils.banUser

class CacheMessageListener(
    private val checkBan: CheckBanUseCase
) : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        if (ctx.message.chat.type == "private") return
        val userId = ctx.message.from?.id ?: return
        val chatId = ctx.rawChatId
        val messageId = ctx.message.messageId
        //todo:for logs
//        println("📥 Добавление в кэш: user=$userId, chat=$chatId, message=$messageId")
        MessageCache.add(chatId, userId, messageId = messageId)
        println("[CacheMessageListener]: check user [$userId] on ban")
        if (checkBan(userId)) {
            ctx.bot.banUser(ctx.chatId, userId)
            println("[CacheMessageListener]: user [$userId] is banned")
            return
        }
    }
}
