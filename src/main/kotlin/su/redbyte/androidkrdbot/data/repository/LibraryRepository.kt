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
    private data class GitHub(val owner: String, val repo: String) : Meta
    private data class GoogleMaven(val group: String, val artifact: String) : Meta

    private val libs = mapOf(
        "compose" to GoogleMaven("androidx.compose.ui", "ui"),
        "retrofit" to Maven("com.squareup.retrofit2", "retrofit"),
        "coil" to GitHub("coil-kt", "coil"),
        "kotlin" to GitHub("JetBrains", "kotlin")
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

        val update = runCatching {
            when (val meta = libs[key]) {
                is Maven -> fetchFromMaven(meta)?.copy(name = key)
                is GitHub -> fetchFromGitHub(meta)?.copy(name = key)
                is GoogleMaven -> fetchFromGoogleMaven(meta)?.copy(name = key)
                null -> fetchByArtifactOnly(key)
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
        if (!resp.status.isSuccess()) return null

        val body: MavenResponse = resp.body()

        val doc = body.response.docs.firstOrNull() ?: return null
        return LibraryUpdate(
            name = m.artifact,
            version = doc.latestVersion,
            publishedAt = Instant.ofEpochMilli(doc.timestamp),
            url = "https://search.maven.org/artifact/${m.group}/${m.artifact}/${doc.latestVersion}/jar"
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
            name = g.repo,
            version = body.tagName,
            publishedAt = published,
            url = body.htmlUrl
        )
    }

    private suspend fun fetchFromGoogleMaven(g: GoogleMaven): LibraryUpdate? {
        val path = g.group.replace('.', '/') + "/" + g.artifact
        val url = "https://dl.google.com/android/maven2/$path/maven-metadata.xml"

        val xmlText = client.get(url).bodyAsText()

        val doc = org.jsoup.Jsoup.parse(xmlText, "", org.jsoup.parser.Parser.xmlParser())
        val version = doc.selectFirst("versioning > release")?.text() ?: return null
        val lastUpdated = doc.selectFirst("versioning > lastUpdated")?.text() ?: "19700101000000"

        val published = runCatching {
            val fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneId.of("UTC"))
            Instant.from(fmt.parse(lastUpdated))
        }.getOrElse { Instant.EPOCH }

        return LibraryUpdate(
            name = g.artifact,
            version = version,
            publishedAt = published,
            url = "https://maven.google.com/web/index.html#${g.group}:${g.artifact}:$version"
        )
    }

    private suspend fun fetchByArtifactOnly(artifact: String): LibraryUpdate? {
        val query = """a:"$artifact""""
        val url = "https://search.maven.org/solrsearch/select" +
                "?rows=1&wt=json&q=" + URLEncoder.encode(query, Charsets.UTF_8)

        val resp: HttpResponse = client.get(url)
        if (!resp.status.isSuccess()) return null

        val doc = resp.body<MavenResponse>().response.docs.firstOrNull() ?: return null

        return LibraryUpdate(
            name = artifact,
            version = doc.latestVersion,
            publishedAt = Instant.ofEpochMilli(doc.timestamp),
            url = "https://search.maven.org/artifact/${doc.group}/${doc.artifact}/${doc.latestVersion}/jar"
        )
    }
}


