# 1.4.5 Measurement of Data Quality: An Example for Accuracy

This small example project demonstrates the practical measurement of the **Accuracy** data quality dimension. It is based on **Section 1.4.5** of the book *Data Quality for Software Engineers* by Roland and Richard Petrasch.

**At the time this example was last checked this chapter started from Page 104. Please reach out if that meanwhile has changed.**

The example uses a dataset of customer ages to illustrate how to combine domain knowledge, rule-based validation, and statistical methods (IQR) to classify data quality issues.

## **Project Overview**

The main goal is to calculate metrics for "Field validation accuracy" and "Error rate" by processing a semi-structured dataset. The workflow follows these steps:

1. **Domain Knowledge Input**: The user defines realistic boundaries for the data (e.g., minimum and maximum valid age).  
2. **Validation \#1 (Rule-based)**: Uses pandera schemas to check for data types and strict violations (e.g., negative ages).  
3. **Filtering**: Removes invalid records to prevent them from skewing statistical analysis.  
4. **Validation \#2 (Statistical)**: Uses the Interquartile Range (IQR) method to identify outliers—values that are technically valid but suspicious (e.g., unusually high ages).  
5. **Reporting**: Classifies records into a "Traffic Light" system:  
   * **Errors (Red)**: Data that violates hard rules.  
   * **Warnings (Yellow)**: Outliers/Suspicious data.  
   * **OK (Green)**: Valid, consistent data.

## **Project Structure**

* main.py: The entry point. It creates the dataset, accepts user input, runs validations, and prints the DQ report.  
* schema\_service.py: Defines the pandera validation schema for the dataset.  
* country\_util.py: Utility module to fetch and save country data (used for extending the dataset context).  
* countries.csv: (Generated) Caches country data fetched from the REST API.

## **Prerequisites**

The project requires Python and the following libraries:

* pandas  
* pandera  
* numpy  
* requests

You can install dependencies via pip:

pip install pandas pandera numpy requests

## **Usage**

Run the main script from your terminal:

python main.py

### **Interactive Steps**

When the script runs, it will display the raw DataFrame and prompt you for domain knowledge:

1. **Enter the minimum age**: (e.g., 18\)  
2. **Enter the maximum age**: (e.g., 80\)

The script will then process the data and output the validation results.

## **Example Output**

Below is an example of the program's execution flow:

Test DataFrame:  
   user\_id   age  
0        1    18  
1        2    16  
2        3    25  
3        4   102  
4        5   150  
5        6    67  
6        7   200  
7        8    \-1  
8        9    45  
9       10  \<NA\>

Enter the minimum age: 18  
Enter the maximum age: 80

1\. Schema Validation (Errors)  
The script identifies values that fail the hard schema check (e.g., age 16 is below 18, \-1 is invalid).  
Schema validation result:  
   schema\_context column  failure\_case  index  
0         Column     age          \<NA\>      9  
2         Column     age            16      1  
3         Column     age           200      6  
4         Column     age            \-1      7

2\. Statistical Validation (Warnings)  
After filtering errors, it checks for outliers. For example, ages like 102 or 150 might be flagged as warnings if they exceed statistical bounds or the domain max.  
\--------------------------------------------------  
a) Errors:  
... (List of invalid records) ...  
\--------------------------------------------------  
b) Warning:  
0     18  
3    102  
4    150  
Name: age, dtype: Int64  
\--------------------------------------------------  
c) OK:  
   user\_id  age  
2        3   25  
5        6   67  
8        9   45

## **Methodology Details**

### **Accuracy Rate Calculation**

The project highlights the difficulty in defining a single "Accuracy Rate" when "suspicious" data exists. It proposes two perspectives:

* **Best Case**: (OK Records \+ Warning Records) / Total Records  
* **Worst Case**: OK Records / Total Records

### **Outlier Detection**

Outliers are determined using the IQR formula:

* $IQR \= Q3 \- Q1$  
* Lower Bound: $Q1 \- (Factor \\times IQR)$  
* Upper Bound: $Q3 \+ (Factor \\times IQR)$

*Note: In this specific example, a strict factor of 0.1 is used to aggressively identify potential issues for demonstration purposes.*
