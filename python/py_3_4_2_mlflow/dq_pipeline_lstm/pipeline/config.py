"""
Configuration for MLOps pipeline
"""

class Config:
    # Database
    POSTGRES_URI = "postgresql://user:password@localhost:5432/mlops_db"

    # MLflow
    MLFLOW_TRACKING_URI = "http://localhost:5001"
    MLFLOW_EXPERIMENT_NAME = "sales-anomaly-detection"

    # OpenLineage
    OPENLINEAGE_URL = "http://localhost:5001/api/v1/lineage"
    OPENLINEAGE_NAMESPACE = "sales_pipeline"

    # LSTM Hyperparameters
    WINDOW_SIZE = 30
    ENCODING_DIM = 16
    EPOCHS = 50
    BATCH_SIZE = 32

    # Anomaly Detection
    ANOMALY_THRESHOLD_SIGMA = 3
    MIN_TRANSACTIONS_PER_CUSTOMER = 10

    # Hyperopt
    HYPEROPT_MAX_EVALS = 10