package su.redbyte.androidkrdbot.data.repository

import su.redbyte.androidkrdbot.domain.factory.QuestionFactory
import su.redbyte.androidkrdbot.domain.model.Question

class QuestionRepository {
    fun getRandomQuestion(): Question = QuestionFactory.randomQuestion()
}