package su.redbyte.androidkrdbot.domain.usecase

import su.redbyte.androidkrdbot.data.repository.QuestionRepository
import su.redbyte.androidkrdbot.domain.model.Question

class GetRandomQuestionUseCase(private val questionRepo: QuestionRepository) {
    operator fun invoke(): Question = questionRepo.getRandomQuestion()
}