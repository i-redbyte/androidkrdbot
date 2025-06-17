package su.redbyte.androidkrdbot.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import su.redbyte.androidkrdbot.data.model.ExpropriationResult
import su.redbyte.androidkrdbot.data.model.LootSource
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HabrSource : Source {
    override val source = LootSource.HABR

    override suspend fun search(query: String, n: Int): List<ExpropriationResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val url = "https://habr.com/ru/search/?q=$encoded&target_type=posts&order=relevance"
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; HabrSearchBot/1.1)")
                .timeout(15_000)
                .get()


            val results: List<ExpropriationResult> = doc.select("a.tm-title__link")
                .map { ExpropriationResult(it.text(), it.absUrl("href"), source) }
                .distinctBy { it.url }
                .take(n)

            if (results.isEmpty()) {
                println("[${source.sourceName}] поиск не дал результатов")
            }
            return@withContext results
        } catch (ex: Exception) {
            println("[${source.sourceName}] Ошибка при выполнении запроса: ${ex.message}")
            return@withContext emptyList()
        }

    }
}
