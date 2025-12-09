from flask import Flask, request, jsonify
from motor_simulator import simulate_motor_vibration

app = Flask(__name__)

# Endpoint for simulating motor vibration
@app.route('/simulate', methods=['GET'])
def simulate_endpoint():
    try:
        duration = float(request.args.get('duration', 5.0))
        sampling_rate = int(request.args.get('sampling_rate', 1000))
        noise = float(request.args.get('noise', 0.3))
        fault_time = float(request.args.get('fault_time', 2.5))
        inject_fault = request.args.get('inject_fault', 'true').lower() == 'true'  # bool("false") ➔ evaluates to True in Python

        t, signal = simulate_motor_vibration(duration, sampling_rate, noise, fault_time, inject_fault)

        return jsonify({"vibration": signal})

    except Exception as e:
        return jsonify({"status": "Error", "message": str(e)}), 500


# Endpoint to receive anomaly data
@app.route('/anomaly', methods=['POST'])
def anomaly_endpoint():
    try:
        anomalies = request.json
        if not anomalies:
            return jsonify({"status": "Error", "message": "Invalid or no data. Did you train the autoencoder?"}), 400

        # Process the anomalies as needed
        print("Received anomalies:", anomalies)

        return jsonify({"status": "Success", "message": "Anomalies received"}), 200

    except Exception as e:
        return jsonify({"status": "Error", "message": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5003)
