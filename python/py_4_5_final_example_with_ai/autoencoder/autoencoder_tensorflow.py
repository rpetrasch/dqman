# autoencoder_tensorflow.py
"""
Autoencoder model using TensorFlow and Keras
This is deprecated because it is not compatible with Python 3.11 and TensorFlow 2.x
"""
# from tensorflow.python.keras.models import Sequential
# from tensorflow.python.keras.layers import Dense
# from tensorflow.python.keras.models import save_model

# Define Autoencoder in TensorFlow
# This is the old TensorFlow version using keras, but it is deprecated in TensorFlow 2.x (only woirks with Python 3.10, TensorFlow 2.12.0)
@DeprecationWarning
def create_autoencoder(input_dim):
    raise DeprecationWarning("This function is deprecated. Use the new version with PyTorch")
    # model = Sequential([
    #     Dense(32, activation='relu', input_shape=(input_dim,)),
    #     Dense(16, activation='relu'),
    #     Dense(32, activation='relu'),
    #     Dense(input_dim, activation='linear')
    # ])
    # model.compile(optimizer='adam', loss='mse')
    return model


@DeprecationWarning
def save_trained_model(model):
    # Save the model to a file
    # save_model(model, "/models/saved_model.h5")
    pass