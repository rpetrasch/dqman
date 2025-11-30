# **Chapter 2.1.2: Semi-Structured Data Quality**

This project demonstrates how to define schemas, validate data, and handle data quality issues in **semi-structured data** (e.g., JSON, CSV) using Python and the pandera library. It accompanies the book **"Data Quality for Software Engineers (DQ4SWE)"**.

## **📂 Project Structure**

* main.py: The **primary educational script**. It demonstrates a "naive" approach to data cleansing that contains an **INTENDED ERROR** to illustrate a common pitfall.  
* schema\_service.py: Defines the Pandera schema and validation logic.  
* country\_util.py: Utility to fetch valid country names (simulating a reference data check).  
* 2\_pass/: This directory contains the **robust solution**.  
  * 2\_pass/main.py: The fixed orchestrator that runs successfully.  
  * 2\_pass/schema\_service.py: Implements a robust cleanse\_data function using type coercion.

## **🛠️ Installation & Setup**

Prerequisites: Python 3.9+

1. **Create a Virtual Environment**:  
   \# Linux/Mac  

```bash
   python3 \-m venv venv  
   source venv/bin/activate
```

   \# Windows  

```bash
   python \-m venv venv  
   venv\\Scripts\\activate
```

2. Install Dependencies:

   This project relies on the global requirements file located in the parent directory (..).  
    `pip install \-r ../requirements.txt`

## **🚀 Usage**

### **1\. Run the Naive Approach (Educational Crash)**

Run the main script to see the validation in action—and the intended failure.

`python main.py`

**⚠️ Note:** This script will crash with a TypeError. This is **intentional** (see explanation below).

### **2\. Run the Robust Solution (The Fix)**

To see how to correctly handle these data quality issues using a 2-pass strategy (Validate \-\> Cleanse with Coercion \-\> Re-validate):

cd 2\_pass  
`python main.py`

## **🎓 The "Trap": Intended TypeError**

When you run main.py, you will encounter the following error during the cleansing phase:

TypeError: '\>' not supported between instances of 'NoneType' and 'int'

### **❓ Short Question: What went wrong?**

**Q: Why does the script crash even though we tried to clean the data?**

**A:** The script uses a "naive" cleansing approach.

1. It correctly identifies invalid strings (like "sixteen") and turns them into None.  
2. However, the column now contains **mixed types** (integers and None).  
3. The subsequent check df\_adults\["age"\] \> 120 fails because Python cannot compare None with 120\.

**The Lesson:** You cannot perform numerical logic on dirty data. You must apply **Type Coercion** (forcing data into the correct numeric type, converting errors to NaN) *before* applying business logic rules. This is demonstrated in the 2\_pass/ solution.
