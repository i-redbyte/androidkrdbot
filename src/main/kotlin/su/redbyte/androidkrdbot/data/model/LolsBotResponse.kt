package su.redbyte.androidkrdbot.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LolsBotResponse(
    @SerialName("banned") val banned: Boolean
)
