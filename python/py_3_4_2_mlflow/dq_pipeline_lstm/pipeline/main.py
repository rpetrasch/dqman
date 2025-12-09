"""
Main pipeline orchestrator
"""

from .config import Config
from .data_ingestion import ingest_data
from .schema_validation import (
    validate_customer_schema, validate_sales_schema,
    print_validation_results, clean_customers, clean_sales
)
from .anomaly_detection import train_all_customer_models, detect_anomalies
from .lineage import LineageTracker
from .explainability import explain_anomalies
from .database import save_to_postgres
from .reporting import generate_anomaly_report


def main():
    """Run complete MLOps pipeline"""
    print("\n" + "=" * 80)
    print("MLOps DATA QUALITY PIPELINE")
    print("=" * 80)

    config = Config()
    lineage = LineageTracker(config)

    # 1. INGEST
    customers, sales = ingest_data()
    lineage.track_ingestion(len(customers), len(sales))

    # 2. SCHEMA VALIDATION
    customer_validation = validate_customer_schema(customers)
    sales_validation = validate_sales_schema(sales)
    print_validation_results(customer_validation, "customers")
    print_validation_results(sales_validation, "sales")
    lineage.track_validation()

    # 3. CLEAN DATA
    customers_clean = clean_customers(customers)
    sales_clean = clean_sales(sales)
    print(f"\n✓ Cleaned: Customers {len(customers)}→{len(customers_clean)}, Sales {len(sales)}→{len(sales_clean)}")
    lineage.track_cleaning(len(customers_clean), len(sales_clean))

    # 4. TRAIN MODELS
    customer_models, customer_scalers, customer_thresholds = train_all_customer_models(
        sales_clean, config
    )
    lineage.track_training(len(customer_models))

    # 5. DETECT ANOMALIES
    good_records, anomalies = detect_anomalies(
        sales_clean, customer_models, customer_scalers, customer_thresholds, config
    )
    lineage.track_anomaly_detection(len(good_records), len(anomalies))

    # 6. EXPLAINABILITY
    anomalies_explained = explain_anomalies(
        anomalies, customer_models, customer_scalers, config
    )

    # 7. SAVE TO DATABASE
    save_to_postgres(good_records, anomalies_explained, config)

    # 8. GENERATE REPORT
    generate_anomaly_report(anomalies_explained)

    print("\n" + "=" * 80)
    print("✓ PIPELINE COMPLETE")
    print("=" * 80)
    print(f"📊 View lineage: Run ID = {lineage.run_id}")
    print(f"📈 View experiments: {config.MLFLOW_TRACKING_URI}")


if __name__ == "__main__":
    main()
