import mlflow
import os
import random

if __name__ == "__main__":

    # --- CONFIGURATION ---
    # 1. Point to the MLflow Tracking Server (Metadata)
    mlflow.set_tracking_uri("http://localhost:5001")

    # 2. Point to the MinIO Server (Artifacts)
    # These environment variables are read by boto3 to authenticate uploads
    os.environ["MLFLOW_S3_ENDPOINT_URL"] = "http://localhost:9000"  # API port, not Console port
    os.environ["AWS_ACCESS_KEY_ID"] = "minio_user"
    os.environ["AWS_SECRET_ACCESS_KEY"] = "minio_password"

    # Set the experiment name
    experiment_name = "Docker_MinIO_Test"
    mlflow.set_experiment(experiment_name)

    print(f"Running experiment: {experiment_name}")

    with mlflow.start_run():
        # A. Log a parameter (key-value pair)
        param_val = 0.5
        mlflow.log_param("alpha", param_val)
        print(f"Logged param alpha: {param_val}")

        # B. Log a metric (numeric value over time)
        accuracy = random.random()
        mlflow.log_metric("accuracy", accuracy)
        print(f"Logged metric accuracy: {accuracy}")

        # C. Create and Log an Artifact (File)
        # We create a dummy file to test if MinIO storage is working
        filename = "test_artifact.txt"
        with open(filename, "w") as f:
            f.write("Hello! If you are reading this, MinIO artifact storage is working.")

        # Upload the file
        mlflow.log_artifact(filename)
        print(f"Uploaded artifact: {filename}")

        # Cleanup local file
        os.remove(filename)

    print("Run Complete! Check http://localhost:5001")