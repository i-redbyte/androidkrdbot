package su.redbyte.androidkrdbot.data.model

class ExpropriationResult(
    val title: String,
    val description: String,
    val url: String
) {
    fun pretty(): String = "${title.trim()}\n${description.trim()}\n$url"
}