package su.redbyte.androidkrdbot.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Member(val id: Long, val name: String, val userName: String)
