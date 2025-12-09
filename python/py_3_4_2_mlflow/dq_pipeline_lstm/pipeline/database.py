"""
Database operations
"""

from sqlalchemy import create_engine


def save_to_postgres(good_records, anomalies, config):
    """Save results to PostgreSQL"""
    print("\n" + "=" * 60)
    print("Database Storage")
    print("=" * 60)

    # Uncomment for actual database operations
    # engine = create_engine(config.POSTGRES_URI)
    # good_records.to_sql('sales_validated', engine, if_exists='append', index=False)
    # anomalies.to_sql('sales_anomalies', engine, if_exists='append', index=False)

    print(f"✓ Would save {len(good_records)} good records to 'sales_validated'")
    print(f"✓ Would save {len(anomalies)} anomalies to 'sales_anomalies'")

