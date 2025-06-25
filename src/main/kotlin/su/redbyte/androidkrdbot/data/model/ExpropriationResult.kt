package su.redbyte.androidkrdbot.data.model

class ExpropriationResult(
    val title: String,
    val url: String,
    val source: LootSource,
) {
    fun pretty(): String = "\uD83D\uDD39${title.trim()}\n\t\t→\t\t$url"
    fun prettyWithUrl(): String = "\uD83D\uDD39[${title.trim()}]($url)"
    override fun toString(): String = pretty()
}