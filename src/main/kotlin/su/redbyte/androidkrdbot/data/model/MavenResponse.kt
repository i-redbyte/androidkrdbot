package su.redbyte.androidkrdbot.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class MavenResponse(
    val response: ResponsePart
) {
    @Serializable
    data class ResponsePart(val docs: List<Doc>)

    @Serializable
    data class Doc(
        @SerialName("g") val group: String,
        @SerialName("a") val artifact: String,
        @SerialName("latestVersion") val latestVersion: String,
        val timestamp: Long
    )
}
