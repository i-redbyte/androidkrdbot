package su.redbyte.androidkrdbot.cli.command

import su.redbyte.androidkrdbot.domain.factory.QuestionFactory
import su.redbyte.androidkrdbot.domain.model.BotCommands

@RequireAdmin
class ReloadQuestionsCmd() : BotCommand {
    override val name: String = BotCommands.RELOAD_QUESTIONS.commandName
    override suspend fun handle(ctx: CommandContext) {
        val success = QuestionFactory.reload()
        if (success) {
            ctx.reply("✅ Вопросы успешно перезагружены. Товарищ Берия принял новые директивы.")
        } else {
            ctx.reply("❌ Ошибка при перезагрузке вопросов. Проверка остановлена.")
        }
    }
}
