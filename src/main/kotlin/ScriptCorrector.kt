package correct_script.cli

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import java.io.File

data class FixScriptRequest(val script: String, val error: String)
data class FixScriptResponse(val fixedScript: String)

class ScriptCorrector(
    inputFileName: String,
    outputFileName: String,
    serverHost: String,
    serverPort: Int,
    private val maxRetries: Int,
    private val pythonInterpreter: String,
    private val timeout: Int,
    private val dockerImage: String?,
    logRequests: Boolean
): Closeable {
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        engine {
            requestTimeout = timeout.toLong() * 1000
        }
    }
    private val url = "http://$serverHost:$serverPort/fix-script"
    private val inputFile: File = File(inputFileName)
    private val outputFile: File = File(outputFileName)
    private val useDocker: Boolean = dockerImage != null
    private var dockerContainerId: String? = null
    private val requestResponseLogger = LoggerFactory.getLogger("requestResponseLogger") as Logger
    private val communicationLogger = LoggerFactory.getLogger("communicationLogger") as Logger

    init {
        if (!logRequests) {
            requestResponseLogger.level = Level.OFF
        }
    }

    private fun startDockerContainer() {
        val outputPath = outputFile.absolutePath
        val command = "docker run -d -v $outputPath:/data/${outputFile.name} --entrypoint tail $dockerImage -f /dev/null"
        communicationLogger.info("Starting docker container for image $dockerImage")
        Runtime.getRuntime().exec(command).let { process ->
            process.waitFor()
            dockerContainerId = process.inputStream.bufferedReader().readText().trim()
            communicationLogger.info("Docker container started")
        }
    }

    private fun stopDockerContainer() {
        if (dockerContainerId != null) {
            Runtime.getRuntime().exec("docker stop $dockerContainerId")
            communicationLogger.info("Docker container stopped")
        }
    }


    private fun getCommand(): String {
        return if (useDocker && dockerContainerId != null) {
            "docker exec $dockerContainerId $pythonInterpreter /data/${outputFile.name}"
        } else {
            "$pythonInterpreter ${outputFile.absolutePath}"
        }
    }

    override fun close() {
        client.close()
        if (useDocker) {
            try {
                stopDockerContainer()
            } catch (e: Exception) {
                communicationLogger.info("Can't stop docker container with id $dockerContainerId: ${e.message}")
            }

        }
    }

    suspend fun fix() {
        try {
            outputFile.writeText(inputFile.readText())
            communicationLogger.info("Output file created.")
        } catch (e: Exception) {
            communicationLogger.info("Can not create output file: ${e.message}")
            return
        }

        if (useDocker) {
            try {
                startDockerContainer()
            } catch (e: Exception) {
                communicationLogger.info("Can't start docker container: ${e.message}")
                return
            }
        }

        var hasErrors = false
        var retries = 0

        do {
            val command = getCommand()
            var errors: String
            try {
                val process = Runtime.getRuntime().exec(command)
                errors = process.errorStream.bufferedReader().readText()
            } catch (e: Exception) {
                communicationLogger.info("Can not run your script: ${e.message}")
                return
            }

            if (errors.isNotEmpty()) {
                hasErrors = true

                retries++
                communicationLogger.info("Found errors, trying to fix, $retries retry of $maxRetries retries.")


                val scriptText = outputFile.readText()
                val request = FixScriptRequest(script = scriptText, error = errors)
                requestResponseLogger.debug(request.toString())
                var response: HttpResponse

                try {
                    response = client.post(url) {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                } catch (e: Exception) {
                    communicationLogger.info("Can't get response from the server: ${e.message}")
                    return
                }

                requestResponseLogger.debug(response.toString())

                val responseData = response.body<FixScriptResponse>()
                requestResponseLogger.debug(responseData.toString())

                outputFile.writeText(responseData.fixedScript)
                communicationLogger.info("Updated script saved.")
            } else {
                communicationLogger.info("No errors found in script.")
                hasErrors = false
            }
        } while (hasErrors && retries <= maxRetries)

        if (hasErrors) {
            communicationLogger.info("Sorry, I didn't manage to fix you script :(")
        }
    }
}