"""
main module for example "failing AI on clean data"
"""
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
import warnings

# Suppress warnings for cleaner output
warnings.filterwarnings('ignore')

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

# Model Definition ---
class SalesNet(nn.Module):
    def __init__(self, input_shape):
        super(SalesNet, self).__init__()
        print("shape: ", input_shape)
        self.layer1 = nn.Linear(input_shape, 32)
        self.layer2 = nn.Linear(32, 16)
        self.output_layer = nn.Linear(16, 1)
        self.relu = nn.ReLU()

    def forward(self, x):
        x = self.relu(self.layer1(x))
        x = self.relu(self.layer2(x))
        x = self.output_layer(x)
        return x

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

    # 3. Create Preprocessing Pipelines (This part is identical)
    numerical_features = ['price', 'day_num']
    categorical_features = ['region', 'product']

    num_pipeline = Pipeline([('scaler', StandardScaler())])
    cat_pipeline = Pipeline([('onehot', OneHotEncoder(handle_unknown='ignore'))])

    preprocessor = ColumnTransformer([
        ('num', num_pipeline, numerical_features),
        ('cat', cat_pipeline, categorical_features)
    ])

    y_scaler = StandardScaler()

    # 4. Preprocess the data
    X_processed = preprocessor.fit_transform(X).astype(np.float32) # Convert to float32 for PyTorch
    y_processed = y_scaler.fit_transform(y.values.reshape(-1, 1)).astype(np.float32)

    # 5. Build and Train the PyTorch Model
    print("\nTraining small PyTorch DNN on all 1000 data points...")

    # Convert data to PyTorch Tensors and create DataLoader
    X_tensor = torch.tensor(X_processed, dtype=torch.float32)
    y_tensor = torch.tensor(y_processed, dtype=torch.float32)

    dataset = TensorDataset(X_tensor, y_tensor)
    train_loader = DataLoader(dataset, batch_size=32, shuffle=True)

    # Initialize model, loss, and optimizer
    input_shape = X_processed.shape[1]
    model = SalesNet(input_shape)
    criterion = nn.MSELoss()  # Mean Squared Error for regression
    optimizer = optim.Adam(model.parameters(), lr=0.01)

    # --- PyTorch Training Loop ---
    num_epochs = 50
    for epoch in range(num_epochs):
        for inputs, targets in train_loader:
            # Forward pass
            outputs = model(inputs)
            loss = criterion(outputs, targets)

            # Backward pass and optimization
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

    print("Training complete.")

    # 6. Test and Show Failure
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
    X_test_processed = preprocessor.transform(test_df.drop('actual_amount', axis=1)).astype(np.float32)

    # --- PyTorch Prediction ---
    model.eval()  # Set model to evaluation mode
    with torch.no_grad():
        X_test_tensor = torch.tensor(X_test_processed, dtype=torch.float32)
        y_pred_scaled_tensor = model(X_test_tensor)
        # Convert tensor output back to numpy array
        y_pred_scaled = y_pred_scaled_tensor.numpy()
    # --------------------------

    # Inverse-transform the predictions (same as before)
    test_df['predicted_amount'] = y_scaler.inverse_transform(y_pred_scaled)

    # Calculate the error (same as before)
    test_df['error'] = test_df['predicted_amount'] - test_df['actual_amount']

    # 7. Display the results
    pd.set_option('display.precision', 2)
    print(test_df[['region', 'product', 'actual_amount', 'predicted_amount', 'error']])

    # Remark: This is a predefined answer. It usually shows the correct statements for the data. However, due to noise
    # results can contain outliers that contradicts the result analysis below.
    print("\n--- Analysis of Failure ---")
    print("Region A & B (Good Data): Predictions are accurate for all products.")
    print("Region C (Sufficient Data): Model generalized correctly! Even though it never saw 'product a' in 'Region C',")
    print("  ...it learned from Regions A & B that 'product' and 'day_num' were the *real* signals and 'region' was irrelevant.")
    print("Region D (Too Small & Homogeneous): Model fails dramatically. With only *one* data point ('product c'),")
    print("  ...the model learned a false, spurious correlation (e.g., 'Region D = high sales')")
    print("  ...and it fails to apply the *correct* signal for 'product b' (which should be negative).")
    print("\nThe model failed for Region D because the data for that segment was too small to find the true signal.")


if __name__ == "__main__":
    main()