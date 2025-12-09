"""
LSTM Autoencoder model definition
"""

import numpy as np
from tensorflow.keras.models import Model
from tensorflow.keras.layers import LSTM, Dense, RepeatVector, TimeDistributed, Input


def create_lstm_autoencoder(window_size, n_features, encoding_dim, lstm_units):
    """
    Create LSTM Autoencoder for time series anomaly detection

    Args:
        window_size: Length of input sequence
        n_features: Number of features per timestep
        encoding_dim: Latent dimension size
        lstm_units: Number of LSTM units in encoder/decoder

    Returns:
        Compiled Keras model
    """
    # Encoder
    inputs = Input(shape=(window_size, n_features))
    encoded = LSTM(lstm_units, activation='relu', return_sequences=True)(inputs)
    encoded = LSTM(encoding_dim, activation='relu', return_sequences=False)(encoded)

    # Decoder
    decoded = RepeatVector(window_size)(encoded)
    decoded = LSTM(encoding_dim, activation='relu', return_sequences=True)(decoded)
    decoded = LSTM(lstm_units, activation='relu', return_sequences=True)(decoded)
    decoded = TimeDistributed(Dense(n_features))(decoded)

    # Autoencoder
    autoencoder = Model(inputs, decoded)
    autoencoder.compile(optimizer='adam', loss='mse', metrics=['mae'])

    return autoencoder


def prepare_sequences(data, window_size):
    """
    Create sliding window sequences for LSTM

    Args:
        data: 1D array of values
        window_size: Length of each sequence

    Returns:
        3D numpy array of shape (n_sequences, window_size, n_features)
    """
    sequences = []
    for i in range(len(data) - window_size + 1):
        sequences.append(data[i:i + window_size])

    return np.array(sequences)