package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.utils.fetchMembers

class FetchMembersUseCase {
    operator fun invoke(apiId: String, apiHash: String) = fetchMembers(apiId, apiHash)
}