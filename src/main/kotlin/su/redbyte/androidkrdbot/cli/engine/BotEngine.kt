package su.redbyte.androidkrdbot.cli.engine

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.chatMember
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.cli.command.BotCommand
import su.redbyte.androidkrdbot.cli.command.CommandContext
import su.redbyte.androidkrdbot.cli.command.RequireAdmin
import su.redbyte.androidkrdbot.cli.command.buildContext
import su.redbyte.androidkrdbot.cli.message.ChatMemberListener
import su.redbyte.androidkrdbot.cli.message.MessageContext
import su.redbyte.androidkrdbot.cli.message.MessageListener
import su.redbyte.androidkrdbot.cli.middleware.Middleware

class BotEngine(
    token: String,
    private val scope: CoroutineScope,
    private val commands: List<BotCommand>,
    private val globalMiddlewares: List<Middleware>,
    private val adminOnly: Middleware,
    private val messageListeners: List<MessageListener>) {

    private val telegramBot = bot {
        this.token = token

        dispatch {
            commands.forEach { cmd ->
                command(cmd.name.removePrefix("/")) {
                    scope.launch {
                        val ctx = buildContext(bot, message, args)

                        val stack = buildList {
                            addAll(globalMiddlewares)
                            if (cmd::class.annotations.any { it is RequireAdmin }) add(adminOnly)
                        }
                        val pipeline: suspend (CommandContext) -> Unit =
                            stack.foldRight({ c: CommandContext -> cmd.handle(c) }) { mw, next ->
                                { c -> mw.intercept(c, next) }
                            }
                        pipeline(ctx)
                    }
                }
            }

            message {
                scope.launch {
                    val mctx = MessageContext(bot, message)
                    messageListeners.forEach { it.handle(mctx) }
                }
            }
        }
    }

    fun start() = telegramBot.startPolling()
}

