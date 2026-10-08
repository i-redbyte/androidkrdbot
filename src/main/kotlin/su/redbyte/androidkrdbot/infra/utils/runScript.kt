package su.redbyte.androidkrdbot.infra.utils

import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.Json
import su.redbyte.androidkrdbot.domain.model.Comrade
import java.lang.ProcessBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.redbyte.androidkrdbot.domain.usecase.FetchComradesUseCase
import java.io.File
import java.util.concurrent.TimeUnit

private const val DIGEST = "digest.py"
private const val SCRIPT_TIMEOUT_MINUTES = 15L
private const val MEMBERS_EXPORTER = "members_exporter.py"
private const val SESSION_FILE = "bot_auth"
suspend fun fetchComrades(apiId: String, apiHash: String): List<Comrade> = withContext(Dispatchers.IO) {
    val output = processScript(apiId, apiHash, MEMBERS_EXPORTER)

    val jsonStartIndex = output.indexOf("[")
    if (jsonStartIndex == -1) {
        error(
            "JSON output not found in script output. " +
                "Частая причина: нет авторизованной Pyrogram-сессии bot_auth — один раз запустите script/members_exporter.py из корня проекта."
        )
    }

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

    val pythonExecutable = resolvePythonExecutable(baseDir)

    val process = ProcessBuilder(
        pythonExecutable,
        scriptFile.absolutePath,
        apiId,
        apiHash,
        SESSION_FILE
    )
        .directory(baseDir)
        .redirectErrorStream(true)
        .start()

    val outputBuffer = StringBuilder()
    val reader = Thread {
        process.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                outputBuffer.appendLine(line)
            }
        }
    }.apply { isDaemon = true; start() }

    val finished = process.waitFor(SCRIPT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
    reader.join(5_000)

    if (!finished) {
        process.destroyForcibly()
        reader.join(5_000)
        error(
            "Python-скрипт $scriptName не завершился за $SCRIPT_TIMEOUT_MINUTES мин. " +
                "Частая причина: зависание Pyrogram/kurigram или переполнение буфера вывода."
        )
    }

    val output = outputBuffer.toString()
    val exitCode = process.exitValue()
    if (exitCode != 0) {
        val fullOutput = """
            Exit code: $exitCode
            Output:
            $output
        """.trimIndent()
        error("Скрипт $scriptName завершился с ошибкой. Output:\n$fullOutput")
    }
    return output
}

/**
 * Python for Pyrogram scripts: PYTHON_PATH / PYTHON (env or .env), then ./venv/bin/python3, then python3 on PATH.
 */
internal fun resolvePythonExecutable(baseDir: File): String {
    listOf("PYTHON_PATH", "PYTHON").forEach { key ->
        readConfigValue(key)?.let { candidate ->
            return normalizePythonCandidate(candidate, baseDir)
        }
    }

    val venvPython = File(baseDir, "venv/bin/python3")
    if (venvPython.isFile) {
        return venvPython.absolutePath
    }

    return "python3"
}

private fun readConfigValue(key: String): String? {
    val fromEnv = System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
    if (fromEnv != null) return fromEnv
    return runCatching { dotenv()[key]?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()
}

private fun normalizePythonCandidate(candidate: String, baseDir: File): String {
    val file = File(candidate)
    val resolved = when {
        file.isAbsolute -> file
        else -> File(baseDir, candidate)
    }
    if (resolved.isFile) {
        return resolved.absolutePath
    }
    if (!file.isAbsolute) {
        return candidate
    }
    error(
        """
        Python интерпретатор не найден: ${resolved.absolutePath}
        Укажите PYTHON_PATH в .env, создайте venv в корне проекта:
          python3 -m venv venv && ./venv/bin/pip install -r requirements.txt
        или установите python3 в PATH.
        """.trimIndent()
    )
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
