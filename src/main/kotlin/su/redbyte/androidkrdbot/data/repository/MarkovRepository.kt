package su.redbyte.androidkrdbot.data.repository

import kotlinx.coroutines.*
import su.redbyte.androidkrdbot.data.model.markov.BiKey
import su.redbyte.androidkrdbot.data.model.markov.CumulativeNext
import su.redbyte.androidkrdbot.utils.detectBaseDir
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

private const val START = "<START>"
private const val END = "<END>"

class MarkovRepository private constructor(
    private val chain: Map<BiKey, WeightedOptions>,
    private val startingKeys: List<BiKey>
) {

    fun generate(seed: List<String>? = null, maxTokens: Int = 1200): String {
        require(maxTokens >= 2) { "maxTokens must be at least 2" }

        var current: BiKey = when {
            seed != null && seed.size >= 2 -> BiKey(seed[seed.size - 2], seed.last())
            seed != null && seed.size == 1 -> {
                val candidates = chain.keys.filter { it.first == seed.first() }
                if (candidates.isNotEmpty()) candidates.random() else startingKeys.random()
            }

            else -> startingKeys.random()
        }

        val result = mutableListOf<String>()
        if (current.first != START) result += current.first
        result += current.second

        repeat(maxTokens - result.size) {
            val options = chain[current] ?: return@repeat
            val nextTok = options.drawRandom()
            if (nextTok == END) return@repeat
            result += nextTok
            current = BiKey(current.second, nextTok)
        }

        return result.joinToString(" ")
            .replace("\\s+([,.!?;:])".toRegex(), "$1")
            .replace(" +".toRegex(), " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    companion object {

        suspend fun load(
            fileName: String = "clean_code_tokenized.txt",
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ): MarkovRepository = withContext(scope.coroutineContext) {
            val baseDir = detectBaseDir().toString().dropLast(2)
            val corpusPath = File(baseDir, "markov/$fileName").toPath()
            val tokens = tokenize(Files.readString(corpusPath))
            val (chain, starts) = train(tokens)
            MarkovRepository(chain, starts)
        }

        private fun tokenize(text: String): List<String> {
            val rawTokens = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val punctuation = setOf(".", "!", "?")
            return buildList {
                add(START)
                rawTokens.forEach { tok ->
                    add(tok)
                    if (tok in punctuation) {
                        add(END)
                        add(START)
                    }
                }
                if (last() != END) add(END)
            }
        }

        private fun train(tokens: List<String>):
                Pair<Map<BiKey, WeightedOptions>, List<BiKey>> {

            val counts = mutableMapOf<BiKey, MutableMap<String, Int>>()

            for (i in 0 until tokens.size - 2) {
                val key = BiKey(tokens[i], tokens[i + 1])
                val next = tokens[i + 2]
                counts.getOrPut(key) { mutableMapOf() }
                    .merge(next, 1, Int::plus)
            }

            val chain = buildMap {
                counts.forEach { (key, freqMap) ->
                    var cumulative = 0
                    val cumulativeList = freqMap
                        .map { (tok, cnt) ->
                            cumulative += cnt
                            CumulativeNext(tok, cumulative)
                        }
                        .sortedBy { it.cumulativeWeight }
                    put(key, WeightedOptions(cumulativeList, cumulative))
                }
            }

            val startingKeys = chain.keys.filter { it.first == START }

            return chain to startingKeys
        }
    }
}

private class WeightedOptions(
    private val cumulative: List<CumulativeNext>,
    private val totalWeight: Int
) {
    fun drawRandom(rnd: Random = Random.Default): String {
        val target = rnd.nextInt(totalWeight)
        var low = 0
        var high = cumulative.lastIndex
        while (low < high) {
            val mid = (low + high) ushr 1
            if (target < cumulative[mid].cumulativeWeight) high = mid
            else low = mid + 1
        }
        return cumulative[low].token
    }
}