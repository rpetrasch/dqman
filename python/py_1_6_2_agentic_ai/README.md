# Agentic AI Company Validator

This is a simple Agentic AI example using LangChain, LangGraph, and Ollama.
It validates company information against Wikipedia and extracts master data with age calculation.

## Prerequisites

1.  **Python 3.10+**
2.  **Ollama** running locally.
    *   This example is configured to use `gpt-oss` by default (see `config.py`).
    *   Ensure your model supports **Tool Calling** (e.g. `llama3.1`, `mistral`, `qwen2.5`).
    *   To pull a model: `ollama pull llama3.1`.
    *   **Configuration**: Update `config.py` if your model name is different.

## Setup

1.  Create a virtual environment:
    ```bash
    python3 -m venv .venv
    source .venv/bin/activate
    ```

2.  Install dependencies:
    ```bash
    pip install -r requirements.txt
    ```

## Usage

Run the main script:

```bash
python main.py
```

Or pass arguments directly:

```bash
python main.py "Google" "Mountain View"
```

## How it works

1.  **Lookup Agent**: Searches Wikipedia for the company.
2.  **Validation Agent**: Checks if the found Wikipedia page matches the user's input.
3.  **Extraction Agent**: If valid, extracts data (Name, Employees, Revenue) and uses a **Tool** to convert converted revenue to EUR.
