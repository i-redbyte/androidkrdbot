package su.redbyte.androidkrdbot.utils

import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.ProcessBuilder

@Serializable
data class Member(val id: Long, val name: String, val userName: String)

fun fetchMembers(apiId: String, apiHash: String): List<Member> {
    // 1️⃣ Абсолютный путь к твоему скрипту
    val scriptPath = "/Users/red_byte/IdeaProjects/androidkrdbot/scipt/members_exporter.py" // <-- замени на свой
    val scriptFile = File(scriptPath).also {
        if (!it.exists()) error("Script file not found at path: $scriptPath")
    }

    // 2️⃣ Путь к Python (может быть python или python3)
    val pythonPath = "venv/bin/python3" // или "/absolute/path/to/venv/bin/python3"

    // 3️⃣ Запускаем процесс
    val process = ProcessBuilder(pythonPath, scriptFile.absolutePath, apiId, apiHash)
        .redirectErrorStream(true) // объединяем stderr в stdout
        .start()

    // 4️⃣ Ждём завершения и читаем вывод
    val exitCode = process.waitFor()
    val output = process.inputStream.bufferedReader().readText()
    val errorOutput = process.errorStream.bufferedReader().readText()

    // 5️⃣ Проверяем на ошибки
    if (exitCode != 0 || !output.trim().startsWith("[")) {
        val fullOutput = """
            Exit code: $exitCode
            Stdout:
            $output
            Stderr:
            $errorOutput
        """.trimIndent()
        error("Failed to fetch members. Output:\n$fullOutput")
    }

    // 6️⃣ Парсим JSON в List<Member>
    return Json.decodeFromString(output)
}

//fun fetchMembers(apiId: String, apiHash: String): List<Member> {
//    // Загружаем скрипт из ресурсов
//    val scriptStream = {}.javaClass.getResourceAsStream("/members_exporter.py")
//        ?: error("members_exporter.py not found in resources")
//    // Копируем во временный файл
//    val scriptFile = File.createTempFile("members_exporter", ".py")
//    scriptFile.writeBytes(scriptStream.readBytes())
//    scriptFile.deleteOnExit()
//    val pythonPath = "venv/bin/python3"
//    val process = ProcessBuilder(pythonPath, scriptFile.absolutePath, apiId, apiHash)
//        .redirectErrorStream(true)
//        .start()
//
//    val output = process.inputStream.bufferedReader().readText()
//    process.waitFor()
//
//    if (!output.trim().startsWith("[")) {
//        error("Python did not return valid JSON. Output:\n$output")
//    }
//
//    return Json.decodeFromString(output)
//}
//
