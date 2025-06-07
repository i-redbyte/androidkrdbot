package su.redbyte.androidkrdbot.cli.command

enum class Commands(
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
    INTERROGATION(
        commandName = "interrogation",
        description = "Проверить на принадлежность к ботам через внешние источники"
    ),
    POLITBURO(
        commandName = "politburo",
        description = "Вывести список всех администраторов чата"
    ),
}