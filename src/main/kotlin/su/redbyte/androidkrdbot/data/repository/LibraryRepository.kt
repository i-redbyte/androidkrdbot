package su.redbyte.androidkrdbot.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import su.redbyte.androidkrdbot.data.TtlCache
import su.redbyte.androidkrdbot.data.model.GitHubRelease
import su.redbyte.androidkrdbot.data.model.MavenResponse
import su.redbyte.androidkrdbot.domain.model.LibraryUpdate
import java.net.URLEncoder

class LibraryRepository(
    cacheTtl: Duration = Duration.ofMinutes(45)
) {

    private sealed interface Meta
    private data class Maven(val group: String, val artifact: String) : Meta
    private data class GitHub(val owner: String, val repo: String)    : Meta

    private val libs = mapOf(
        "retrofit" to Maven("com.squareup.retrofit2", "retrofit"),
        "coil"     to GitHub("coil-kt", "coil"),
        "compose"  to Maven("androidx.compose.ui", "ui"),
        "kotlin"   to GitHub("JetBrains", "kotlin")
    )

    private val cache = TtlCache<LibraryUpdate>(cacheTtl.toMillis())

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine { requestTimeout = 30.seconds.inWholeMilliseconds }
    }


    suspend fun latestFor(name: String): LibraryUpdate? = withContext(Dispatchers.IO) {
        val key = name.lowercase()
        cache.get(key)?.let { return@withContext it }

        val meta = libs[key] ?: return@withContext null
        val update = runCatching {
            when (meta) {
                is Maven  -> fetchFromMaven(meta)
                is GitHub -> fetchFromGitHub(meta)
            }
        }.onFailure { e ->
            println("[LIB_UPD] error for $key: ${e.message}")
        }.getOrNull()

        update?.let { cache.put(key, it) }
        return@withContext update
    }

    private suspend fun fetchFromMaven(m: Maven): LibraryUpdate? {
        val query = """g:"${m.group}" AND a:"${m.artifact}""""
        val url = Url("https://search.maven.org/solrsearch/select").toString() +
                "?rows=1&wt=json&q=" + URLEncoder.encode(query, Charsets.UTF_8)

        val resp: HttpResponse = client.get(url)
        if (!resp.status.isSuccess()) return null     // 4xx/5xx → null

        val body: MavenResponse = resp.body()

        val doc = body.response.docs.firstOrNull() ?: return null
        return LibraryUpdate(
            name        = m.artifact,
            version     = doc.latestVersion,
            publishedAt = Instant.ofEpochMilli(doc.timestamp),
            url         = "https://search.maven.org/artifact/${m.group}/${m.artifact}/${doc.latestVersion}/jar"
        )
    }

    private suspend fun fetchFromGitHub(g: GitHub): LibraryUpdate? {
        val resp: HttpResponse = client.get(
            "https://api.github.com/repos/${g.owner}/${g.repo}/releases/latest"
        )
        if (!resp.status.isSuccess()) return null

        val body: GitHubRelease = resp.body()

        val published = try {
            Instant.parse(body.publishedAt)
        } catch (e: DateTimeParseException) {
            Instant.EPOCH
        }

        return LibraryUpdate(
            name        = g.repo,
            version     = body.tagName,
            publishedAt = published,
            url         = body.htmlUrl
        )
    }
}
