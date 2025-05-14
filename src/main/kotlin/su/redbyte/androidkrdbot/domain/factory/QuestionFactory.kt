package su.redbyte.androidkrdbot.domain.factory

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import su.redbyte.androidkrdbot.domain.model.Question

object QuestionFactory {
    private var questions: List<Question> = emptyList()

    init {
        loadQuestions()
    }

    fun randomQuestion(): Question = questions.random()

    fun reload(): Boolean {
        return try {
            loadQuestions()
            true
        } catch (e: Exception) {
            println("❌ Ошибка при загрузке вопросов: ${e.message}")
            false
        }
    }

    private fun loadQuestions() {
        val inputStream = javaClass.classLoader.getResourceAsStream("questions.json")
            ?: error("❌ Файл questions.json не найден в resources!")

        val mapper = jacksonObjectMapper()
        questions = mapper.readValue(inputStream)
        println("✅ Загружено ${questions.size} вопросов из questions.json")
    }
}
