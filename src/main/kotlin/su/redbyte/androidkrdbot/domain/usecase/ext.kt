package su.redbyte.androidkrdbot.domain.usecase

import com.github.kotlintelegrambot.entities.User

fun User.candidateName(): String = username?.let { "@$it" } ?: firstName