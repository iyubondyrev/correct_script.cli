package correct_script.cli

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import java.io.File
import java.io.FileNotFoundException

class ScriptCorrectorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `test successful script correction`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"fixedScript":"fixed content"}""")
            .addHeader("Content-Type", "application/json"))

        val inputFile = this::class.java.classLoader.getResource("input.py")?.file ?: throw FileNotFoundException("input.py not found in resources")
        val scriptCorrector = ScriptCorrector(
            inputFileName = inputFile,
            outputFileName = "output.py",
            serverHost = server.hostName,
            serverPort = server.port,
            maxRetries = 1,
            pythonInterpreter = "python3",
            timeout = 60,
            dockerImage = null,
            logRequests = false
        )

        scriptCorrector.use {
            it.fix()
        }

        val outputFile = File("output.py")
        assertTrue(outputFile.readText().contains("fixed content"))

        val request = server.takeRequest()
        assertEquals("/fix-script", request.path)
    }

    @Test
    fun `test docker`() = runBlocking {
        server.enqueue(MockResponse().setBody("{\"fixedScript\":\"a = [1, 2]\na[0\"}")
            .addHeader("Content-Type", "application/json"))

        server.enqueue(MockResponse().setBody("{\"fixedScript\":\"a = [1, 2]\na[0]\"}")
            .addHeader("Content-Type", "application/json"))

        val inputFile = this::class.java.classLoader.getResource("input.py")?.file ?: throw FileNotFoundException("input.py not found in resources")
        val scriptCorrector = ScriptCorrector(
            inputFileName = inputFile,
            outputFileName = "output.py",
            serverHost = server.hostName,
            serverPort = server.port,
            maxRetries = 2,
            pythonInterpreter = "python3",
            timeout = 60,
            dockerImage = "python:latest",
            logRequests = false
        )

        scriptCorrector.use {
            it.fix()
        }


        val outputFile = File("output.py")
        assertTrue(outputFile.readText().contains("a = [1, 2]\na[0]"))

        var request = server.takeRequest()
        assertEquals("/fix-script", request.path)
        request = server.takeRequest()
        assertEquals("/fix-script", request.path)
    }
}
