"""
Schema validation using Great Expectations 1.0+ (Corrected)
"""

import pandas as pd
import great_expectations as gx
import great_expectations.expectations as gxe
from typing import Dict


def _get_batch(df: pd.DataFrame, datasource_name: str):
    context = gx.get_context()

    # Check datasource names explicitly
    if datasource_name in context.data_sources.all():
        ds = context.data_sources.get(datasource_name)
    else:
        ds = context.data_sources.add_pandas(datasource_name)

    # Check asset names explicitly using the correct API method
    asset_name = f"{datasource_name}_asset"
    if asset_name in ds.get_asset_names():
        asset = ds.get_asset(asset_name)
    else:
        asset = ds.add_dataframe_asset(name=asset_name)

    # Check batch definition names explicitly
    batch_def_name = "default_batch_def"
    # Note: batch_definitions is a dictionary-like object where keys are names
    if batch_def_name in [bd.name for bd in asset.batch_definitions]:
        batch_def = asset.get_batch_definition(batch_def_name)
    else:
        batch_def = asset.add_batch_definition_whole_dataframe(batch_def_name)

    return batch_def.get_batch(batch_parameters={"dataframe": df})

def validate_customer_schema(df: pd.DataFrame) -> Dict[str, bool]:
    """Validate customer data schema"""
    batch = _get_batch(df, "customers_source")

    checks = {
        "cid_not_null": gxe.ExpectColumnValuesToNotBeNull(column="cid"),
        "cid_unique": gxe.ExpectColumnValuesToBeUnique(column="cid"),
        "name_not_null": gxe.ExpectColumnValuesToNotBeNull(column="name"),
        "city_not_null": gxe.ExpectColumnValuesToNotBeNull(column="city"),
    }

    results = {}
    for name, expectation in checks.items():
        # Validate returns a Result object immediately
        validation_result = batch.validate(expectation)
        results[name] = validation_result.success

    return results

def validate_sales_schema(df: pd.DataFrame) -> Dict[str, bool]:
    """Validate sales data schema"""
    batch = _get_batch(df, "sales_source")

    checks = {
        "sid_not_null": gxe.ExpectColumnValuesToNotBeNull(column="sid"),
        "cid_not_null": gxe.ExpectColumnValuesToNotBeNull(column="cid"),
        "date_not_null": gxe.ExpectColumnValuesToNotBeNull(column="date"),
        "amount_not_null": gxe.ExpectColumnValuesToNotBeNull(column="amount"),
        "amount_positive": gxe.ExpectColumnValuesToBeBetween(
            column="amount", min_value=0, max_value=None
        ),
    }

    results = {}
    for name, expectation in checks.items():
        validation_result = batch.validate(expectation)
        results[name] = validation_result.success

    return results

def print_validation_results(results: Dict[str, bool], df_name: str):
    """Print validation results"""
    print(f"\n{'=' * 60}")
    print(f"Schema Validation: {df_name}")
    print(f"{'=' * 60}")

    all_passed = True
    for check_name, success in results.items():
        if success:
            status = "✓ PASS"
        else:
            status = "✗ FAIL"
            all_passed = False
        print(f"{status:<10} : {check_name}")

    print(f"{'-' * 60}")
    print(f"Overall Status: {'✅ SUCCESS' if all_passed else '❌ FAILED'}")

def clean_customers(df: pd.DataFrame) -> pd.DataFrame:
    """Clean customer data"""
    clean = df.copy()
    clean = clean.dropna(subset=['cid', 'name', 'city'])
    clean = clean.drop_duplicates(subset=['cid'])
    clean['cid'] = clean['cid'].astype(int)
    return clean

def clean_sales(df: pd.DataFrame) -> pd.DataFrame:
    """Clean sales data"""
    clean = df.copy()
    clean = clean.dropna(subset=['sid', 'cid', 'date', 'amount'])
    clean['cid'] = clean['cid'].astype(int)
    clean['amount'] = clean['amount'].astype(float)
    clean['date'] = pd.to_datetime(clean['date'], format='%d.%m.%Y', errors='coerce')
    clean = clean.dropna(subset=['date'])
    clean = clean[clean['amount'] > 0]
    return clean

# --- Main Execution Block ---
if __name__ == "__main__":
    # Create Dummy Data
    df_customers = pd.DataFrame({
        "cid": [1, 2, 2, None],
        "name": ["Alice", "Bob", "Bob", "Dave"],
        "city": ["NYC", "LA", "LA", None]
    })

    # 1. Validate Raw
    print("--- Validating Raw Data ---")
    results = validate_customer_schema(df_customers)
    print_validation_results(results, "Customers Raw")

    # 2. Clean
    df_clean = clean_customers(df_customers)

    # 3. Validate Clean
    print("\n--- Validating Clean Data ---")
    results_clean = validate_customer_schema(df_clean)
    print_validation_results(results_clean, "Customers Clean")