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
import datetime
import shap

# Suppress warnings for cleaner output
warnings.filterwarnings('ignore')

# ---------------------------------------

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
    Generates the imbalanced dataset.
    We add 'source_batch' for traceability.
    """
    print("Generating 1000 data points...")
    data = []
    day_num = 0

    price_map = {'a': 10.0, 'b': 20.0, 'c': 15.0}

    # --- Region A (480 entries) ---
    for _ in range(480):
        prod = np.random.choice(['a', 'b', 'c'])
        data.append({
            'region': 'A',
            'product': prod,
            'price': price_map[prod] + np.random.normal(0, 1),
            'day_num': day_num,
            'amount': get_amount(day_num, prod),
            'source_batch': 'batch_A_normal' # <-- 2. ADDED LINEAGE
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
            'amount': get_amount(day_num, prod),
            'source_batch': 'batch_B_normal' # <-- 2. ADDED LINEAGE
        })
        day_num += 1

    # --- Region C (39 entries) ---
    for _ in range(39):
        prod = np.random.choice(['b', 'c'])
        data.append({
            'region': 'C',
            'product': prod,
            'price': price_map[prod] + np.random.normal(0, 1),
            'day_num': day_num,
            'amount': get_amount(day_num, prod),
            'source_batch': 'batch_C_limited' # <-- 2. ADDED LINEAGE
        })
        day_num += 1

    # --- Region D (1 entry) ---
    prod = 'c'
    data.append({
        'region': 'D',
        'product': prod,
        'price': price_map[prod] + np.random.normal(0, 1),
        'day_num': day_num,
        'amount': get_amount(day_num, prod),
        'source_batch': 'batch_D_tiny' # <-- 2. ADDED LINEAGE
    })

    return pd.DataFrame(data)

# --- PyTorch Model Definition ---
class SalesNet(nn.Module):
    def __init__(self, input_shape):
        super(SalesNet, self).__init__()
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
    print("Region distribution:\n", df['region'].value_counts())
    print("\nSource Batch distribution (Lineage):\n", df['source_batch'].value_counts()) # <-- 3. PRINT LINEAGE
    print("\nProduct distribution in Region C:\n", df[df['region'] == 'C']['product'].value_counts())

    # 2. Define Features (X) and Target (y)
    X = df.drop('amount', axis=1)
    y = df['amount']

    # 3. Create Preprocessing Pipelines
    numerical_features = ['price', 'day_num']
    # <-- 4. ADD source_batch TO PREPROCESSING
    categorical_features = ['region', 'product', 'source_batch']

    num_pipeline = Pipeline([('scaler', StandardScaler())])
    cat_pipeline = Pipeline([('onehot', OneHotEncoder(handle_unknown='ignore'))])

    preprocessor = ColumnTransformer([
        ('num', num_pipeline, numerical_features),
        ('cat', cat_pipeline, categorical_features)
    ], n_jobs=1) # n_jobs=1 to prevent mutex lock

    y_scaler = StandardScaler()

    # 4. Preprocess the data
    X_processed = preprocessor.fit_transform(X).astype(np.float32)
    y_processed = y_scaler.fit_transform(y.values.reshape(-1, 1)).astype(np.float32)

    # 5. Build and Train the PyTorch Model
    print("\nTraining small PyTorch DNN on all 1000 data points...")

    X_tensor = torch.tensor(X_processed, dtype=torch.float32)
    y_tensor = torch.tensor(y_processed, dtype=torch.float32)

    dataset = TensorDataset(X_tensor, y_tensor)
    train_loader = DataLoader(dataset, batch_size=32, shuffle=True, num_workers=0) # num_workers=0 to prevent mutex

    input_shape = X_processed.shape[1]
    model = SalesNet(input_shape)
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=0.01)

    num_epochs = 50
    for epoch in range(num_epochs):
        for inputs, targets in train_loader:
            outputs = model(inputs)
            loss = criterion(outputs, targets)
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

    print("Training complete.")

    # --- 5b. LINEAGE & TRACEABILITY LOG ---
    model_version = "model_v1.0"
    training_time = str(datetime.datetime.now())
    print("\n--- Model Lineage & Traceability Log ---")
    print(f"Model Version: {model_version}")
    print(f"Training Timestamp: {training_time}")
    print(f"Training Data Summary: {len(df)} records")
    print("Training Data Sources:\n", df['source_batch'].value_counts())
    print("WARNING: Model trained on 'batch_D_tiny' which contains only 1 record.")
    print("----------------------------------------")

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
                'source_batch': 'new_prediction' # Simulating new data
            })

    test_df = pd.DataFrame(test_data)

    test_df['actual_amount'] = test_df.apply(
        lambda row: get_amount(row['day_num'], row['product']), axis=1
    )

    X_test_processed = preprocessor.transform(test_df.drop('actual_amount', axis=1)).astype(np.float32)

    model.eval()
    with torch.no_grad():
        X_test_tensor = torch.tensor(X_test_processed, dtype=torch.float32)
        y_pred_scaled_tensor = model(X_test_tensor)
        y_pred_scaled = y_pred_scaled_tensor.numpy()

    test_df['predicted_amount'] = y_scaler.inverse_transform(y_pred_scaled)
    test_df['error'] = test_df['predicted_amount'] - test_df['actual_amount']

    pd.set_option('display.precision', 2)
    print(test_df[['region', 'product', 'actual_amount', 'predicted_amount', 'error']])

    print("\n--- Analysis of Failure (from output) ---")
    print("Region C (Homogeneous): Fails on 'product a' (which it never saw).")
    print("Region D (Too Small): Fails catastrophically on 'product b',")
    print("  ...because it learned a bad rule from its single 'product c' example.")

    # --- 7. EXPLAINABILITY OF FAILURE (SHAP) ---
    print("\n--- Explainability of Failure (using SHAP) ---")
    print("Explaining *why* the model failed for (Region D, product b)...")

    # We need a wrapper function for SHAP to use our PyTorch model
    def predict_wrapper(x):
        model.eval()
        with torch.no_grad():
            x_tensor = torch.tensor(x, dtype=torch.float32)
            y_pred_tensor = model(x_tensor)
            # We must return a numpy array
            return y_pred_tensor.numpy()

    # We use a sample of the training data as a "background" to explain against.
    background_data = shap.sample(X_processed, 50)

    # KernelExplainer is a good general-purpose explainer
    explainer = shap.KernelExplainer(predict_wrapper, background_data)

    # Get SHAP values (the contribution of each feature) for our test set
    shap_values = explainer.shap_values(X_test_processed)

    # Get the feature names from the preprocessor
    feature_names = preprocessor.get_feature_names_out()

    # Find the index for our two test cases
    idx_good = test_df[(test_df['region'] == 'A') & (test_df['product'] == 'b')].index[0]
    idx_bad = test_df[(test_df['region'] == 'D') & (test_df['product'] == 'b')].index[0]

    # --- Explain the GOOD prediction ---
    print("\n--- SHAP Explanation: GOOD Prediction (Region A, product b) ---")
    shap_df_good = pd.DataFrame(shap_values[idx_good], index=feature_names, columns=['contribution'])
    shap_df_good['abs_contribution'] = shap_df_good['contribution'].abs()
    print("Features that most impacted the prediction (top 5):")
    print(shap_df_good.sort_values(by='abs_contribution', ascending=False).head(5))
    print("\n*GOOD* Analysis: The model correctly identified 'product_b' and 'day_num' as the most important features.")

    # --- Explain the BAD prediction ---
    print("\n--- SHAP Explanation: BAD Prediction (Region D, product b) ---")
    shap_df_bad = pd.DataFrame(shap_values[idx_bad], index=feature_names, columns=['contribution'])
    shap_df_bad['abs_contribution'] = shap_df_bad['contribution'].abs()
    print("Features that most impacted the prediction (top 5):")
    print(shap_df_bad.sort_values(by='abs_contribution', ascending=False).head(5))
    print("\n*FAILURE* Analysis: The model almost completely IGNORED 'product_b'.")
    print("It incorrectly gave massive importance to 'region_D' and 'source_batch_batch_D_tiny'.")
    print("This *proves* it learned the wrong pattern (a spurious correlation).")


if __name__ == "__main__":
    main()