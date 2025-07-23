package su.redbyte.androidkrdbot.utils

import kotlinx.serialization.json.Json
import su.redbyte.androidkrdbot.domain.model.Comrade
import java.lang.ProcessBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase
import java.io.File

private const val DIGEST = "digest.py"
private const val MEMBERS_EXPORTER = "members_exporter.py"
private const val SESSION_FILE = "bot_auth"
suspend fun fetchComrades(apiId: String, apiHash: String): List<Comrade> = withContext(Dispatchers.IO) {
    val output = processScript(apiId, apiHash, MEMBERS_EXPORTER)

    val jsonStartIndex = output.indexOf("[")
    if (jsonStartIndex == -1) error("JSON output not found in script output")

    val jsonText = output.substring(jsonStartIndex).trim()

    Json.decodeFromString(jsonText)
}

suspend fun fetchDigest(apiId: String, apiHash: String): String = withContext(Dispatchers.IO) {
    return@withContext processScript(apiId, apiHash, DIGEST)
}

private fun processScript(apiId: String, apiHash: String, scriptName: String): String {
    val baseDir = detectBaseDir()

    val scriptFile = File(baseDir, "script/$scriptName")
    require(scriptFile.exists()) { "Python-скрипт не найден: ${scriptFile.absolutePath}" }

    val pythonFile = File(baseDir, "venv/bin/python3")
    require(pythonFile.exists()) { "Python интерпретатор не найден: ${pythonFile.absolutePath}" }

    val process = ProcessBuilder(
        pythonFile.absolutePath,
        scriptFile.absolutePath,
        apiId,
        apiHash,
        SESSION_FILE
    )
        .directory(baseDir)
        .redirectErrorStream(true)
        .start()
    val exitCode = process.waitFor()
    val output = process.inputStream.bufferedReader().readText()
    val errorOutput = process.errorStream.bufferedReader().readText()
    if (errorOutput.isNotEmpty() || exitCode != 0) {
        println("Error output:\n$errorOutput")
        println("Exit code: $exitCode")
        if (exitCode != 0) {
            val fullOutput = """
            Exit code: $exitCode
            Stdout:
            $output
            Stderr:
            $errorOutput
        """.trimIndent()
            error("Failed to fetch members. Output:\n$fullOutput")
        }
    }
    return output
}

fun detectBaseDir(): File {
    return try {
        val jarPath = File(
            FetchComradesUseCase::class.java.protectionDomain.codeSource.location.toURI()
        ).absoluteFile

        val jarDir = if (jarPath.isFile) jarPath.parentFile
        else jarPath

        if (jarDir.path.contains("/build/")) {
            File(".").absoluteFile
        } else {
            jarDir
        }
    } catch (e: Exception) {
        File(".").absoluteFile
    }
}
