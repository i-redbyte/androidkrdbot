package su.redbyte.androidkrdbot.cli.message

import su.redbyte.androidkrdbot.data.repository.MarkovRepository
import su.redbyte.androidkrdbot.infra.utils.containsBotMention
import java.security.SecureRandom

class ReplyToMessageListener(
    private val markov: MarkovRepository
) : MessageListener {
    val secureRandom = SecureRandom()
    override suspend fun handle(ctx: MessageContext) {
        val botUserName = ctx.bot.getMe().get().username ?: ""
        if (!ctx.message.containsBotMention(botUserName)) return
        //todo example for seed example
//        val cleanText = ctx.message.text
//            ?.replace("@$botUserName", "", ignoreCase = true)
//            ?.trim()
//            ?.split(' ')
//            ?: emptyList()
//        println(cleanText)

        val n: Int = secureRandom.nextInt(130_000) + 1200
        println("n == $n")
        ctx.reply(markov.generate(maxTokens = n))
    }
}