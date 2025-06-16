package su.redbyte.androidkrdbot.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.repository.encodeUrlParam

class HabrSource(private val client: HttpClient) : Source {
    override val baseUrl: String = "https://habr.com"

    override suspend fun search(query: String): List<ExpropriationResult> = withContext(Dispatchers.Default) {
        val url = "$baseUrl/ru/search/?q=${query.encodeUrlParam()}&target_type=posts"
        val html = client.get(url).bodyAsText()
        val doc = Jsoup.parse(html)
        doc.select("article.tm-article-snippet")
            .mapNotNull { art ->
                val titleEl = art.selectFirst("a.tm-article-snippet__title-link") ?: return@mapNotNull null
                val title = titleEl.text()
                val link = baseUrl + titleEl.attr("href")
                val desc = art.selectFirst("div.article-formatted-body")?.text()?.take(200) ?: ""
                ExpropriationResult(title, desc, link)
            }
    }
}