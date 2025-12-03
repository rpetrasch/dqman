"""
Unit tests for schema validation
"""

import pytest
import pandas as pd
from pipeline.schema_validation import (
    validate_customer_schema,
    validate_sales_schema,
    clean_customers,
    clean_sales
)


def test_validate_customer_schema():
    """Test customer schema validation"""
    # Valid data
    df = pd.DataFrame({
        'cid': [1, 2, 3],
        'name': ['Alice', 'Bob', 'Charlie'],
        'city': ['NYC', 'LA', 'SF'],
        'status': ['ACTIVE', 'ACTIVE', 'INACTIVE']
    })

    results = validate_customer_schema(df)
    assert isinstance(results, dict)
    assert 'cid_not_null' in results


def test_clean_customers():
    """Test customer data cleaning"""
    df = pd.DataFrame({
        'cid': [1, 2, None, 4],
        'name': ['Alice', 'Bob', 'Charlie', None],
        'city': ['NYC', 'LA', 'SF', 'Boston'],
        'status': ['ACTIVE', 'ACTIVE', 'INACTIVE', 'ACTIVE']
    })

    clean_df = clean_customers(df)

    assert len(clean_df) < len(df)  # Some rows removed
    assert clean_df['cid'].notna().all()
    assert clean_df['name'].notna().all()


def test_clean_sales():
    """Test sales data cleaning"""
    df = pd.DataFrame({
        'sid': [1, 2, 3],
        'cid': [1, 2, 3],
        'date': ['1.1.2025', '2.1.2025', '3.1.2025'],
        'amount': [100, -50, 200]
    })

    clean_df = clean_sales(df)

    assert (clean_df['amount'] > 0).all()  # Negative amounts removed
    assert clean_df['date'].dtype == 'datetime64[ns]'
