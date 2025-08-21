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
    LOOT_THE_LOOTED(
        commandName = "lootTheLooted",
        description = "Получить информацию по запросу с целевых сайтов"
    ),
    LIB_UPDATES(
        commandName = "libUpdates",
        description = "Получить информацию о последней версии указанной библиотеки"
    ),
    DIGEST(
        commandName = "digest",
        description = "Краткая выжимка за сутки из популярных IT-каналов"
    ),
    GAME(
        commandName = "game",
        description = "Поиграем?!"
    ),
    COMMAND_LIST(
        commandName = "commandList",
        description = "Вывести список команд бота"
    ),
}