# **Chapter 1.6.1: Failing AI on "Clean" Data**

**Book:** Data Quality for Software Engineers (DQ4SWE)

## **Overview**

This example demonstrates a critical lesson in Data Quality for AI: **Technically "clean" data (no nulls, correct formats, valid ranges) can still lead to catastrophic AI failures.**

We train a neural network (using PyTorch or TensorFlow) on a synthetic sales dataset. The dataset is perfectly "clean" in a traditional sense, but it suffers from **distributional quality issues**:

1. **Homogeneity:** Region C lacks diversity (missing specific products).  
2. **Small Data:** Region D has insufficient volume (only 1 record).

### **The "True" Signal**

The data is generated with a known mathematical rule:

* Sales generally **increase** over time.  
* **Product 'b'** has a specific negative coefficient (sales decrease).  
* **Region** has *no* effect on the price/amount.

### **The Failure**

Because Region D only had one training example (Product 'c'), the model learns a **spurious correlation**: it assumes "Region D" implies "High Sales" and ignores the actual product characteristics. When asked to predict for "Product 'b'" in Region D, it fails to apply the negative coefficient and predicts a high positive value instead.

## **Files**

| File | Description |
| :---- | :---- |
| main\_pytorch.py | Generates data, trains a PyTorch Neural Network, and evaluates predictions to highlight failures in Regions C and D. |
| main\_pytorch\_shap.py | Extends the PyTorch example using **SHAP (SHapley Additive exPlanations)** to visualize *why* the model failed (showing it ignored the 'Product' feature in favor of the 'Region' feature). |
| main\_tensorflow.py | A TensorFlow/Keras implementation of the same experiment. |

## **Installation & Usage**

Ensure you have the required dependencies installed (PyTorch, pandas, scikit-learn, shap).

### Install dependencies (if not using the global requirements.txt)

`pip install torch pandas scikit-learn shap tensorflow`

### **Running the Standard Experiment**

`python main_pytorch.py`

*Observe the output for "Analysis of Failure". Note how Region D has a massive error.*

### **Running the Explainable AI (XAI) Experiment**

`python main_pytorch_shap.py`

*This will calculate SHAP values. Look for the "FAILURE Analysis" section in the logs to see how the model wrongly weighted the 'Region' feature.*

## **❓ Concept Check**

**Q: Why did the AI model fail even though the data contained no missing values, no typos, and valid data types?**

**A:** The failure was caused by **Distributional Data Quality** issues, specifically **Representativeness** and **Sufficiency**.

1. **Homogeneity (Region C):** The data for Region C was consistent but not *representative* of the full product catalog (it never saw Product A).  
2. **Small Data (Region D):** The data was insufficient. With only one record, the model "overfit," learning a bias (Region D \= Good) instead of the true signal (Product B \= Negative Trend).

**Key Takeaway:** Data Quality for AI extends beyond *correctness* to include *context* and *distribution*.
