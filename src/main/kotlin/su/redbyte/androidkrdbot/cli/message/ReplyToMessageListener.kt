package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.data.repository.BookReplyRepository
import su.redbyte.androidkrdbot.data.repository.VerificationRepository
import su.redbyte.androidkrdbot.infra.utils.containsBotMention

class ReplyToMessageListener(
    private val bookReplies: BookReplyRepository,
    private val verificationRepository: VerificationRepository
) : MessageListener {
    override suspend fun handle(ctx: MessageContext) {
        if (ctx.message.from?.id?.let(verificationRepository::contains) == true) return
        val botUser = ctx.bot.getMe().get()
        val botUserName = botUser.username.orEmpty()
        val isReplyToBot = ctx.message.replyToMessage?.from?.id == botUser.id
        val mentionsBot = ctx.message.containsBotMention(botUserName)
        if (!isReplyToBot && !mentionsBot) return

        val messageText = ctx.message.text ?: return
        val prompt = messageText
            .let { text ->
                if (botUserName.isNotEmpty()) {
                    text.replace(Regex("@" + Regex.escape(botUserName), RegexOption.IGNORE_CASE), "")
                } else {
                    text
                }
            }
            .trim()
        ctx.reply(if (isReplyToBot) bookReplies.markov(prompt) else bookReplies.quote(prompt))
    }
}
