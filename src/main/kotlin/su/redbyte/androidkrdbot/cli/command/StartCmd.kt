package su.redbyte.androidkrdbot.cli.command

class StartCmd : BotCommand {
    override val name: String = Commands.START.commandName

    override suspend fun handle(ctx: CommandContext) {
        if (ctx.message.chat.type != "private") return
        ctx.reply("Напиши мне вопрос — отвечу цитатой из книги. Ответь на моё сообщение — сгенерирую фразу цепью Маркова.")
    }
}
