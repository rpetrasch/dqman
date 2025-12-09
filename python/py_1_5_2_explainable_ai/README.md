# **Chapter 1.5.2: Explainable AI with Llama 3**

**Book:** Data Quality for Software Engineers (DQ4SWE)

This project demonstrates the concepts of **Explainable AI (XAI)** versus **Black Box AI** using a Large Language Model (LLM). It corresponds to Chapter 1.5.2 of *Data Quality for Software Engineers*, illustrating how prompt engineering influences the transparency and reasoning of AI decision-making.

The script uses [Ollama](https://ollama.com/) to run the Llama 3 model locally for sentiment analysis.

## **Project Structure**

* **main.py**: The core script executing three different prompting strategies:  
  1. **Black Box:** Requests a classification without reasoning.  
  2. **Glass Box (Explainable):** Requests step-by-step reasoning before the classification.  
  3. **Post-Hoc Rationalization:** Requests the classification first, followed by a justification (often leading to hallucinated logic).

## **Prerequisites**

Before running the script, ensure you have the following installed:

1. **Python 3.x**  
2. **Ollama**: Download and install from [ollama.com](https://ollama.com).  
3. **Llama 3 Model**: You must pull the specific model used in the script.

## **Installation**

1. **Set up your Virtual Environment** (if not already  active):  
   \# Create the venv  
   ```python -m venv .venv```

   \# Activate the venv (Windows)  
   `.venv\\Scripts\\activate`

   \# Activate the venv (Mac/Linux)  
   `source .venv/bin/activate`

2. Install Dependencies:  
   Assuming you have the global requirements file in the parent directory:  
   ```pip install -r ../requirements.txt```
   *Alternatively, install the specific library required for this module:*  
   ```pip install ollama```

3. Download the LLM:  
   This step is critical. The Python script does not auto-download the model.  
   ```ollama pull llama3```

## **Usage**

Ensure the Ollama service is running in the background, then execute the script:

`python main.py`

## **What Can Go Wrong?**

**Q: I am getting a ResponseError: model 'llama3' not found (status code: 404\) even though I installed the Python library.**

**A:** This is the most common error. Installing the Python ollama library is **not** the same as downloading the actual neural network model.

The Python library is just a connector. You must explicitly download the model weights to your local machine using the command line tool.

* **The Fix:** Run ollama pull llama3 in your terminal.  
* **Verification:** Run ollama list to confirm llama3 appears in your list of available models.
