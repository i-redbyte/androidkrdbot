package su.redbyte.androidkrdbot.domain.factory

import su.redbyte.androidkrdbot.domain.model.Question

object QuestionFactory {
    private val questions = listOf(
        Question("Сколько будет шесть плюс шесть?", listOf("12", "двенадцать")),
        Question("Назови любой цвет радуги", listOf("красный", "оранжевый", "жёлтый", "зелёный", "голубой", "синий", "фиолетовый"))
    )

    fun randomQuestion(): Question = questions.random()
}