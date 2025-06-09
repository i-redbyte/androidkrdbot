package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.data.repository.MessageCache

class CacheMessageListener : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        val userId = ctx.message.from?.id ?: return
        val chatId = ctx.rawChatId
        val messageId = ctx.message.messageId
        //todo:for logs
//        println("📥 Добавление в кэш: user=$userId, chat=$chatId, message=$messageId")
        MessageCache.add(chatId, userId, messageId = messageId)
    }
}