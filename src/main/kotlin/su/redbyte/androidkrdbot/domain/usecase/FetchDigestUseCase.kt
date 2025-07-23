package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.data.repository.DigestRepository

class FetchDigestUseCase(
    private val repo: DigestRepository
) {
    suspend operator fun invoke(): String {
        val result = repo.getDigest()
        if (result.isFailure) return "\uD83D\uDC80Наша агентурная сеть раскрыта, явки провалены, информация не добыта."
        val text = result.getOrNull()
        if (text == null || text == NO_NEW) return ""
        return text
    }

    companion object {
        private const val NO_NEW = "NO_NEW" //convert to ENUM
    }
}