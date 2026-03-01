"""
This module defines the schema for the person's age check
"""
import pandera.pandas as pa
from pandera import Column, Check
from pandera.errors import SchemaErrors

schema = None  # The schema object to be defined later

# Define the schema for the semi-structured data
def define_schema(min_age, max_age):
    global schema
    schema = pa.DataFrameSchema({
        "age": Column(
            int,  # Allow only int type
            checks=Check.between(min_age, max_age),
            coerce=True,  # Coerce the data to the defined type
            nullable=False
        )
    })
    return schema


# Validate the DataFrame against the schema and return the result
def validate(df):
    global schema
    if schema is None:
        raise ValueError("Schema not defined. Call define_schema() first.")
    try:
        result = schema.validate(df, lazy=True)  # This will raise an exception if the schema is not valid
    except SchemaErrors as e:
        filtered_result = e.failure_cases[["schema_context", "column", "failure_case", "index"]]
        result = filtered_result.to_string()
    return result
