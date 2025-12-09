"""
This module defines the central DQ repository, the schema, and the cleansing logic.
It acts as the Single Source of Truth for data quality rules.
"""
import pandas as pd
import pandera.pandas as pa
from pandera import Column, Check
from pandera.errors import SchemaErrors

# Central DQ Repository (The Single Source of Truth): Define constraints here. Both validation and cleansing will use these variables.
DQ_CONSTRAINTS = {
    "user_id": {
        "min": 1,  # Must be greater than 0
    },
    "age": {
        "min": 0,
        "max": 120,
        "type": "int"
    },
    "height_cm": {
        "min": 50.0,
        "max": 280.0,
        "type": "float"
    },
    # Country list will be injected dynamically or defined here if static
    "country": {
        "valid_list": []
    }
}

schema = None

# Shared Predicate / Helper Functions
def is_valid_numeric(series, min_val, max_val):
    """
    Predicate used by Schema to validate ranges.
    """
    return series.between(min_val, max_val)

# Schema Definition
def define_schema(country_list):
    """
    Define the schema using the Central DQ Repository constraints.
    """
    global schema

    # Update central config with dynamic data
    DQ_CONSTRAINTS["country"]["valid_list"] = country_list

    schema = pa.DataFrameSchema({
        "user_id": Column(
            int,
            # Use the central constraint
            checks=[Check.ge(DQ_CONSTRAINTS["user_id"]["min"])],
            nullable=False,
            coerce=True # Attempt to convert types if possible during validation
        ),
        "age": Column(
            pd.Int64Dtype(), # Use nullable Int to handle NaNs gracefully
            checks=[
                Check.ge(DQ_CONSTRAINTS["age"]["min"]),
                Check.le(DQ_CONSTRAINTS["age"]["max"])
            ],
            nullable=True, # Allow Nulls, but values must be valid if present
            coerce=True
        ),
        "height_cm": Column(
            float,
            checks=[
                Check.ge(DQ_CONSTRAINTS["height_cm"]["min"]),
                Check.le(DQ_CONSTRAINTS["height_cm"]["max"])
            ],
            nullable=True,
            coerce=True
        ),
        "country": Column(
            str,
            checks=[Check.isin(DQ_CONSTRAINTS["country"]["valid_list"])],
            nullable=True,
            coerce=True
        )
    })
    return schema

# Cleansing service (re-using constraints)
def cleanse_data(df: pd.DataFrame) -> pd.DataFrame:
    """
    Cleanse the data using the EXACT SAME constraints defined in DQ_CONSTRAINTS.
    """
    df_clean = df.copy()

    # Phase 1: Pre-processing (Type Coercion)
    # We force types using pandas to_numeric to handle strings like "25" or "172"
    df_clean["user_id"] = pd.to_numeric(df_clean["user_id"], errors='coerce')
    df_clean["age"] = pd.to_numeric(df_clean["age"], errors='coerce')
    df_clean["height_cm"] = pd.to_numeric(df_clean["height_cm"], errors='coerce')

    # Phase 2: Structural Integrity
    # Rule: user_id is mandatory and must be valid
    df_clean = df_clean.dropna(subset=["user_id"])
    df_clean = df_clean[df_clean["user_id"] >= DQ_CONSTRAINTS["user_id"]["min"]]
    # Convert user_id to integer after dropping NaNs
    df_clean["user_id"] = df_clean["user_id"].astype(int)

    # Phase 3: Value Constraints (Using Central Repository)
    # Clean Age: Set to None if outside (min, max)
    mask_age_invalid = ~df_clean["age"].between(
        DQ_CONSTRAINTS["age"]["min"],
        DQ_CONSTRAINTS["age"]["max"]
    )
    df_clean.loc[mask_age_invalid, "age"] = None
    # Clean Height: Set to None if outside (min, max)
    mask_height_invalid = ~df_clean["height_cm"].between(
        DQ_CONSTRAINTS["height_cm"]["min"],
        DQ_CONSTRAINTS["height_cm"]["max"]
    )
    df_clean.loc[mask_height_invalid, "height_cm"] = None
    # Clean Country: Set to None if not in valid list
    valid_countries = DQ_CONSTRAINTS["country"]["valid_list"]
    if valid_countries:
        mask_country_invalid = ~df_clean["country"].isin(valid_countries)
        df_clean.loc[mask_country_invalid, "country"] = None

    return df_clean

# Validation service
def validate(df):
    """
    Validate the DataFrame against the schema.
    """
    global schema
    if schema is None:
        raise ValueError("Schema not defined. Call define_schema() first.")
    try:
        # lazy=True allows checking all errors at once rather than stopping at the first
        result = schema.validate(df, lazy=True)
        return "Validation Successful: No errors found."
    except SchemaErrors as e:
        # Return a summary of failure cases
        return e.failure_cases