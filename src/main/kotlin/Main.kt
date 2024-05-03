package correct_script.cli

import kotlinx.cli.*
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) {
    val parser = ArgParser("correct_script")
    val inputFile by parser.option(ArgType.String, shortName = "i", fullName = "input_file", description = "Path to the Python script").required()
    val outputFile by parser.option(ArgType.String, shortName = "o", fullName = "output_file", description = "Path to save the corrected script").required()
    val serverHost by parser.option(ArgType.String, shortName = "sh", fullName = "server_host", description = "Server host").default("45.87.80.234")
    val serverPort by parser.option(ArgType.Int, shortName = "p", fullName = "server_port", description = "Server port").default(8080)
    val dockerImage by parser.option(ArgType.String, shortName = "di", fullName = "docker_image", description = "Docker image to run script")
    val maxRetries by parser.option(ArgType.Int, shortName = "r", fullName = "max_retires", description = "Maximum retry attempts").default(5)
    val pythonInterpreter by parser.option(ArgType.String, shortName = "pi", fullName = "python_interpreter",
        description = "What to use when running python script, for example python, python3. Will be used like this: \"python3\" input_file.py")
        .default("python")
    val timeout by parser.option(ArgType.Int, shortName = "t", fullName = "timeout",
        description = "How long to wait the server to respond in seconds")
        .default(60)
    val logRequests by parser.option(ArgType.Boolean, shortName = "lr", fullName = "log_requests", description = "If true request history will be stored in the file correct_script.log").default(true)

    parser.parse(args)

    runBlocking {
        ScriptCorrector(
            inputFileName = inputFile,
            outputFileName = outputFile,
            serverHost = serverHost,
            serverPort = serverPort,
            maxRetries = maxRetries,
            pythonInterpreter = pythonInterpreter,
            timeout = timeout,
            dockerImage = dockerImage,
            logRequests = logRequests,
            ).use { resource ->
            resource.fix()
        }
    }

}