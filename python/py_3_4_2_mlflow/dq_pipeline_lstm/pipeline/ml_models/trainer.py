import numpy as np
from tensorflow import keras
from tensorflow.keras.callbacks import EarlyStopping
import optuna
import mlflow
import mlflow.keras
from sklearn.preprocessing import StandardScaler

from .lstm_autoencoder import create_lstm_autoencoder, prepare_sequences


def optimize_hyperparameters(X_train, window_size, n_features, n_trials=10):
    """
    Use Optuna for hyperparameter optimization

    Args:
        X_train: Training sequences
        window_size: Sequence window size
        n_features: Number of features
        n_trials: Number of optimization trials

    Returns:
        dict: Best hyperparameters
    """

    def objective(trial):
        """Objective function for Optuna"""
        # Suggest hyperparameters
        encoding_dim = trial.suggest_int('encoding_dim', 8, 32, step=4)
        lstm_units = trial.suggest_int('lstm_units', 16, 64, step=8)
        learning_rate = trial.suggest_float('learning_rate', 1e-4, 1e-2, log=True)

        # Create and train model
        model = create_lstm_autoencoder(window_size, n_features, encoding_dim, lstm_units)
        model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=learning_rate),
            loss='mse',
            metrics=['mae']
        )

        early_stop = EarlyStopping(monitor='loss', patience=5, restore_best_weights=True)
        history = model.fit(
            X_train, X_train,
            epochs=20,
            batch_size=32,
            verbose=0,
            callbacks=[early_stop]
        )

        # Return final loss
        return history.history['loss'][-1]

    # Run optimization
    study = optuna.create_study(
        direction='minimize',
        sampler=optuna.samplers.TPESampler(seed=42)
    )

    # Suppress Optuna logs
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    study.optimize(objective, n_trials=n_trials, show_progress_bar=False)

    # Return best parameters
    return study.best_params


def train_customer_model(cid, customer_sales, config):
    """
    Train LSTM autoencoder for a single customer (OPTUNA VERSION)

    Returns:
        (model, scaler, threshold) or (None, None, None) if insufficient data
    """
    if len(customer_sales) < config.MIN_TRANSACTIONS_PER_CUSTOMER:
        return None, None, None

    # Prepare data
    amounts = customer_sales['amount'].values.reshape(-1, 1)
    scaler = StandardScaler()
    amounts_scaled = scaler.fit_transform(amounts)

    if len(amounts_scaled) < config.WINDOW_SIZE:
        return None, None, None

    sequences = prepare_sequences(amounts_scaled, config.WINDOW_SIZE)

    # Optimize hyperparameters with Optuna
    best_params = optimize_hyperparameters_optuna(
        sequences, config.WINDOW_SIZE, 1, n_trials=config.HYPEROPT_MAX_EVALS
    )

    # Train final model
    model = create_lstm_autoencoder(
        config.WINDOW_SIZE,
        1,
        best_params['encoding_dim'],
        best_params['lstm_units']
    )

    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=best_params['learning_rate']),
        loss='mse',
        metrics=['mae']
    )

    early_stop = EarlyStopping(monitor='loss', patience=10, restore_best_weights=True)
    history = model.fit(
        sequences, sequences,
        epochs=config.EPOCHS,
        batch_size=config.BATCH_SIZE,
        verbose=0,
        callbacks=[early_stop]
    )

    # Calculate dynamic threshold
    reconstructions = model.predict(sequences, verbose=0)
    mse = np.mean(np.power(sequences - reconstructions, 2), axis=(1, 2))
    threshold = np.mean(mse) + (config.ANOMALY_THRESHOLD_SIGMA * np.std(mse))

    # Log to MLflow
    mlflow.log_param("customer_id", cid)
    mlflow.log_param("n_transactions", len(customer_sales))
    mlflow.log_params(best_params)
    mlflow.log_metric("final_loss", history.history['loss'][-1])
    mlflow.log_metric("anomaly_threshold", threshold)
    mlflow.keras.log_model(model, f"model_customer_{cid}")

    return model, scaler, threshold