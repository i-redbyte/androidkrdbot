package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.data.repository.InterrogationRepository

class CheckBanUseCase (
    private val interrogationRepository: InterrogationRepository
) {
    suspend operator fun invoke(userId: Long): Boolean {
        return interrogationRepository.checkInLolsBot(userId)
    }
}