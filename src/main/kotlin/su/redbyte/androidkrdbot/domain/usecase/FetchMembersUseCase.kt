package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.utils.fetchMembers

class FetchMembersUseCase {
    //todo: extract to repository and uee cache
    operator fun invoke(apiId: String, apiHash: String) = fetchMembers(apiId, apiHash)
}