package su.redbyte.androidkrdbot.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")   val tagName: String,
    @SerialName("html_url")   val htmlUrl: String,
    @SerialName("published_at") val publishedAt: String // ISO-8601
)