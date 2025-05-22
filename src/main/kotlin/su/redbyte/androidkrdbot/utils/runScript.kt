package su.redbyte.androidkrdbot.utils

import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import su.redbyte.androidkrdbot.domain.model.Member
import java.lang.ProcessBuilder


fun fetchMembers(apiId: String, apiHash: String): List<Member> {
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

    return Json.decodeFromString(output)
}