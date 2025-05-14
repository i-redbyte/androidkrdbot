package su.redbyte.androidkrdbot.domain.model

enum class BotCommands(
    val commandName: String,
    val description: String
) {
    START_VERIFICATION(
        commandName = "startVerification",
        description = "Запустить проверку новых людей на спам"
    ),
    STOP_VERIFICATION(
        commandName = "stopVerification",
        description = "Остановить проверку новых людей на спам"
    ),
    VERIFICATION_STATUS(
        commandName = "verificationStatus",
        description = "Узнать статус проверки"
    ),
    RELOAD_QUESTIONS(
        commandName = "reloadQuestions",
        description = "Перезагрузить файл с вопросами"
    ),
}