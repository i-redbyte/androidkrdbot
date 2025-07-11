package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.Bot
import su.redbyte.androidkrdbot.data.repository.ChatAdminRepository

class IsUserAdminUseCase(
    private val adminRepository: ChatAdminRepository
) {
    operator fun invoke(
        bot: Bot,
        chatId: Long,
        userId: Long
    ): Boolean {
        return adminRepository.getAdmins(bot, chatId).any { it.user.id == userId }
    }
}