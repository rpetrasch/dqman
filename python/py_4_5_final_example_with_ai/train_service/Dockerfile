FROM python:3.13-slim

#RUN pip install flask tensorflow==2.12.0
RUN pip install --no-cache-dir flask numpy
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# Clean up apt caches
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Copy the shared module
COPY autoencoder /app/autoencoder
# Copy the service's specific files
COPY train_service/train_microservice.py /app/

EXPOSE 5001

CMD ["python", "train_microservice.py"]
