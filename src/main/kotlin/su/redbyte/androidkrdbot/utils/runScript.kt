package su.redbyte.androidkrdbot.utils

import kotlinx.serialization.json.Json
import su.redbyte.androidkrdbot.domain.model.Comrade
import java.lang.ProcessBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path


suspend fun fetchComrades(apiId: String, apiHash: String): List<Comrade> = withContext(Dispatchers.IO) {
    val scriptPath: Path = Path.of("script", "members_exporter.py")   // ⚠️ script, не scipt
    val scriptFile = scriptPath.toFile()
    require(scriptFile.exists()) { "Python-скрипт не найден: $scriptPath" }
    val pythonPath = "venv/bin/python3"

    val process = ProcessBuilder(pythonPath, scriptFile.absolutePath, apiId, apiHash)
        .redirectErrorStream(true)
        .start()

    val exitCode = process.waitFor()
    val output = process.inputStream.bufferedReader().readText()
    val errorOutput = process.errorStream.bufferedReader().readText()

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

    Json.decodeFromString(output)
}
