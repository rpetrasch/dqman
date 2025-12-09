# detect_microservice.py
"""
This is a simple Flask microservice that trains an autoencoder on vibration data.
It receives vibration data via a POST request, trains the model, and saves it to a file.
It uses PyTorch for the model and numpy for data manipulation.
The service is designed to be part of a larger system for detecting anomalies in motor vibrations.
"""
import os
import sys
from time import sleep
import logging
import torch
logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
# shared_path = os.environ.get('SHARED_PATH', '../autoencoder')
# sys.path.append(shared_path)
from autoencoder.autoencoder_pytorch import Autoencoder
from flask import Flask, request, jsonify
import numpy as np
# from tensorflow.python.keras.models import load_model   # tensorflow version

app = Flask(__name__)
autoencoder = Autoencoder()
model = None

# Parameters
# RECONSTRUCTION_ERROR_THRESHOLD = 0.01  # tensorflow version ToDo tune this threshold
k = 3.0 # multiplier for the standard deviation (σ = std) used for dynamic threshold


def detect(data):
    # Step 1: Prepare data
    # vibration_data = np.array(data['vibration'])  # tensorflow version
    # vibration_data = vibration_data.reshape(-1, 1)  # tensorflow version
    vibration_data = np.array(data['vibration']).reshape(-1, 1).astype(np.float32)  # Each sample 1D
    vibration_tensor = torch.from_numpy(vibration_data)

    # Step 2: Predict reconstructed signal
    # reconstructed = model.predict(vibration_data, verbose=0) # Tensorflow version
    with torch.no_grad():  # Calculate losses for all samples
        reconstructed = autoencoder(vibration_tensor)
        losses = ((reconstructed - vibration_tensor) ** 2).mean(dim=1)

        # Step 3: Calculate reconstruction error
        # errors = np.mean(np.square(vibration_data - reconstructed), axis=1)  # Tensorflow version

        # Step 4: Detect anomalies
        # anomalies = np.where(errors > RECONSTRUCTION_ERROR_THRESHOLD)[0]  # Tensorflow version
        # anomalies = (loss > 0.01).nonzero(as_tuple=True)[0]  # hard-coded threshold for anomalies
        # Calculate dynamic threshold
        mean_loss = losses.mean()
        std_loss = losses.std()
        threshold = mean_loss + k * std_loss
        anomalies = (losses > threshold).nonzero(as_tuple=True)[0]  # Use dynamic threshold
        return anomalies, losses, threshold, vibration_data

# Flask endpoint
@app.route('/detect', methods=['POST'])
def detect_endpoint():
    global autoencoder, k
    try:
        # receive vibration data
        data = request.get_json()
        # get detection data
        anomalies, losses, threshold, vibration_data = detect(data)
        anomaly_list = anomalies.tolist()
        # return the results
        return jsonify({
            "status": "Detection completed",
            "anomalies": anomaly_list,
            "total_samples": len(vibration_data)
        })
    except Exception as e:
        return jsonify({"status": "Error", "message": str(e)}), 500


# Start server
if __name__ == "__main__":
    logger.info("1. loading model ...")
    loaded = False
    while not loaded:
        try:
            autoencoder.load_model()
            loaded = True
            break
        except Exception as e:
            sleep(3)
            logger.error("model file not found")
            pass
    logger.info("loading done")
    logger.info("2. starting web services")
    app.run(host="0.0.0.0", port=5002)