package su.redbyte.androidkrdbot.domain.factory

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import su.redbyte.androidkrdbot.domain.model.Question
import java.nio.file.Files
import java.nio.file.Path

object QuestionFactory {
    @Volatile private var questions: List<Question> = emptyList()

    private val FILE_PATH: Path = Path.of("data", "questions.json")

    init { loadQuestions() }

    fun randomQuestion(): Question = questions.random()

    fun reload(): Boolean = runCatching { loadQuestions() }
        .onFailure { println("❌ Ошибка: ${it.message}") }
        .isSuccess

    private fun loadQuestions() {
        val bytes = Files.readAllBytes(FILE_PATH)
        questions = jacksonObjectMapper().readValue(bytes)
        println("✅ Загружено ${questions.size} вопросов из $FILE_PATH")
    }
}
