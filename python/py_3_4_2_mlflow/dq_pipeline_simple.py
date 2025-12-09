import mlflow
import pandas as pd
import os
import numpy as np

"""
DQ pipeline with validation:
This simple version of the pipeline uses a hard-coded cleansing and validation.
Ingest -> Clean & Validate -> Decide
"""

# --- CONFIGURATION ---
# Connect to your Dockerized MLflow
mlflow.set_tracking_uri("http://localhost:5001")

# Configure MinIO access (Essential for uploading artifacts)
os.environ["MLFLOW_S3_ENDPOINT_URL"] = "http://localhost:9000"
os.environ["AWS_ACCESS_KEY_ID"] = "minio_user"
os.environ["AWS_SECRET_ACCESS_KEY"] = "minio_password"
os.environ["AWS_DEFAULT_REGION"] = "us-east-1"

# --- 1. DATA GENERATION ---
# Creating the CSV file locally as requested
csv_content = """id,name,city,status
1,mike,berlin,
2,susan,new york,
,ralf,melbourne,
4,,dubai,
5,marie,null,
7,Susan,new york,UNVERIFIED
7,Susan,new york,UNVERIFIED"""

raw_filename = "raw_data.csv"
clean_filename = "cleaned_data.csv"

with open(raw_filename, "w") as f:
    f.write(csv_content)

print(f"Created local file: {raw_filename}")


def validate_and_clean(df):
    """
    Returns a tuple: (cleaned_df, metrics_dict, decision_str)
    """
    total_rows = len(df)

    # A. Check Duplicates
    duplicates = df.duplicated().sum()

    # B. Normalize 'null' strings to actual NaNs
    df.replace(['null', 'NULL', ''], np.nan, inplace=True)

    # C. Check Critical Missing Data (ID or Name must exist)
    missing_critical = df[df['id'].isna() | df['name'].isna()].shape[0]

    # D. Perform Cleaning
    # 1. Drop duplicates
    df_clean = df.drop_duplicates()
    # 2. Drop rows where ID or Name is missing
    df_clean = df_clean.dropna(subset=['id', 'name'])

    remaining_rows = len(df_clean)

    # E. Calculate Quality Score
    quality_score = remaining_rows / total_rows if total_rows > 0 else 0

    metrics = {
        "total_rows": total_rows,
        "duplicate_rows": duplicates,
        "rows_missing_critical_data": missing_critical,
        "remaining_rows": remaining_rows,
        "data_quality_score": quality_score
    }

    # F. Make Decision (Threshold: 70% data retention)
    if quality_score >= 0.70:
        decision = "CLEAN"
    else:
        decision = "BAD"

    return df_clean, metrics, decision


if __name__ == "__main__":
    # --- 2. PIPELINE EXECUTION ---
    experiment_name = "Data_Quality_Gate"
    mlflow.set_experiment(experiment_name)

    print("Starting MLflow Run...")

    with mlflow.start_run(run_name="Data_Ingestion_Job") as run:
        # Step 1: Ingest & Log Raw Data (Bronze Layer)
        # We log this BEFORE validation so we have a record of bad data imports
        mlflow.log_artifact(raw_filename, artifact_path="bronze_raw")
        print("Logged raw data artifact.")

        # Load Data
        df_raw = pd.read_csv(raw_filename)

        # Step 2: Validate & Decide
        df_clean, metrics, decision = validate_and_clean(df_raw)

        # Step 3: Log Metrics & Tags
        mlflow.log_metrics(metrics)
        mlflow.set_tag("data_decision", decision)

        print(f"Validation Complete. Decision: {decision}")
        print(f"Metrics: {metrics}")

        # Step 4: Action based on Decision
        if decision == "CLEAN":
            # Save the cleaned file (Silver Layer)
            df_clean.to_csv(clean_filename, index=False)
            mlflow.log_artifact(clean_filename, artifact_path="silver_cleaned")
            print("Data passed validation. Cleaned version uploaded.")
        else:
            # Do NOT save the clean version, but maybe log a report
            with open("validation_report.txt", "w") as f:
                f.write(f"Data Rejected. Quality Score: {metrics['data_quality_score']:.2f}\n")
                f.write("Reasons: Too many missing IDs or Duplicates.")
            mlflow.log_artifact("validation_report.txt")
            print("Data failed validation. Report uploaded.")

    # Cleanup local files
    if os.path.exists(raw_filename): os.remove(raw_filename)
    if os.path.exists(clean_filename): os.remove(clean_filename)
    if os.path.exists("validation_report.txt"): os.remove("validation_report.txt")