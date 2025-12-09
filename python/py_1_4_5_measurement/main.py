"""
Example how to define a schema for a semi-structured dataset, and validate/clean/re-validate it.
"""
import numpy as np
import pandas as pd
from country_util import get_countries
from schema_service import define_schema, validate

maximum_age_error = 150  # Maximum age for the validation
factor_outlier = 0.1  # Factor for outlier detection with the IQR method

# Create a small DataFrame with some records containing flawed data, e.g., negative or unrealistic values
df_customer = pd.DataFrame({
    "user_id":   [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
    "age":       pd.Series([18, 16, 25, 102, 150, 67, 200, -1, 45, None], dtype="Int64"),  # A None value lead to a conversion of all values to be float64,so we need zo use Series with explicit dtype
})

if __name__ == "__main__":
    # Print the DataFrame for the test
    print("Test DataFrame:")
    print(df_customer)
    print()

    # 1. Domain knowledge
    # get the age range from the domain expert
    min_age_domain = int(input('Enter the minimum age:'))
    print('Minimum age (domain): ', min_age_domain)
    max_age_domain = int(input('Enter the maximum age:'))
    print('Maximum age (domain): ', max_age_domain)

    # 2. Validation #1 (errors)
    # Define the schema
    schema = define_schema(min_age_domain, maximum_age_error)
    print("Schema:")
    print(schema)
    print()
    # Validate the DataFrame
    validation1_errors = validate(df_customer)
    # Inform the user about the problems and the need for the missing data
    print("Schema validation result:")
    print(validation1_errors)
    print()
    # Filter the data for the next steps
    print("Filtering")
    df_customer.loc[(df_customer["age"] < min_age_domain) |
                    (df_customer["age"] > maximum_age_error), "age"] = None
    df_customer = df_customer.dropna(subset=["age"])
    print(df_customer)
    print()

    # 3. Validation #2 (statistics): Calculate the IQR and identify outliers
    # Calculate Q1, Q3, and IQR
    ages = df_customer["age"]
    Q1 = np.percentile(ages, 25)
    Q3 = np.percentile(ages, 75)
    IQR = Q3 - Q1
    # Determine outlier boundaries
    multipliers = [1.0, 1.5, 2.0]
    for m in multipliers:
        lower_bound = Q1 - m * IQR
        upper_bound = Q3 + m * IQR
        outliers = ages[(ages < lower_bound) | (ages > upper_bound)]
        print(f"Multiplier {m}: {len(outliers)} outliers detected.")
    print()
    lower_bound = Q1 - factor_outlier * IQR
    upper_bound = Q3 + factor_outlier * IQR
    if upper_bound > max_age_domain:  # Adjust the upper bound if it exceeds the domain knowledge
        upper_bound = max_age_domain
    # Identify outliers
    validation2_outliers = ages[(ages < lower_bound) | (ages > upper_bound)]
    print(f"Outlier candidates: \n{outliers}")

    # 5. Cross-Validation: Compare recorded ages with other fields (e.g., date of birth) or internal/external data for consistency.
    # This step is not implemented in this example

    # 6. Metrics and feedback
    customers_OK_df = df_customer[~df_customer["age"].isin(validation2_outliers)]
    print(f"\033[91m{50*"-"}\na) Errors:\n{validation1_errors}\033[0m")
    print(f"\033[93m{50*"-"}\nb) Warning:\n{validation2_outliers}\033[0m")
    print(f"\033[92m{50*"-"}\nc) OK:\n{customers_OK_df}\033[0m")

    # 7. Re-validate after data correction
    print("Schema re-validation result:")
    validation_result = validate(df_customer)
    print(validation_result)