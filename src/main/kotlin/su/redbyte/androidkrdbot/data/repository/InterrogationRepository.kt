package su.redbyte.androidkrdbot.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LolsBotResponse(
    @SerialName("banned") val banned: Boolean
)

class InterrogationRepository {

    suspend fun checkInLolsBot(userId: Long): Boolean {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
            }
        }

        return try {
            val response: HttpResponse = client.get("https://api.lols.bot/account") {
                parameter("id", userId)
            }

            if (response.status.value != 200) {
                println("lols bot returned ${response.status.value} code")
                false
            } else {
                println("lols response: ${response.bodyAsText()}")
                val data: LolsBotResponse = response.body()
                data.banned
            }
        } catch (e: Exception) {
            println("${e::class.simpleName}: cannot connect to lols bot")
            false
        } finally {
            client.close()
        }
    }
}
