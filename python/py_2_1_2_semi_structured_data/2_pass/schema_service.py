"""
This module defines the schema for the semi-structured data and validates the data against the schema.
"""
import pandera.pandas as pa
from pandera import Column, Check
from pandera.errors import SchemaErrors

schema = None


def is_int(v):
    """
    Custom check function for age validation
    """
    # Return True if we can parse v as an integer
    try:
        int(v)
        return True
    except ValueError:
        return False

def is_valid_age(x):
    """
    Custom check function for age validation
    """
    # Try to parse x as int; return True if 0 <= int(x) <= 120
    try:
        val = int(str(x).strip())  # strip spaces, convert to int
        return 0 <= val <= 120
    except ValueError:
        # If cannot convert to int, it's invalid
        return False

def define_schema(country_list):
    """
    Define the schema for the semi-structured data
    :param country_list:
    :return: schema for countries
    """
    global schema
    schema = pa.DataFrameSchema({
        "user_id": Column(
            int,
            checks=[Check.gt(0)],  # Must be greater than 0
            nullable=False
        ),
        "age": Column(
            object,  # We will try to convert to int (see is_valid_age)
            checks=[
                # Check(lambda s: s.apply(lambda x: isinstance(x, int))),  # 1. Check if all values are integers. This will not work (exception), so we use coerce
                # Check.between(0, 120),  # Between 0 and 120. Replaced by custom age check
                # Check(lambda s: s.apply(is_int), element_wise=True),  # 3. Check if all values can be parsed as integers. This will not work (exception), so we use a custom age check
                Check(lambda s: s.apply(is_valid_age), element_wise=True),  # 4. Custom Check if all values are valid ages
            ],
            # coerce=True,  # 2. Try to convert to int. Also, this will raise an exception
            nullable=False
        ),
        "height_cm": Column(
            float,
            checks=[Check.between(50, 280)],  # Between 50 and 280
            nullable=False
        ),
        "country": Column(
            str,
            checks=[Check.isin(country_list)],  # Must be one of these
            nullable=False
        )
        }
    )
    return schema


def validate(df):
    """
    Validate the DataFrame against the schema and return the result
    :param df: dataframe
    :return: result as a string
    """
    global schema
    if schema is None:
        raise ValueError("Schema not defined. Call define_schema() first.")
    try:
        result = schema.validate(df, lazy=True)  # This will raise an exception if the schema is not valid
    except SchemaErrors as e:
        result = e.failure_cases
    return result
