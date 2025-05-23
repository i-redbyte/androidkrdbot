package su.redbyte.androidkrdbot.utils

import java.io.File
import kotlinx.serialization.json.Json
import su.redbyte.androidkrdbot.domain.model.Comrade
import java.lang.ProcessBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun fetchComrades(apiId: String, apiHash: String): List<Comrade> = withContext(Dispatchers.IO) {
    val scriptPath = "/Users/red_byte/IdeaProjects/androidkrdbot/scipt/members_exporter.py"
    val scriptFile = File(scriptPath).also {
        if (!it.exists()) error("Script file not found at path: $scriptPath")
    }
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
