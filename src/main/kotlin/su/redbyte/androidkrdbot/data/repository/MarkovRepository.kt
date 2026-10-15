package su.redbyte.androidkrdbot.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.random.Random

private const val END = "<END>"
private const val MARKOV_ORDER = 5
private val TOKEN = Regex("[\\p{L}\\p{N}]+|[^\\s]")
private val WORD = Regex("[\\p{L}\\p{N}]+")
private val STOP_WORDS = setOf(
    "а", "без", "бы", "в", "во", "для", "до", "его", "ее", "её", "же", "за", "и", "из", "или",
    "к", "как", "на", "не", "но", "о", "об", "он", "она", "они", "от", "по", "при", "с", "со",
    "так", "то", "у", "что", "это", "такое", "такая", "такой", "такие", "какое", "какая",
    "какой", "какие", "зачем", "почему", "дела", "дело", "деле", "делу"
)

private data class Sentence(val tokens: List<String>, val keywords: Set<String>) {
    val wordCount: Int = tokens.count { WORD.matches(it) }
}

/** Sentence retrieval plus an optional sentence-bounded Markov model. */
class MarkovRepository private constructor(
    private val chain: Map<List<String>, WeightedOptions>,
    private val sentences: List<Sentence>,
    private val documentFrequency: Map<String, Int>
) {
    /** Select an intact passage for a mention. */
    fun generate(prompt: String = "", maxWords: Int = 45, random: Random = Random.Default): String =
        render(selectSource(prompt, maxWords, random).tokens)

    /** Optional playful mode: a Markov walk that can mix passages. */
    fun generateMarkov(prompt: String = "", maxWords: Int = 45, random: Random = Random.Default): String {
        val pool = selectPool(prompt, maxWords)
        repeat(8) {
            val source = pool.random(random)
            val generated = walk(source.tokens.take(MARKOV_ORDER), maxWords, random)
            if (generated != null) return render(generated)
        }
        return render(pool.random(random).tokens)
    }

    private fun selectSource(prompt: String, maxWords: Int, random: Random): Sentence =
        selectPool(prompt, maxWords).random(random)

    /** Best match in this book; zero means none of the prompt's keywords occur in a usable sentence. */
    fun relevance(prompt: String, maxWords: Int = 45): Double =
        scoreCandidates(prompt, maxWords).maxOf { it.second }

    private fun selectPool(prompt: String, maxWords: Int): List<Sentence> {
        val scored = scoreCandidates(prompt, maxWords)
        val candidates = scored.map { it.first }
        val fallback = candidates.filter {
            it.wordCount <= 22 && it.tokens.first().lowercase() !in
                setOf("это", "этот", "эта", "но", "однако", "вот", "таким")
        }.ifEmpty { candidates }
        val best = scored.maxOf { it.second }
        return if (best > 0.0) {
            scored.filter { it.second >= best * 0.9 }.sortedByDescending { it.second }.take(5).map { it.first }
        } else {
            fallback
        }
    }

    private fun scoreCandidates(prompt: String, maxWords: Int): List<Pair<Sentence, Double>> {
        require(maxWords >= 8) { "maxWords must be at least 8" }
        val candidates = sentences.filter { it.wordCount in 8..maxWords && render(it.tokens).length <= 900 }
        require(candidates.isNotEmpty()) { "No sentences fit the requested length" }

        val query = keywords(tokenize(prompt))
        if (query.isEmpty()) return candidates.map { it to 0.0 }
        val lowerPrompt = prompt.lowercase()
        val definitionQuestion = lowerPrompt.contains("что такое")
        val purposeQuestion = lowerPrompt.contains("зачем") || lowerPrompt.contains("почему")
        val exactQueryWords = tokenize(prompt).map { it.lowercase().replace('ё', 'е') }.toSet()
        return candidates.map { sentence ->
            val overlap = query.intersect(sentence.keywords)
            var score = overlap.sumOf { term ->
                ln(1.0 + sentences.size.toDouble() / (documentFrequency[term] ?: 1))
            }
            val firstKeyword = sentence.tokens.firstOrNull()?.lowercase()?.replace('ё', 'е')?.take(5)
            if (firstKeyword in overlap) {
                score += 1.0
                if (definitionQuestion &&
                    sentence.tokens.first().lowercase().replace('ё', 'е') in exactQueryWords &&
                    sentence.tokens.drop(1).take(3).any {
                        it.lowercase() in setOf("есть", "—", "является", "представляет")
                    }
                ) score += 5.0
                if (purposeQuestion && sentence.tokens.any {
                    it.lowercase() in setOf("для", "чтобы", "необходимо", "нужно", "должна", "должен")
                }) score += 2.0
            }
            if (sentence.tokens.first().lowercase() in setOf("это", "этот", "эта", "но", "однако", "вот")) {
                score *= 0.5
            }
            score /= 1.0 + (sentence.wordCount - 24).coerceAtLeast(0) * 0.03
            sentence to score
        }
    }

    private fun walk(start: List<String>, maxWords: Int, random: Random): List<String>? {
        val result = start.toMutableList()
        repeat(maxWords * 4) {
            val next = chain[result.takeLast(MARKOV_ORDER)]
                ?.draw(random) ?: return null
            if (next == END) {
                return if (result.last() in setOf(".", "!", "?")) result else null
            }
            result += next
            if (result.count { WORD.matches(it) } > maxWords || render(result).length > 900) return null
        }
        return null
    }

    companion object {
        suspend fun load(fileName: String = "lenin_sentences.txt"): MarkovRepository = withContext(Dispatchers.IO) {
            val stream = MarkovRepository::class.java.classLoader
                .getResourceAsStream("markov/$fileName")
                ?: error("Missing book corpus: markov/$fileName")
            fromSentences(stream.bufferedReader(Charsets.UTF_8).use { it.readLines() })
        }

        fun fromSentences(lines: List<String>): MarkovRepository {
            val sentences = lines.map(::tokenize)
                .filter { it.size > MARKOV_ORDER && it.last() in setOf(".", "!", "?") }
                .map { Sentence(it, keywords(it)) }
            require(sentences.isNotEmpty()) { "The corpus has no complete sentences" }

            val frequencies = mutableMapOf<String, Int>()
            val transitions = mutableMapOf<List<String>, MutableMap<String, Int>>()
            sentences.forEach { sentence ->
                sentence.keywords.forEach { frequencies.merge(it, 1, Int::plus) }
                val tokens = sentence.tokens
                for (index in 0..tokens.size - MARKOV_ORDER) {
                    val key = tokens.subList(index, index + MARKOV_ORDER).toList()
                    val next = tokens.getOrElse(index + MARKOV_ORDER) { END }
                    transitions.getOrPut(key) { mutableMapOf() }.merge(next, 1, Int::plus)
                }
            }
            return MarkovRepository(transitions.mapValues { WeightedOptions(it.value) }, sentences, frequencies)
        }

        private fun tokenize(text: String): List<String> = TOKEN.findAll(text).map { it.value }.toList()

        private fun keywords(tokens: List<String>): Set<String> = tokens.asSequence()
            .filter { WORD.matches(it) && (it.length >= 4 || it.lowercase().startsWith("код")) }
            .map {
                val word = it.lowercase().replace('ё', 'е')
                if (word.startsWith("код")) "код" else word.take(5)
            }
            .filterNot { it in STOP_WORDS }
            .toSet()

        private fun render(tokens: List<String>): String = tokens.joinToString(" ")
            .replace(Regex("\\s+([,.!?;:%»\\]\\)])"), "$1")
            .replace(Regex("([«\\[(])\\s+"), "$1")
            .replace(Regex("(?<=\\p{L})\\s+-\\s+(?=\\p{L})"), "-")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

private class WeightedOptions(frequencies: Map<String, Int>) {
    private val cumulative: List<Pair<String, Int>>
    private val total: Int

    init {
        var weight = 0
        cumulative = frequencies.map { (token, count) ->
            weight += count
            token to weight
        }
        total = weight
    }

    fun draw(random: Random): String {
        val value = random.nextInt(total)
        var low = 0
        var high = cumulative.lastIndex
        while (low < high) {
            val middle = (low + high) ushr 1
            if (value < cumulative[middle].second) high = middle else low = middle + 1
        }
        return cumulative[low].first
    }
}
