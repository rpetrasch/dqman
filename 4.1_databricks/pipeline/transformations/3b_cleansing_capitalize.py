%python
# Transformation: capitalize name and city
import dlt
from pyspark.sql.functions import udf
from pyspark.sql.types import StringType

# 1. Import function from utilities.py
from utilities.str_util import capitalize

# 2. Wrap it as a UDF so Spark understands it
# capitalize_udf = udf(capitalize, StringType()). # Not necessary (it is already wrapped as UDF)

@dlt.table(
    name="clean_names_py",
    comment="Cleans names using custom Python logic"
)
def create_clean_names():
    # Read the previous step
    df = dlt.read("live.filtered_names")

    # Apply custom UDF
    return df.withColumn("name", capitalize("name")) \
             .withColumn("city", capitalize("city"))