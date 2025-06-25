package su.redbyte.androidkrdbot.domain.model

import java.time.Instant

data class LibraryUpdate(
    val name: String,
    val version: String,
    val publishedAt: Instant,
    val url: String
)