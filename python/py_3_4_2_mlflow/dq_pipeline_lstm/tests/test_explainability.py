"""
Unit tests for explainability module
"""

import pytest
import numpy as np
from ..pipeline.ml_models.lstm_autoencoder import create_lstm_autoencoder
from ..pipeline.explainability import AnomalyExplainer


def test_anomaly_explainer_initialization():
    """Test AnomalyExplainer initialization"""
    model = create_lstm_autoencoder(10, 1, 8, 16)
    background_data = np.random.randn(20, 10, 1)

    explainer = AnomalyExplainer(model, background_data)

    assert explainer.model is not None
    assert explainer.explainer is not None