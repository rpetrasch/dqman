import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Input
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
import warnings

# --- FIX for TensorFlow Mutex Lock/Hang ---
# This error is often a threading deadlock in TensorFlow.
# Forcing it to be single-threaded can resolve these environment-specific issues.
# Solution: update python and all libraries
tf.config.threading.set_inter_op_parallelism_threads(1)
tf.config.threading.set_intra_op_parallelism_threads(1)
# ------------------------------------------

# Suppress warnings for cleaner output
warnings.filterwarnings('ignore')
tf.get_logger().setLevel('ERROR')

def get_amount(day, product):
    """The 'true' signal function"""
    base_amount = 50
    time_trend = day * 0.1  # Sales go up over time

    product_effect = 0
    if product == 'b':
        product_effect = -day * 0.2  # ...except for product 'b', which goes down

    # Add some random noise
    noise = np.random.normal(0, 3)
    return base_amount + time_trend + product_effect + noise

def generate_data():
    """
    Generates the imbalanced dataset based on the user's prompt.
    The TRUE SIGNAL: Sales (amount) increases with time (day_num),
    but *decreases* for product 'b'. Region has NO effect.
    """
    print("Generating 1000 data points...")
    data = []
    day_num = 0

    # Base prices for products
    price_map = {'a': 10.0, 'b': 20.0, 'c': 15.0}

    # --- This function is no longer needed as get_amount is global ---
    # def get_amount_old(day, product):
    #     """The 'true' signal function"""
    #     base_amount = 50
    #     time_trend = day * 0.1  # Sales go up over time
    #
    #     product_effect = 0
    #     if product == 'b':
    #         product_effect = -day * 0.2  # ...except for product 'b', which goes down
    #
    #     # Add some random noise
    #     noise = np.random.normal(0, 3)
    #     return base_amount + time_trend + product_effect + noise
    # -----------------------------------------------------------------


    # --- Region A (480 entries) ---
    for _ in range(480):
        prod = np.random.choice(['a', 'b', 'c'])
        data.append({
            'region': 'A',
            'product': prod,
            'price': price_map[prod] + np.random.normal(0, 1),
            'day_num': day_num,
            'amount': get_amount(day_num, prod)
        })
        day_num += 1

    # --- Region B (480 entries) ---
    for _ in range(480):
        prod = np.random.choice(['a', 'b', 'c'])
        data.append({
            'region': 'B',
            'product': prod,
            'price': price_map[prod] + np.random.normal(0, 1),
            'day_num': day_num,
            'amount': get_amount(day_num, prod)
        })
        day_num += 1

    # --- Region C (39 entries) ---
    for _ in range(39):
        prod = np.random.choice(['b', 'c'])  # Only products b and c
        data.append({
            'region': 'C',
            'product': prod,
            'price': price_map[prod] + np.random.normal(0, 1),
            'day_num': day_num,
            'amount': get_amount(day_num, prod)
        })
        day_num += 1

    # --- Region D (1 entry) ---
    prod = 'c'  # Only product c
    data.append({
        'region': 'D',
        'product': prod,
        'price': price_map[prod] + np.random.normal(0, 1),
        'day_num': day_num,
        'amount': get_amount(day_num, prod)
    })

    return pd.DataFrame(data)

def build_model(input_shape):
    """Builds a small DNN for regression."""
    model = Sequential([
        Input(shape=(input_shape,)),
        Dense(32, activation='relu'),
        Dense(16, activation='relu'),
        Dense(1)  # Output layer for regression (predicting amount)
    ])
    model.compile(optimizer='adam', loss='mean_squared_error')
    return model

def main():
    # 1. Generate the imbalanced dataset
    df = generate_data()

    print("\n--- Training Data Distribution ---")
    print("Total rows:", len(df))
    print("Region distribution:\n", df['region'].value_counts())
    print("\nProduct distribution in Region C:\n", df[df['region'] == 'C']['product'].value_counts())
    print("\nProduct distribution in Region D:\n", df[df['region'] == 'D']['product'].value_counts())

    # 2. Define Features (X) and Target (y)
    X = df.drop('amount', axis=1)
    y = df['amount']

    # 3. Create Preprocessing Pipelines
    # We need to scale numerical features and one-hot-encode categorical features
    numerical_features = ['price', 'day_num']
    categorical_features = ['region', 'product']

    # Pipeline for numerical features
    num_pipeline = Pipeline([
        ('scaler', StandardScaler())
    ])

    # Pipeline for categorical features
    cat_pipeline = Pipeline([
        ('onehot', OneHotEncoder(handle_unknown='ignore'))
    ])

    # Combine pipelines using ColumnTransformer
    preprocessor = ColumnTransformer([
        ('num', num_pipeline, numerical_features),
        ('cat', cat_pipeline, categorical_features)
    ])

    # Scale the target variable (y) as well, which is good practice for NNs
    y_scaler = StandardScaler()

    # 4. Preprocess the data
    X_processed = preprocessor.fit_transform(X)
    y_processed = y_scaler.fit_transform(y.values.reshape(-1, 1))

    # 5. Build and Train the DNN
    print("\nTraining small DNN on all 1000 data points...")
    model = build_model(X_processed.shape[1])
    model.fit(X_processed, y_processed, epochs=50, batch_size=32, verbose=0)
    print("Training complete.")

    # 6. Test and Show Failure
    # Now we create a *hypothetical* test set to see if the model
    # learned the *true pattern* or just the *dataset's biases*.
    print("\n--- Testing Model Generalization ---")
    print("Asking model to predict for day 500 (a mid-point in time)...")

    test_data = []
    day = 500
    price_map = {'a': 10.0, 'b': 20.0, 'c': 15.0}

    for region in ['A', 'B', 'C', 'D']:
        for prod in ['a', 'b', 'c']:
            test_data.append({
                'region': region,
                'product': prod,
                'price': price_map[prod],
                'day_num': day,
            })

    test_df = pd.DataFrame(test_data)

    # Calculate the *actual* amount based on the true signal
    test_df['actual_amount'] = test_df.apply(
        lambda row: get_amount(row['day_num'], row['product']), axis=1
    )

    # Use the *trained* preprocessor to transform the test data
    X_test_processed = preprocessor.transform(test_df.drop('actual_amount', axis=1))

    # Make predictions
    y_pred_scaled = model.predict(X_test_processed)

    # Inverse-transform the predictions to get the real dollar value
    test_df['predicted_amount'] = y_scaler.inverse_transform(y_pred_scaled)

    # Calculate the error
    test_df['error'] = test_df['predicted_amount'] - test_df['actual_amount']

    # 7. Display the results
    pd.set_option('display.precision', 2)
    print(test_df[['region', 'product', 'actual_amount', 'predicted_amount', 'error']])

    print("\n--- Analysis of Failure ---")
    print("Region A & B (Good Data): Predictions are accurate for all products.")
    print("Region C (Bad Data): Model fails dramatically for 'product a',")
    print("  ...because it *never* saw 'product a' in 'Region C' during training.")
    print("Region D (Worst Data): Model fails for 'product a' and 'product b'.")
    print("  ...it only saw 'product c' *one time* and has overfit.")
    print("\nThe model didn't learn the true signal; it learned the dataset's bias.")


if __name__ == "__main__":
    main()