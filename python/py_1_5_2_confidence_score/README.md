# 1.5.2 Where and How Can AI Be Used for DQ?

**Book:** Data Quality for Software Engineers (DQ4SWE)

## **📖 Overview**

This module demonstrates how to assess the **quality of AI-generated outputs** by calculating confidence scores. Specifically, it utilizes a Sequence-to-Sequence (Seq2Seq) Transformer model to translate text from English to German and computes statistical confidence metrics based on the model's internal probability distribution (logits).

In the context of **Data Quality (DQ)**, relying solely on an AI's raw output is risky. Confidence scores provide metadata about the model's certainty, serving as a proxy for reliability. This example highlights the difference in certainty when the model encounters simple, common language versus complex, specialized vocabulary.

## **🚀 Features**

* **Model:** Uses Helsinki-NLP/opus-mt-en-de (MarianMT), a standard open-source transformer for translation.  
* **Logit Extraction:** Accesses raw output scores to calculate probabilities for each generated token using Softmax.  
* **Metrics Calculated:**  
  * **Token Probabilities:** The certainty for each specific word/sub-word generated.  
  * **Average Confidence:** The mean probability across the entire sentence.  
  * **Minimum Confidence:** The lowest probability encountered, often indicating the "weakest link" in the translation.

## **🛠️ Prerequisites**

* Python 3.8+  
* pip (Python package installer)

## **📦 Installation**

1. **Clone or download** this repository.  
2. **Create a virtual environment** (recommended):  
   python \-m venv .venv  
   \# Windows  
   .venv\\Scripts\\activate  
   \# macOS/Linux  
   source .venv/bin/activate

3. Install dependencies:  
   This script requires Hugging Face Transformers and PyTorch. It also requires sentencepiece for the MarianMT tokenizer.  
   pip install transformers torch sentencepiece

## **💻 Usage**

Run the main script directly from your terminal:

python main.py

*Note: On the first run, the script will download the model weights (\~300MB) from the Hugging Face Hub.*

## **📊 Example Output & Interpretation**

The script compares two inputs: a simple sentence and a complex sentence.

### **1\. Simple Input**

Input: "The bank is closed today."  
Translation: "Die Bank ist heute geschlossen."  
Confidence: \~87.7%

* **Interpretation:** The model has high confidence because the vocabulary is common and the sentence structure is standard.

### **2\. Complex Input**

Input: "The elderly woman's sycophantic praise for the king's pulchritudinous visage..."  
Translation: "Das sykophantische Lob der älteren Frau für das pulchritudinöse Bild..."  
Confidence: \~75.3% (Average) / 23.6% (Minimum)

* **Interpretation:** While the *average* confidence is decent, the *minimum* confidence drops drastically (23.6%). This low score signals a potential data quality issue—the model is "guessing" on specific, rare words (like "accismus" or "lypophrenia").

## **🧠 Data Quality Takeaway**

Automated confidence scoring allows us to build **Quality Gates** for AI systems.

* **High Confidence:** Pass data through automatically.  
* **Low Minimum Confidence:** Flag data for human review (Human-in-the-loop), as the risk of hallucination or inaccuracy is high.

## **📄 License**

This project is part of the open-access book Data Quality for Software Engineers.  
Licensed under CC BY-NC-ND 4.0.
