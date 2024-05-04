
# CorrectScript CLI

CorrectScript CLI is a command line tool designed to automatically correct Python scripts by leveraging a server-side LLM. To see the server side implementation visit [correct_script.server](https://github.com/iyubondyrev/correct_script.server), there you can learn how to run the server.

## How It Works

The Correct Script CLI operates by managing the lifecycle of a script correction process in an efficient loop mechanism. Here's a streamlined explanation of how the system works:

* Script Execution: The CLI attempts to execute the provided Python script locally. This can be done within a Docker container for isolated environments or directly on the host machine, depending on configuration preferences.
* Error Capture: If the script execution fails, the CLI captures the error output along with the script text.
* Request Formation: Using the captured error and script text, the CLI constructs a JSON-formatted request, structured as {"script": "<script>", "error": "<error>"}. This request is then sent to the server for processing.
* Server-side Processing: Upon receiving the request, the server utilizes LLMs to analyze the script and the associated error. It processes these inputs to suggest corrections.
* Receiving the Correction: The server responds with a JSON object containing the corrected script, formatted as {"fixedScript": "<fixedScript>"}.
* Loop Until Resolved: The CLI applies the suggested corrections and re-attempts to execute the script. This process repeats iteratively until the script executes without errors or until the maximum number of retries is reached.


## Features

- **Automatic Correction**: Send Python scripts to an LLM-powered server to get corrections.
- **Docker Integration**: Run Python scripts in a Docker container for isolated environments.
- **Retry Mechanism**: Automatically retries fixing the script a specified number of times if initial attempts fail.
- **Logging Support**: Optional logging of requests and responses for debugging purposes.

## Requirements

Java Runtime Environment (JRE): Requires Java to run the cli. It is tested with OpenJDK version "17.0.8.1".

## Installation

To install CorrectScript CLI, run the following command in your terminal:

```bash
curl -s https://raw.githubusercontent.com/iyubondyrev/correct_script.cli/main/install.sh | bash
```

This script will download the latest version of `correct_script.cli.jar` from the GitHub releases, place it into `/opt/correct_script.cli/`, and set up an alias `correct_script` for easy execution from the terminal. After the installation you will have to restart your terminal or do (you will get instructions from the installation script)
```bash
source .zshrc/.bashrc
```

## Usage

After installation, you can use the CLI tool as follows:

```bash
correct_script -i <input_file.py> -o <output_file.py>
```



### Command Line Arguments

- `-i, --input_file`: Specifies the path to the Python script that needs correction.
- `-o, --output_file`: Specifies the output file path where the corrected script will be saved.
- `-sh, --server_host` (default: `host of the default server`): Specifies the server host where the correction server is running.
- `-p, --server_port` (default: `port of the default server`): Specifies the port on which the server accepts connections.
- `-di, --docker_image`: Specifies a Docker image to use for running the script. NB: you need to have docker installed and running to use this feature.
- `-r, --max_retires` (default: `5`): Specifies the maximum number of retries for fixing the script.
- `-pi, --python_interpreter` (default: `python3`): Specifies the Python interpreter to use.
- `-t, --timeout` (default: `60`): Specifies the timeout in seconds for server responses. NB: LLMs can be quite slow so 40+ seconds is recommended.
- `-lr, --log_requests` (default: `false`): Enables logging of the request and response data.

## About the default server

The CLI is pre-configured to connect to a server operating at a specific host and port, set via default arguments. This allows users to utilize the CLI immediately without needing to set up their own server. The default server configuration employs two [Phi-3-mini-4k-instruct](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf) models. These models are not quantized, so the quality should be pretty decent.

## Usage examples

### 1

You can find the log example in correct_script_example_1.log

#### Input file

```python
a = [1, 2, 3, "lol"]
for i in a:
    e = int(i)

# Error:

Traceback (most recent call last):
File "/data/fixed.py", line 3, in <module>
e = int(i)
^^^^^^
ValueError: invalid literal for int() with base 10: 'lol'
)
```

#### First try

```python
for i in a:
    try:
        e = int(i)
    except ValueError:
        print(f'Cannot convert {i} to int')
)
```

### 2
You can find the log example in correct_script_example_2.log

```python
for i in range(1, 10)
    print(1 / (7 - i))

# Error:

File "/data/fixed.py", line 1
for i in range(1, 10)
    ^
SyntaxError: expected ':'
)
```

#### First try

```python
for i in range(1, 10):
    print(1 / (7 - i))
)

# Error
Traceback (most recent call last):
File "/data/fixed.py", line 2, in <module>
print(1 / (7 - i))
~~^~~~~~~~~
ZeroDivisionError: division by zero
)
```

#### Second try

```python
for i in range(1, 10):
    if i == 7:
        continue
    print(1 / (7 - i))
```
