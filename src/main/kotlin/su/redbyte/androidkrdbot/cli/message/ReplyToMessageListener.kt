package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.utils.containsBotMention

class ReplyToMessageListener : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        val botUserName = ctx.bot.getMe().get().username ?: ""
        if (!ctx.message.containsBotMention(botUserName)) return
        val cleanText = ctx.message.text
            ?.replace("@$botUserName", "", ignoreCase = true)
            ?.trim()
            ?: ""
        //todo: STUB
        println(cleanText)
//        ctx.reply(cleanText)
    }
}