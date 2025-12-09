"""
Orchestrator for the Data Quality Pipeline.
Implements a multi-pass strategy: Validate (Fail) -> Cleanse -> Validate (Pass).
"""
import pandas as pd
from country_util import get_countries
# Import both validation and cleansing logic from the service
from schema_service import define_schema, validate, cleanse_data

# Create a small DataFrame with flaws
# - user_id: 0 and None (Invalid)
# - age: "25" (Wrong Type), 150 (Out of bounds)
# - height: "172" (Wrong Type), 310.6 (Out of bounds)
# - country: "New York City" (Invalid)
df_adults = pd.DataFrame({
    "user_id":   [0, 1, 2, 3, 3, None],
    "age":       [22, 4, "25", 34, 150, 67],
    "height_cm": [100, 170.8, 160, 75.0, 310.6, "172"],
    "country":   ["United States", "Italy", "New York City", "Brazil", "Japan", "Kenya"]
})

if __name__ == "__main__":
    print("--- Raw Data ---")
    print(df_adults)
    print()

    # 1. Setup phase: Initialize the central schema
    country_list = get_countries()
    schema = define_schema(country_list)
    print("--- Schema Initialized with Central Constraints ---")

    # 2. First pass: Validation (diagnostic)
    # This will fail: show 'dirty' data details.
    print("\n[Pass 1] Validating Raw Data...")
    validation_result = validate(df_adults)

    if isinstance(validation_result, pd.DataFrame):
        print(f"Found {len(validation_result)} data quality issues.")
        # print(validation_result.head()) # Optional: print specific errors
    else:
        print(validation_result)

    # 3. Processing phase: Data cleansing
    # Ask the service to apply the constraints defined in the schema to fix the data.
    print("\n[Processing] Applying Central Cleansing Rules...")
    df_clean = cleanse_data(df_adults)

    print("\n--- Cleaned Data ---")
    print(df_clean)

    # 4. Second pass: Re-validation (verification)
    # This will pass.
    print("\n[Pass 2] Re-Validating Cleaned Data...")
    final_result = validate(df_clean)
    print(final_result)