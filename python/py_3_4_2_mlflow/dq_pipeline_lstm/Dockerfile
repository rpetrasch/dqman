# Base image
FROM python:3.13-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first (for caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy entire package
COPY pipeline/ ./mlops_pipeline/
COPY setup.py .
COPY README.md .

# Install package
RUN pip install -e .

# Create directories for MLflow artifacts and logs
RUN mkdir -p /app/mlruns /app/logs

# Expose ports
EXPOSE 5000 8888

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV MLFLOW_TRACKING_URI=http://mlflow:5000
ENV POSTGRES_HOST=postgres
ENV POSTGRES_PORT=5432
ENV POSTGRES_DB=mlops_db
ENV POSTGRES_USER=mlops_user
ENV POSTGRES_PASSWORD=mlops_pass

# Default command - now uses package entry point
CMD ["pipeline"]