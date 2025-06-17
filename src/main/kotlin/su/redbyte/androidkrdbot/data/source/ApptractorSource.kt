package su.redbyte.androidkrdbot.data.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AppTractorSource : Source {
    override val sourceName: String = "AppTractor"

    override suspend fun search(query: String, n: Int): List<ExpropriationResult> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val apiUrl = "https://apptractor.ru/wp-json/wp/v2/search?search=$encoded&per_page=10"

        val results: List<ExpropriationResult> = runCatching { fetchViaApi(apiUrl) }
            .getOrElse { fetchViaHtml("https://apptractor.ru/?s=$encoded", n) }

        if (results.isEmpty()) {
            println("поиск не дал результатов")
        } else {
            results.forEach { println(it.pretty()) }
        }
        return results
    }

    private fun fetchViaApi(url: String): List<ExpropriationResult> {
        val body = Jsoup.connect(url)
            .ignoreContentType(true)
            .userAgent("Mozilla/5.0 (compatible; AppTractorSearchBot/2.0)")
            .timeout(15_000)
            .execute()
            .body()

        val json = Json.parseToJsonElement(body).jsonArray
        return json.map {
            val obj = it.jsonObject
            val title = obj["title"]!!.jsonPrimitive.content
            val link = obj["url"]!!.jsonPrimitive.content
            ExpropriationResult(title, link)
        }
    }

    private fun fetchViaHtml(url: String, n: Int = 10): List<ExpropriationResult> {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; AppTractorSearchBot/2.0)")
            .timeout(15_000)
            .get()

        val anchors = doc.select(
            "article h1 a, article h2 a, article h3 a, h2.entry-title a, h3.entry-title a, a[href^=https://apptractor.ru/]"
        )

        return anchors
            .map { ExpropriationResult(it.text(), it.absUrl("href")) }
            .distinctBy { it.url }
            .take(n)
    }

}