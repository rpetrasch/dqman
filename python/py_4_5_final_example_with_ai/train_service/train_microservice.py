# train_microservice.py
"""
This is a simple Flask microservice that trains an autoencoder on vibration data.
It receives vibration data via a POST request, trains the model, and saves it to a file.
It uses PyTorch for the model and numpy for data manipulation.
The service is designed to be part of a larger system for detecting anomalies in motor vibrations.
"""
import os
import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
# shared_path = os.environ.get('SHARED_PATH', '../autoencoder')
# sys.path.append(shared_path)
from autoencoder.autoencoder_pytorch import Autoencoder
# from autoencoder_tensorflow import create_autoencoder, save_trained_model  # tensorflow version
from flask import Flask, request, jsonify
import numpy as np

app = Flask(__name__)


# Flask endpoint
@app.route('/train', methods=['POST'])
def train_endpoint():
    try:
        # Step 1: Receive vibration data
        data = request.get_json()

        # Step 2: Prepare data
        # vibration_data = np.array(data['vibration'])  # tensorflow version
        # vibration_data = vibration_data.reshape(-1, 1)  # tensorflow version
        vibration_data = np.array(data['vibration']).reshape(-1, 1).astype(np.float32)  # Each sample 1D

        # Step 3: Build and train model
        # model = create_autoencoder(input_dim=1)  # tensorflow version
        # model.fit(vibration_data, vibration_data, epochs=20, batch_size=32, verbose=0)   # tensorflow version
        autoencoder = Autoencoder()
        autoencoder.train(vibration_data)

        # Step 4: Save model
        # save_trained_model(model)   # tensorflow version
        autoencoder.save_model()

        return jsonify({"status": "Training completed", "samples": len(vibration_data)})

    except Exception as e:
        return jsonify({"status": "Error", "message": str(e)}), 500


# Start server
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
