"""
Example how to define a schema for a semi-structured dataset, and validate/clean/re-validate it
as a 2-pass solution with data cleansing
"""
import pandas as pd
from country_util import get_countries
from schema_service import define_schema, validate

# Create a small DataFrame with one row containing flawed data
df_adults = pd.DataFrame({
    "user_id":   [0, 1, 2, 3, 3, None],  # <-- 0 and None is not allowed
    "age":       [22, 4, "25", 34, 150, 67],  # <-- 150 is outside our allowed age range
    "height_cm": [100, 170.8, 160, 75.0, 310.6, "172"],  # <-- 310 is outside our allowed height range
    "country":   ["United States", "Italy", "New York City", "Brazil", "Japan", "Kenya"]  # <-- New York City is not a country
})

if __name__ == "__main__":
    # Print the DataFrame for the test
    print("Test DataFrame:")
    print(df_adults)
    print()

    # Define the schema
    country_list = get_countries()
    schema = define_schema(country_list)
    print("Schema:")
    print(schema)
    print()

    # Validate the DataFrame
    validation_result = validate(df_adults)
    print("Schema validation result:")
    print(validation_result)

    # Fix the data
    print("Cleansing: Fix the data")
    df_adults = df_adults.dropna(subset=["user_id"])  # Drop rows with missing user_id
    df_adults = df_adults[df_adults["user_id"] > 0]  # Drop rows with user_id <= 0
    df_adults.loc[~df_adults["age"].apply(lambda x: isinstance(x, int)), "age"] = None  # Check if age is an integer
    df_adults.loc[df_adults["age"] > 120, "age"] = None  # Cap ages at 120 (or handle differently)
    df_adults.loc[~df_adults["height_cm"].apply(lambda x: isinstance(x, float)), "height_cm"] = None  # Check if height is a float
    df_adults.loc[~df_adults["country"].isin(country_list), "country"] = None  # Check for valid countries

    # Re-validate the DataFrame
    print("Schema re-validation result:")
    validation_result = validate(df_adults)
    print(validation_result)