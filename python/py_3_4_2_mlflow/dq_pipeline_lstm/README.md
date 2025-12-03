# MLOps Data Quality Pipeline

Production-ready MLOps pipeline with LSTM Autoencoder for anomaly detection, complete with lineage tracking, explainability, and comprehensive testing.

## Features

- **LSTM Autoencoder**: Per-customer anomaly detection with temporal awareness
- **Hyperparameter Optimization**: Automated tuning with Hyperopt
- **Data Lineage**: Full OpenLineage integration
- **Explainability**: SHAP-based anomaly explanations
- **Visualization**: Interactive dashboards and SHAP plots
- **Testing**: Comprehensive unit test suite
- **Containerization**: Docker and Docker Compose deployment

## Quick Start

### Local Installation

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install package
pip install -e .

# Run pipeline
- pipeline 
or
- python pipeline/main.py
or
-  python -m pipeline.main

### Docker Deployment

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f mlops_pipeline

# Access services:
# - MLflow UI: http://localhost:5000
# - MinIO Console: http://localhost:9001
# - Jupyter Notebook: http://localhost:8888
# - PostgreSQL: localhost:5432
```

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Ingest    │────▶│  Validate    │────▶│   Clean     │
└─────────────┘     └──────────────┘     └─────────────┘
                                                
