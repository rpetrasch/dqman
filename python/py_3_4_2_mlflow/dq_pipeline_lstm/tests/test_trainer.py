"""
Unit tests for model trainer
"""

import pytest
import numpy as np
from unittest.mock import Mock
from ..pipeline.ml_models.trainer import optimize_hyperparameters


def test_optimize_hyperparameters():
    """Test hyperparameter optimization"""
    # Create dummy training data
    X_train = np.random.randn(50, 10, 1)

    # Run optimization with minimal evaluations
    best_params = optimize_hyperparameters(
        X_train,
        window_size=10,
        n_features=1,
        max_evals=2  # Minimal for testing
    )

    assert 'encoding_dim' in best_params
    assert 'lstm_units' in best_params
    assert 'learning_rate' in best_params

    assert best_params['encoding_dim'] > 0
    assert best_params['lstm_units'] > 0
    assert best_params['learning_rate'] > 0