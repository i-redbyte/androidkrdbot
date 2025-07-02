package su.redbyte.androidkrdbot.cli.comrade

import com.github.kotlintelegrambot.entities.ChatPermissions
import com.github.kotlintelegrambot.entities.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.cli.message.MessageContext
import su.redbyte.androidkrdbot.data.repository.MessageCache
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase
import su.redbyte.androidkrdbot.utils.rawChatId
import java.time.Instant

class VerificationNewComradeListener(
    private val getQuestion: GetRandomQuestionUseCase,
    private val scheduleVerification: ScheduleVerificationUseCase,
    private val scope: CoroutineScope
) : NewComradeListener {

    override suspend fun handle(ctx: MessageContext, user: User) {
        val chatId = ctx.chatId
        if (!VerificationState.enabled) return
        val botId = ctx.bot.getMe().get().id
        if (user.id == botId) return
        val joinMsgId = ctx.message.messageId
        MessageCache.add(chatId.rawChatId(), botId, joinMsgId)
        val until = Instant.now().plusSeconds(40).epochSecond
        println("[VerificationNewComradeListener] botId = $botId userId = ${user.id}")
        ctx.bot.restrictChatMember(
            chatId = chatId,
            userId = user.id,
            chatPermissions = ChatPermissions(
                canSendMessages = false,
                canSendMediaMessages = false,
                canSendPolls = false,
                canSendOtherMessages = false,
                canAddWebPagePreviews = false
            ),
            untilDate = until
        )
        scope.launch {
            delay(3_000)
            ctx.bot.restrictChatMember(
                chatId = chatId,
                userId = user.id,
                chatPermissions = ChatPermissions(
                    canSendMessages = true,
                    canSendMediaMessages = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddWebPagePreviews = true
                ),
                untilDate = 0L
            )
        }
        val question = getQuestion()
        val introText = "Привет, ${user.username ?: user.firstName}! Ответь на вопрос:\n${question.text}"
        ctx.reply(introText)
        scheduleVerification(user, chatId, question, ctx.bot)
    }

}