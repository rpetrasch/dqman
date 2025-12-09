FROM python:3.13-slim

#RUN pip install --no-cache-dir flask tensorflow==2.12.0
RUN pip install --no-cache-dir flask numpy
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# Clean up apt caches
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY autoencoder /app/autoencoder
COPY detect_service/detect_microservice.py .

EXPOSE 25002

CMD ["python", "detect_microservice.py"]
