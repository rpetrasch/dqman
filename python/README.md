# About the Python Examples

  **Towards Data Quality Excellence:** AI-ready data, reliable business intelligence, optimized operations & trustworthy decisions.

<!-- markdownlint-disable-next-line MD026 -->
## TLDR;

### **Prerequisites**

To run all examples in this repository, you will need:

1. **Python 3.10+**  
2. **Docker & Docker Compose** (Required for Chapter 4.5 Microservices)  
3. **Ollama** (Required for Chapter 1.5 local LLM examples)  
4. **Git**

### **1\. Installation**

Clone the repository and set up a virtual environment:

git clone \[<https://github.com/yourusername/dq-software-engineers.git\>](<https://github.com/yourusername/dq-software-engineers.git>)  
cd dq-software-engineers

\# Create virtual environment  
python3 \-m venv venv

\# Activate environment  
\# On Linux/macOS:  
source venv/bin/activate  
\# On Windows:  
\# venv\\Scripts\\activate.bat

\# Install dependencies  
pip install \-r fqman/requirements.txt

## **📖 About the Project**

This repository contains the source code, examples, and practical exercises accompanying the book **"Data Quality for Software Engineers"** (Part I: Fundamentals) by **Roland Petrasch and Richard Petrasch**.

Data Quality (DQ) is often treated as an afterthought in software engineering. This book and repository aim to change that by providing a "full-stack" perspective: from the fundamentals of DQ dimensions to advanced implementations using **Microservices**, **AI/ML**, **Autoencoders**, and **Workflow Automation**.

**🔗 Resources:**

- **Website:** [dqman.org](https://dqman.org)  
- **Book Status:** Part I (Version 0.8) \- *Open Access / Free*

## **📂 Repository Structure**

The codebase is organized to correspond with the chapters of the book. Below is a mapping of the directory structure to the concepts covered.

| Directory | Chapter | Description | Tech Stack |
| :---- | :---- | :---- | :---- |
| py\_1\_4\_5\_measurement | 1.4.5 | **Measuring Accuracy:** Combining domain knowledge, rule-based validation, and statistical methods (IQR). | pandas, pandera, numpy |
| py\_1\_5\_2\_confidence\_score | 1.5.2 | **AI Confidence:** Calculating confidence scores for sequence-to-sequence models (Translation). | transformers, torch |
| py\_1\_5\_2\_explainable\_ai | 1.5.2 | **Explainable AI (XAI):** Using LLMs (Llama3 via Ollama) to compare "Black Box" vs. "Chain-of-Thought" reasoning. | ollama |
| py\_1\_6\_1\_failing\_ai\_... | 1.6.1 | **AI Failure Modes:** Demonstrating how clean but biased data causes Neural Networks to fail. Includes SHAP analysis. | pytorch, tensorflow, shap |
| py\_2\_1\_2\_semi\_structured... | 2.1.2 | **Schema Validation:** A multi-pass approach (Validate $\\to$ Cleanse $\\to$ Re-validate) for semi-structured data. | pandera, requests |
| py\_2\_1\_3\_adt\_for\_zip\_code | 2.1.3 | **Abstract Data Types:** Implementing robust ADTs for specific domain types (ZIP codes). | python |
| py\_4\_5\_final\_example... | 4.5 | **Final Project:** A complete Motor Vibration Anomaly Detection system using Microservices, Autoencoders, and FFT. | Docker, Flask, n8n, PyTorch |

## **🚀 Getting Started**

### **Prerequisites**

To run all examples in this repository, you will need:

1. **Python 3.10+**  
2. **Docker & Docker Compose** (Required for Chapter 4.5 Microservices)  
3. **Ollama** (Required for Chapter 1.5 local LLM examples)  
4. **Git**

### **1\. Installation**

Clone the repository and set up a virtual environment:

git clone \[<https://github.com/yourusername/dq-software-engineers.git\>](<https://github.com/yourusername/dq-software-engineers.git>)  
cd dq-software-engineers

\# Create virtual environment  
python3 \-m venv venv

\# Activate environment  
\# On Linux/macOS:  
source venv/bin/activate  
\# On Windows:  
\# venv\\Scripts\\activate.bat

\# Install dependencies  
pip install \-r fqman/requirements.txt

### **2\. Setting up AI Models (Ollama)**

For the Explainable AI examples (py\_1\_5\_2), ensure [Ollama](https://ollama.com/) is installed and running. Pull the required model:

ollama pull llama3

## **💡 Featured Example: Motor Vibration Anomaly Detection**

**Chapter 4.5** presents a comprehensive, real-world scenario: detecting anomalies in motor vibrations using both classical signal processing (FFT) and Deep Learning (Autoencoders).

**Architecture:**

- **Simulate Service:** Generates synthetic vibration data with noise and fault injection.  
- **Train Service:** Trains a PyTorch Autoencoder on "clean" data.  
- **Detect Service:** Infers anomalies on noisy data using reconstruction error.  
- **n8n:** Orchestrates the workflow between services.

### **Running the Microservices**

Navigate to the project folder and use the provided helper scripts:

cd fqman/py\_4\_5\_final\_example\_with\_ai

\# Rebuild and start all containers (Simulation, Train, Detect, n8n)  
./rebuildServicesAndStart.sh

Once running:

1. **n8n Dashboard:** Open <http://localhost:5678/> to view the workflow orchestration.  
2. **Import Workflow:** If not automatically loaded, import Motor\_Vibration\_Control\_Workflow.json from the n8n-import folder.  
3. **Execute:** Trigger the workflow via the Webhook nodes to Simulate $\\to$ Train $\\to$ Detect.

## **🧪 Running Individual Examples**

### **Measurement & Validation (Chapter 1 & 2\)**

To see how to validate semi-structured data using Pandera:

cd fqman/py\_2\_1\_2\_semi\_structured\_data/2\_pass  
python main.py

*Observe how the pipeline performs a diagnostic pass, cleanses the data based on a central schema, and performs a final verification.*

### **AI Failure Analysis (Chapter 1.6)**

To understand why AI fails on "clean" data and how to use **SHAP** values for explainability:

cd fqman/py\_1\_6\_1\_failing\_ai\_on\_clean\_data  
python main\_pytorch\_shap.py

## **🤝 Contribution**

We welcome contributions\! Whether it's fixing a bug in the code, suggesting a new DQ metric, or improving the documentation.

1. Fork the Project  
2. Create your Feature Branch (git checkout \-b feature/AmazingFeature)  
3. Commit your Changes (git commit \-m 'Add some AmazingFeature')  
4. Push to the Branch (git push origin feature/AmazingFeature)  
5. Open a Pull Request

Feedback on the Book:  
If you find errata or have suggestions for the book content, please open an Issue in this repository or contact us via dqman.org.

## **📜 License**

**Book Content:** [Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0)](https://creativecommons.org/licenses/by-nc-nd/4.0/).

**Source Code:** The code samples provided in this repository are available for educational and practical use under the **MIT License**.

**Authors:** Roland Petrasch and Richard Petrasch

*October 2025*
