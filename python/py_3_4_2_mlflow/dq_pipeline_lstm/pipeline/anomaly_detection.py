"""
Anomaly detection logic
"""

import numpy as np
import pandas as pd
import mlflow

from .ml_models import train_customer_model


def train_all_customer_models(sales_clean, config):
    """Train LSTM autoencoder for each customer"""
    print("\n" + "=" * 60)
    print("Training Per-Customer LSTM Autoencoders")
    print("=" * 60)

    mlflow.set_tracking_uri(config.MLFLOW_TRACKING_URI)
    mlflow.set_experiment(config.MLFLOW_EXPERIMENT_NAME)

    customer_models = {}
    customer_scalers = {}
    customer_thresholds = {}

    for cid in sales_clean['cid'].unique():
        customer_sales = sales_clean[sales_clean['cid'] == cid].sort_values('date')

        print(f"\nCustomer {cid}: {len(customer_sales)} transactions")

        with mlflow.start_run(run_name=f"customer_{cid}"):
            model, scaler, threshold = train_customer_model(cid, customer_sales, config)

            if model is not None:
                customer_models[cid] = model
                customer_scalers[cid] = scaler
                customer_thresholds[cid] = threshold
                print(f"  ✓ Model trained | Threshold: {threshold:.2f}")
            else:
                print(f"  → Skipped (insufficient data)")

    return customer_models, customer_scalers, customer_thresholds


def detect_anomalies(sales_clean, customer_models, customer_scalers, customer_thresholds, config):
    """Detect anomalies using trained models"""
    print("\n" + "=" * 60)
    print("Anomaly Detection")
    print("=" * 60)

    anomalies = []
    good_records = []

    for cid in sales_clean['cid'].unique():
        if cid not in customer_models:
            continue

        customer_sales = sales_clean[sales_clean['cid'] == cid].sort_values('date')
        model = customer_models[cid]
        scaler = customer_scalers[cid]
        threshold = customer_thresholds[cid]

        amounts = customer_sales['amount'].values.reshape(-1, 1)
        amounts_scaled = scaler.transform(amounts)

        for i in range(len(amounts_scaled) - config.WINDOW_SIZE + 1):
            sequence = amounts_scaled[i:i + config.WINDOW_SIZE]
            sequence = sequence.reshape(1, config.WINDOW_SIZE, 1)

            reconstruction = model.predict(sequence, verbose=0)
            mse = np.mean(np.power(sequence - reconstruction, 2))

            idx = i + config.WINDOW_SIZE - 1
            row = customer_sales.iloc[idx]

            if mse > threshold:
                anomalies.append({
                    'sid': row['sid'],
                    'cid': row['cid'],
                    'date': row['date'],
                    'amount': row['amount'],
                    'reconstruction_error': mse,
                    'threshold': threshold,
                    'anomaly_score': (mse - threshold) / threshold
                })
            else:
                good_records.append(row)

    print(f"\n✓ Detection complete:")
    print(f"  Good records: {len(good_records)}")
    print(f"  Anomalies: {len(anomalies)}")

    return pd.DataFrame(good_records), pd.DataFrame(anomalies)
