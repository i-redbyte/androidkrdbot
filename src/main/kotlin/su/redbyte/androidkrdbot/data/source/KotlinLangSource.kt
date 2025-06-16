package su.redbyte.androidkrdbot.data.source

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.repository.encodeUrlParam

class KotlinLangSource(private val client: HttpClient) : Source {
    override val baseUrl: String = "https://kotlinlang.ru"

    override suspend fun search(query: String): List<ExpropriationResult> = withContext(Dispatchers.Default) {
        val url = "$baseUrl/search/?q=${query.encodeUrlParam()}"
        val html = client.get(url).bodyAsText()
        val doc = Jsoup.parse(html)
        doc.select("div.gsc-result")
            .mapNotNull { div ->
                val titleEl = div.selectFirst("a.gs-title") ?: return@mapNotNull null
                val title = titleEl.text()
                val link = titleEl.attr("data-ctorig")
                val desc = div.selectFirst("div.gs-bidi-start-align.gs-snippet")?.text()?.take(200) ?: ""
                ExpropriationResult(title, desc, link)
            }
    }
}