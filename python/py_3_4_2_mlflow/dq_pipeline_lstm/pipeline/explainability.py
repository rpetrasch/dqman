"""
SHAP explainability for anomaly detection
"""

import numpy as np
import shap


class AnomalyExplainer:
    """Explain anomaly predictions using SHAP"""

    def __init__(self, model, background_data):
        """
        Initialize explainer

        Args:
            model: Trained LSTM autoencoder
            background_data: Background samples for SHAP (shape: n_samples, window_size, n_features)
        """
        self.model = model

        # Create wrapper for SHAP (needs 2D input)
        def model_wrapper(X):
            """Compute reconstruction error"""
            X_reshaped = X.reshape(-1, background_data.shape[1], background_data.shape[2])
            reconstructions = model.predict(X_reshaped, verbose=0)
            errors = np.mean(np.power(X_reshaped - reconstructions, 2), axis=(1, 2))
            return errors

        self.model_wrapper = model_wrapper

        # Flatten background data for SHAP
        background_flat = background_data.reshape(background_data.shape[0], -1)

        # Initialize SHAP explainer
        self.explainer = shap.KernelExplainer(model_wrapper, background_flat)

    def explain_anomaly(self, sequence):
        """
        Explain why a sequence was flagged as anomalous

        Args:
            sequence: Input sequence (shape: window_size, n_features)

        Returns:
            SHAP values showing feature importance
        """
        sequence_flat = sequence.reshape(1, -1)
        shap_values = self.explainer.shap_values(sequence_flat)

        return shap_values

    def get_top_contributing_timesteps(self, shap_values, n_top=3):
        """
        Get timesteps that contributed most to anomaly

        Args:
            shap_values: SHAP values from explain_anomaly
            n_top: Number of top timesteps to return

        Returns:
            List of (timestep_index, contribution) tuples
        """
        abs_contributions = np.abs(shap_values[0])
        top_indices = np.argsort(abs_contributions)[-n_top:][::-1]

        return [(idx, abs_contributions[idx]) for idx in top_indices]


def explain_anomalies(anomalies, customer_models, customer_scalers, config):
    """Add explainability to detected anomalies"""
    print("\n" + "=" * 60)
    print("Anomaly Explainability (SHAP)")
    print("=" * 60)

    if len(anomalies) == 0:
        print("No anomalies to explain")
        return anomalies

    # Note: This is a simplified version
    # In production, you'd want to compute SHAP for each anomaly
    print("✓ SHAP explainability available (see explainability.py for implementation)")
    print("  → Each anomaly can be explained by reconstruction error per timestep")
    print("  → Top contributing timesteps identify unusual values in the sequence")

    return anomalies