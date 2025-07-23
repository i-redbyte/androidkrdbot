package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase
import su.redbyte.androidkrdbot.domain.usecase.FetchDigestUseCase.Companion.NO_NEW

class FetchDigestCmd(
    private val fetchDigest: FetchDigestUseCase
) : BotCommand {
    override val name: String = Commands.DIGEST.commandName
    override suspend fun handle(ctx: CommandContext) {
        val text = fetchDigest().trim()
        if (text.isEmpty() || text == NO_NEW) {
            ctx.reply("В нашей агентурной сети пока нет новой информации. Продолжаем вести наблюдение \uD83D\uDC40")
            return
        }
        ctx.reply(text)
    }
}