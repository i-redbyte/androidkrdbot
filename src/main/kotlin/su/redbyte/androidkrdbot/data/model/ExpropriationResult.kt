package su.redbyte.androidkrdbot.data.model

class ExpropriationResult(
    val title: String,
    val url: String
) {
    fun pretty(): String = "${title.trim()}\n\t\t$url"
    override fun toString(): String = pretty()
}