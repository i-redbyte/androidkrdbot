package su.redbyte.androidkrdbot.cli.comrade

import com.github.kotlintelegrambot.entities.User
import su.redbyte.androidkrdbot.cli.message.MessageContext
import su.redbyte.androidkrdbot.data.repository.VerificationRepository

class CleanupLeftMemberListener(
    private val repository: VerificationRepository
) : NewComradeListener {
    override suspend fun handle(ctx: MessageContext, user: User) {
        repository.cancelTimer(user.id)
        repository.remove(user.id)
    }
}