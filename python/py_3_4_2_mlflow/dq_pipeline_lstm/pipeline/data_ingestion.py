"""
Data ingestion module
"""

import pandas as pd
from io import StringIO


def ingest_customer_data():
    """Load customer master data"""
    csv_content = """cid,name,city,status
1,mike,berlin,
2,susan,new york,
,ralf,melbourne,
4,,dubai,
5,marie,null,
7,Susan,new york,UNVERIFIED
7,Susan,new york,UNVERIFIED"""

    return pd.read_csv(StringIO(csv_content))


def ingest_sales_data():
    """Load sales operational data"""
    csv_content = """sid,cid,date,amount
1,1,1.2.2025,203
2,2,2.2.2025,1500
3,3,3.2.2025,5040
4,4,14.2.2025,2000000"""

    return pd.read_csv(StringIO(csv_content), skipinitialspace=True)


def ingest_data():
    """Load all data sources"""
    print("✓ Data ingested")
    return ingest_customer_data(), ingest_sales_data()