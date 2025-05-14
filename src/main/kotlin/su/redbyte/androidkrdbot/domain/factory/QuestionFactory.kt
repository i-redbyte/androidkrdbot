package su.redbyte.androidkrdbot.domain.factory

import su.redbyte.androidkrdbot.domain.model.Question

object QuestionFactory {
    private val questions = listOf(
        Question(
            "Назови любой цвет радуги.",
            listOf(
                "красный", "оранжевый", "жёлтый", "желтый", "зелёный", "зеленый",
                "голубой", "синий", "фиолетовый",
                "red", "orange", "yellow", "green", "blue", "indigo", "violet"
            )
        ),
        Question(
            "Назовите столицу СССР.",
            listOf("москва", "moscow")
        ),
        Question(
            "Сколько букв в слове «проверка»?",
            listOf("8", "восемь", "eight")
        ),
        Question(
            "Главный цвет флага СССР?",
            listOf("красный", "red")
        ),
        Question(
            "Какой язык программирования используется в Android-разработке?",
            listOf("kotlin", "java", "котлин", "джава")
        ),
        Question(
            "Сколько бит в одном байте?",
            listOf("8", "восемь", "eight")
        ),
    )

    fun randomQuestion(): Question = questions.random()
}