"""
Unit tests for data ingestion module
"""

import pytest
import pandas as pd
from ..pipeline.data_ingestion import ingest_customer_data, ingest_sales_data, ingest_data


def test_ingest_customer_data():
    """Test customer data ingestion"""
    df = ingest_customer_data()

    assert isinstance(df, pd.DataFrame)
    assert 'cid' in df.columns
    assert 'name' in df.columns
    assert 'city' in df.columns
    assert len(df) > 0


def test_ingest_sales_data():
    """Test sales data ingestion"""
    df = ingest_sales_data()

    assert isinstance(df, pd.DataFrame)
    assert 'sid' in df.columns
    assert 'cid' in df.columns
    assert 'amount' in df.columns
    assert len(df) > 0


def test_ingest_data():
    """Test combined data ingestion"""
    customers, sales = ingest_data()

    assert isinstance(customers, pd.DataFrame)
    assert isinstance(sales, pd.DataFrame)
    assert len(customers) > 0
    assert len(sales) > 0