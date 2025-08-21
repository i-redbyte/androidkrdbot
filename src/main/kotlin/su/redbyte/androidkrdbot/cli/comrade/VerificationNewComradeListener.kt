package su.redbyte.androidkrdbot.cli.comrade

import com.github.kotlintelegrambot.entities.ChatPermissions
import com.github.kotlintelegrambot.entities.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.redbyte.androidkrdbot.cli.message.MessageContext
import su.redbyte.androidkrdbot.data.repository.MessageCache
import su.redbyte.androidkrdbot.domain.VerificationState
import su.redbyte.androidkrdbot.domain.usecase.CheckBanUseCase
import su.redbyte.androidkrdbot.domain.usecase.IsUserAdminUseCase
import su.redbyte.androidkrdbot.domain.usecase.GetRandomQuestionUseCase
import su.redbyte.androidkrdbot.domain.usecase.ScheduleVerificationUseCase
import su.redbyte.androidkrdbot.infra.utils.candidateName
import su.redbyte.androidkrdbot.infra.utils.rawChatId
import java.time.Instant

class VerificationNewComradeListener(
    private val getQuestion: GetRandomQuestionUseCase,
    private val scheduleVerification: ScheduleVerificationUseCase,
    private val isUserAdmin: IsUserAdminUseCase,
    private val scope: CoroutineScope,
    private val checkBan: CheckBanUseCase
) : NewComradeListener {

    override suspend fun handle(ctx: MessageContext, user: User) {
        val inviterId = ctx.message.from?.id ?: return
        val userId = user.id
        if (inviterId != userId && isUserAdmin(ctx.bot, ctx.rawChatId, inviterId)) return
        if (!VerificationState.enabled) return
        val botId = ctx.bot.getMe().get().id
        if (userId == botId || checkBan(userId)) return
        val chatId = ctx.chatId
        val joinMsgId = ctx.message.messageId
        MessageCache.add(chatId.rawChatId(), botId, joinMsgId)
        val until = Instant.now().plusSeconds(40).epochSecond
        println("[VerificationNewComradeListener] botId = $botId userId = $userId")
        ctx.bot.restrictChatMember(
            chatId = chatId,
            userId = userId,
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
                userId = userId,
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
        val introText = "‼\uFE0F Привет, ${user.candidateName()}! Ответь на вопрос:\n${question.text}"
        ctx.reply(introText)
        scheduleVerification(user, chatId, question, ctx.bot)
    }

}