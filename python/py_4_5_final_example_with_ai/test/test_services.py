# test_services.py
"""
Test of the microservices
"""
import os
import sys
import requests
import json
import time
import matplotlib.pyplot as plt
import numpy as np
# Set the Python path to one level up so detect_service can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from detect_service.detect_microservice import detect

# Configuration
simulate_url = "http://localhost:5003/simulate"
train_url = "http://localhost:5001/train"
detect_url = "http://localhost:5002/detect"


def plot_full_detection_report(signal, losses, anomalies, threshold):
    """    Plot the full detection report with vibration signal and reconstruction loss.
    Args:
        signal (list): The vibration signal data.
        losses (torch.Tensor): The reconstruction losses for each sample.
        anomalies (list): Indices of detected anomalies in the signal.
        threshold (float): The threshold for anomaly detection.
    """
    fig, axes = plt.subplots(1, 2, figsize=(18, 6))

    x = np.arange(len(signal))  # sample indices

    # --- Left plot: Vibration signal ---
    axes[0].plot(x, signal, label="Vibration Signal", color="blue")
    if len(anomalies) > 0:
        axes[0].scatter(x[anomalies], np.array(signal)[anomalies], color="red", marker="x", s=80, label="Anomalies")
    axes[0].set_xlabel("Sample Index")
    axes[0].set_ylabel("Amplitude")
    axes[0].set_title("Motor Vibration with Anomalies")
    axes[0].legend()
    axes[0].grid(True)

    # --- Right plot: Reconstruction Loss ---
    axes[1].plot(x, losses.cpu().numpy(), label="Reconstruction Error", color="blue")
    if len(anomalies) > 0:
        axes[1].scatter(x[anomalies], losses[anomalies].cpu().numpy(), color="red", marker="x", s=80, label="Anomalies")
    axes[1].axhline(y=threshold, color="green", linestyle="--", label=f"Threshold ({threshold:.6f})")
    axes[1].set_xlabel("Sample Index")
    axes[1].set_ylabel("Reconstruction Error")
    axes[1].set_title("Reconstruction Error with Threshold")
    axes[1].legend()
    axes[1].grid(True)

    plt.suptitle("Motor Vibration Anomaly Detection Report", fontsize=16)
    plt.tight_layout()
    plt.show()

def show_simulation_data():
    simulate_response = requests.get(simulate_url + "?inject_fault=false&noise=0")
    vibration_data_train = simulate_response.json()
    simulate_response = requests.get(simulate_url + "?inject_fault=true&noise=0")
    vibration_data = simulate_response.json()
    for i, (vdt, vd) in enumerate(zip(vibration_data_train['vibration'], vibration_data['vibration'])):
        print(i, end=' ') # Output: (1, 4), (2, 5), (3, 6)
        if vdt != vd:
            print("difference: ", vdt, vd)
        else :
            print(" = ")
    exit(0)

do_train = True
if __name__ == "__main__":
    if do_train:
        # 1. Call simulate-service
        print("1. Simulating motor vibration data...")
        simulate_response = requests.get(simulate_url + "?inject_fault=false&noise=0")
        if simulate_response.status_code != 200:
            print(f"   Simulation failed: {simulate_response.text}")
            exit(1)
        vibration_data_train = simulate_response.json()
        print(f"   Simulated {len(vibration_data_train['vibration'])} samples.")

        # 2. Call train_service
        print("2. Training Autoencoder...")
        train_response = requests.post(train_url, json=vibration_data_train)
        if train_response.status_code != 200:
            print(f"   Training failed: {train_response.text}")
            exit(1)
        print(f"   Training response: {train_response.json()}")
        # Sleep to give filesystem time to write model
        time.sleep(3)

        # 3. Call detect_service (no anomalies expected)
        print("3. Detecting anomalies...")
        detect_response = requests.post(detect_url, json=vibration_data_train)
        if detect_response.status_code != 200:
            print(f"   Detection failed: {detect_response.text}")
            exit(1)
        detect_result = detect_response.json()
        print(f"   Detection result:")
        print(json.dumps(detect_result, indent=2))

    # 4. Create new simulation data and call detect_service
    print("4. Simulating new motor vibration data with fault injection...")
    simulate_response = requests.get(simulate_url + "?inject_fault=true&noise=0.37")
    if simulate_response.status_code != 200:
        print(f"   Simulation failed: {simulate_response.text}")
        exit(1)
    vibration_data = simulate_response.json()
    print(f"   Simulated {len(vibration_data['vibration'])} samples.")

    # 5. Call detect_service (anomalies expected)
    print("5. Detecting anomalies in new simulation data...")
    detect_response = requests.post(detect_url, json=vibration_data)
    if detect_response.status_code != 200:
        print(f"   Detection failed: {detect_response.text}")
        exit(1)
    detect_result = detect_response.json()
    print(f"   Detection result (new simulation): ", len(detect_result['anomalies']))
    # print(json.dumps(detect_result, indent=3))

    # 6. Plot anomalies
    print("6. Plotting detection report...")
    anomalies, losses, threshold, vibration_data = detect(vibration_data)
    plot_full_detection_report(vibration_data, losses, anomalies, threshold)
    time.sleep(5)

