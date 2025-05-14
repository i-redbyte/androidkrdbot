package su.redbyte.androidkrdbot.domain.model

data class Question(val text: String, val correctAnswers: List<String>) {
    fun isCorrect(answer: String): Boolean =
        correctAnswers.any { it.equals(answer.trim(), ignoreCase = true) }
}
