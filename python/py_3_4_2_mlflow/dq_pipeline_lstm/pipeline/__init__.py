"""
MLOps Pipeline Package
"""

__version__ = "1.0.0"
__author__ = "Roland and Richard Petrasch"

from .config import Config
from .data_ingestion import ingest_data
from .schema_validation import validate_customer_schema, validate_sales_schema
from .anomaly_detection import train_all_customer_models, detect_anomalies
from .lineage import LineageTracker
from .explainability import AnomalyExplainer
from .database import save_to_postgres
from .reporting import generate_anomaly_report

__all__ = [
    'Config',
    'ingest_data',
    'validate_customer_schema',
    'validate_sales_schema',
    'train_all_customer_models',
    'detect_anomalies',
    'LineageTracker',
    'AnomalyExplainer',
    'save_to_postgres',
    'generate_anomaly_report',
]