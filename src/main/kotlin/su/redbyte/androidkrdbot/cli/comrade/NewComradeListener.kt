package su.redbyte.androidkrdbot.cli.comrade

import com.github.kotlintelegrambot.entities.User
import su.redbyte.androidkrdbot.cli.message.MessageContext

interface NewComradeListener {
    suspend fun handle(ctx: MessageContext, user: User)
}


