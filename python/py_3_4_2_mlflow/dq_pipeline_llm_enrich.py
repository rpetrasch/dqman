import mlflow
import pandas as pd
import os
import numpy as np
import time

"""
DQ pipeline with LLM Enrichment Step:
This version of the pipeline uses an LLM to standardize text data (e.g., capitalization, inferring context).
Ingest -> Enrich (LLM) -> Validate -> Decide
"""

# --- CONFIGURATION ---
# Connect to your Dockerized MLflow
mlflow.set_tracking_uri("http://localhost:5001")

# Configure MinIO access (Essential for uploading artifacts)
os.environ["MLFLOW_S3_ENDPOINT_URL"] = "http://localhost:9000"
os.environ["AWS_ACCESS_KEY_ID"] = "minio_user"
os.environ["AWS_SECRET_ACCESS_KEY"] = "minio_password"
os.environ["AWS_DEFAULT_REGION"] = "us-east-1"

# LLM Configuration (Toggle this to True if you have a local LLM or OpenAI key)
USE_REAL_LLM = False
LLM_API_KEY = "sk-..."
LLM_BASE_URL = "http://localhost:11434/v1"  # Example for Local Ollama

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
enriched_filename = "enriched_data.csv"
clean_filename = "cleaned_data.csv"

with open(raw_filename, "w") as f:
    f.write(csv_content)

print(f"Created local file: {raw_filename}")


def enrich_with_llm(df):
    """
    Uses an LLM to standardize text data (e.g., capitalization, inferring context).
    Useful for catching duplicates like 'susan' vs 'Susan'.
    """
    print("--- Starting LLM Enrichment ---")
    start_time = time.time()

    if USE_REAL_LLM:
        # Example Code for integrating OpenAI or Local LLM (Ollama)
        try:
            from openai import OpenAI
            client = OpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL)

            def llm_fix_row(row):
                # Prompt engineering to fix data
                prompt = f"Fix capitalization and typos for this person: Name='{row['name']}', City='{row['city']}'. Return JSON."
                # response = client.chat.completions.create(...)
                # return parsed_response
                return row  # Placeholder

            # Apply to dataframe (Note: sequential calls are slow, use batching in production)
            # df = df.apply(llm_fix_row, axis=1)
            pass
        except ImportError:
            print("OpenAI library not installed. Skipping real LLM call.")

    else:
        # --- SIMULATION MODE ---
        # Simulating what an LLM would do: Capitalizing names and cities
        print("Simulating LLM standardization...")

        # 1. Standardize 'name' (susan -> Susan)
        if 'name' in df.columns:
            df['name'] = df['name'].apply(lambda x: str(x).title() if pd.notnull(x) else x)

        # 2. Standardize 'city' (new york -> New York)
        if 'city' in df.columns:
            df['city'] = df['city'].apply(lambda x: str(x).title() if pd.notnull(x) else x)

    print(f"Enrichment took {time.time() - start_time:.2f} seconds.")
    return df


def validate_and_clean(df):
    """
    Returns a tuple: (cleaned_df, metrics_dict, decision_str)
    """
    total_rows = len(df)

    # A. Check Duplicates
    # Note: Because we ran LLM enrichment first, 'susan' and 'Susan' might now
    # effectively be detected as similar entities if IDs match, though here IDs differ.
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

    with mlflow.start_run(run_name="Data_Ingestion_Job_with_LLM") as run:
        # Step 1: Ingest & Log Raw Data (Bronze Layer)
        mlflow.log_artifact(raw_filename, artifact_path="bronze_raw")
        print("Logged raw data artifact.")

        # Load Data
        df_raw = pd.read_csv(raw_filename)

        # Step 2: Enriched (LLM Step)
        # We enrich BEFORE validation to fix typos that might cause validation errors
        df_enriched = enrich_with_llm(df_raw.copy())

        # Save Enriched Data (Silver-Enriched)
        df_enriched.to_csv(enriched_filename, index=False)
        mlflow.log_artifact(enriched_filename, artifact_path="silver_enriched")

        # Step 3: Validate & Decide
        df_clean, metrics, decision = validate_and_clean(df_enriched)

        # Step 4: Log Metrics & Tags
        mlflow.log_metrics(metrics)
        mlflow.set_tag("data_decision", decision)
        mlflow.set_tag("llm_enrichment", "True")

        print(f"Validation Complete. Decision: {decision}")
        print(f"Metrics: {metrics}")

        # Step 5: Action based on Decision
        if decision == "CLEAN":
            # Save the cleaned file (Gold Layer)
            df_clean.to_csv(clean_filename, index=False)
            mlflow.log_artifact(clean_filename, artifact_path="gold_cleaned")
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
    if os.path.exists(enriched_filename): os.remove(enriched_filename)
    if os.path.exists(clean_filename): os.remove(clean_filename)
    if os.path.exists("validation_report.txt"): os.remove("validation_report.txt")