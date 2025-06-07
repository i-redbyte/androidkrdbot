package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import su.redbyte.androidkrdbot.data.repository.ChatAdminRepository
import su.redbyte.androidkrdbot.domain.model.Comrade

class GetAdminsUseCase(
    private val adminRepository: ChatAdminRepository
) {
    operator fun invoke(
        bot: Bot,
        chatId: Long,
    ): List<Comrade> {
        return adminRepository.getAdmins(bot, chatId).map { admin ->
            val lastName = admin.user.lastName.takeIf { !admin.user.lastName.isNullOrEmpty() }
            Comrade(
                admin.user.id,
                "${admin.user.firstName} ${lastName ?: ""}",
                admin.user.username ?: ""
            )
        }
    }
}
