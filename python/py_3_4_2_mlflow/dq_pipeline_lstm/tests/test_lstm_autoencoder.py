"""
Unit tests for LSTM autoencoder
"""

import pytest
import numpy as np
from ..pipeline.ml_models.lstm_autoencoder import (
    create_lstm_autoencoder,
    prepare_sequences
)


def test_create_lstm_autoencoder():
    """Test LSTM autoencoder creation"""
    model = create_lstm_autoencoder(
        window_size=10,
        n_features=1,
        encoding_dim=8,
        lstm_units=16
    )

    assert model is not None
    assert len(model.layers) > 0

    # Test forward pass
    test_input = np.random.randn(1, 10, 1)
    output = model.predict(test_input, verbose=0)

    assert output.shape == test_input.shape


def test_prepare_sequences():
    """Test sequence preparation"""
    data = np.arange(100).reshape(-1, 1)
    window_size = 10

    sequences = prepare_sequences(data, window_size)

    assert sequences.shape[0] == len(data) - window_size + 1
    assert sequences.shape[1] == window_size
    assert sequences.shape[2] == 1

    # Check first sequence
    assert np.array_equal(sequences[0], data[:window_size])


def test_prepare_sequences_insufficient_data():
    """Test sequence preparation with insufficient data"""
    data = np.arange(5).reshape(-1, 1)
    window_size = 10

    sequences = prepare_sequences(data, window_size)

    assert len(sequences) == 0