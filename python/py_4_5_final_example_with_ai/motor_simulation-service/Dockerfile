FROM python:3.13-slim

# Install system dependencies required by PyTorch
# Step 1: Install system dependencies required by PyTorch
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    libopenblas-dev \
    libblas-dev \
    liblapack-dev && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Step 2: Install Python dependencies
RUN pip install --no-cache-dir flask numpy scipy matplotlib torch

# Clean up apt caches
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY motor_simulation-service/*.py .

EXPOSE 25002

CMD ["python", "simulate_microservice.py"]
