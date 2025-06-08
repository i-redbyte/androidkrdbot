package su.redbyte.androidkrdbot.cli.message

import com.github.kotlintelegrambot.dispatcher.handlers.ChatMemberHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ChatMember
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase

interface ChatMemberListener {
    suspend fun handle(env: ChatMemberHandlerEnvironment)
}

private fun ChatMember.isActive(): Boolean = status in listOf("member", "administrator", "creator")
private fun ChatMember.isInactive(): Boolean = status in listOf("left", "kicked")


class VerificationChatMemberListener(
    private val getQuestion: GetRandomQuestionUseCase,
    private val scheduleVerification: ScheduleVerificationUseCase
) : ChatMemberListener {

    override suspend fun handle(env: ChatMemberHandlerEnvironment) {
        if (!VerificationState.enabled) return

        val upd = env.chatMember
        val becameMember = upd.newChatMember.isActive() && upd.oldChatMember.isInactive()
        println("[VerificationChatMemberListener] becameMember == $becameMember") //todo: remove after debug
        if (!becameMember) return

        val botId = env.bot.getMe().get().id
        val user = upd.newChatMember.user
        if (user.id == botId) return

        val chatId = ChatId.fromId(upd.chat.id)
        val question = getQuestion()
        val intro = "Привет, ${user.username ?: user.firstName}! " +
                "Ответь на вопрос:\n${question.text}"
        env.bot.sendMessage(chatId, intro)
        scheduleVerification(user, chatId, question, env.bot)
    }
}
