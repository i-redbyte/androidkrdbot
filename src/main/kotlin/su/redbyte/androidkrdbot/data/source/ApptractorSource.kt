package su.redbyte.androidkrdbot.data.source

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.repository.encodeUrlParam

class ApptractorSource(private val client: HttpClient) : Source {
    override val baseUrl: String = "https://apptractor.ru"

    override suspend fun search(query: String): List<ExpropriationResult> = withContext(Dispatchers.Default) {
        val url = "$baseUrl/?s=${query.encodeUrlParam()}"
        val html = client.get(url).bodyAsText()
        val doc = Jsoup.parse(html)
        doc.select("article.post")
            .mapNotNull { art ->
                val titleEl = art.selectFirst("h2.entry-title a") ?: return@mapNotNull null
                val title = titleEl.text()
                val link = titleEl.attr("href")
                val desc = art.selectFirst("div.entry-content p")?.text()?.take(200) ?: ""
                ExpropriationResult(title, desc, link)
            }
    }
}