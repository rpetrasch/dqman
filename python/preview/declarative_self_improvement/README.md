# Contract Data Quality Assessment – Dual Implementation

This project provides two implementations of a contract analysis system that extracts key clauses from legal contracts using LLMs.

## 📊 Overview

Both implementations:
- Extract key clauses from legal contracts
- Use Ollama gpt-oss (20b) as the LLM backend
- Compare results with synthetic data 
  - The CUAD dataset could not be used due to contract size and annotation quality)
  - Reference: CUAD (Contract Understanding Atticus Dataset: https://www.kaggle.com/datasets/konradb/atticus-open-contract-dataset-aok-beta/data
- Generate detailed evaluation report
- Process 60 contracts

## 🔀 Two Approaches: Manual vs Automatic Optimization for Prompting via DSPy

### 0. Preparation:
- Install Ollama and gpt-oss:20b model
- Create venv (`python -m venv venv`)
- Install requirements (`pip install -r requirements.txt`)
- Download the CUAD dataset (if needed)
- Create folder `data` (in this folder)
- Copy ground truth CSV to `data`
- Run synthetic data generation (`python synthezsize_data.py`)
  Check USE_LOCAL_OLLAMA in config.py:
  - use call_openai: OpenAI API key is required (use .env file)
  - use call_llm: local ollama model
- Check `data` folder for generated files (folder contracts_syn)
- Run optimization (`python optimize.py`)
- Check the optimized_contract_extractor.json file

### 1. Manual Prompting 

**LLM uses hand-crafted prompt**

- **Run** `python analyze_llm.py`
- **Framework**: Ollama and gpt-oss:20b
- **Prompts**: Hand-crafted template
- **Output**: Console output with stats

**Pros**:
- Simpler to understand
- Direct control over prompts
- Faster startup
- Lower complexity

**Best for**: 
- No programming is possible or wanted
- explicit control and transparency over prompts
- Use one single model and need fast results
- Low data volume
- Quality of results is not critical

### 2. DSPy Implementation

**Declarative programming with automatic optimization**

- **Run** `python analyze_dspy.py`
- **Framework**: DSPy 3.1.3
- **Approach**: Declarative signatures + Pydantic + automatic optimization
- **Prompts**: Auto-optimized using BootstrapFewShot
- **Output**: 

**Pros**:
- Automatic prompt optimization
- Saves optimized state
- Composable modules
- Metrics-driven improvement

**Best for**: When you want automatic optimization and reusable models


## 📈 Extracted Clauses

Both implementations extract these 7 clauses:

1. **Parties** - Parties involved in the contract
2. **Agreement Date** - When the agreement was made
3. **Effective Date** - When the contract becomes effective
4. **Expiration Date** - When the contract expires
5. **Renewal Term** - Contract renewal information
6. **Notice Period To Terminate Renewal** - Required notice period
7. **Governing Law** - Applicable jurisdiction


## 📊 Example Results

Both produce similar output formats:

### 1. Manual Prompting 

========================================
FINAL SUMMARY REPORT (LLM)
========================================
Overall Accuracy: 88.79%

Accuracy by Field:
- parties           : 85.84%
- agreement_date    : 93.33%
- effective_date    : 86.67%
- expiration_date   : 77.50%
- renewal_term      : 84.23%
- notice_period     : 96.46%
- governing_law     : 97.51%
========================================


### 2. DSPy Implementation 

========================================
FINAL SUMMARY REPORT (DSPy)
========================================
Overall Accuracy: 90.02%

Accuracy by Field:
- parties           : 92.60%
- agreement_date    : 93.33%
- effective_date    : 85.00%
- expiration_date   : 83.33%
- renewal_term      : 82.53%
- notice_period     : 95.99%
- governing_law     : 97.35%
========================================


## 🛠️ Configuration

Both implementations use settings in `config.py`


