import importlib
import importlib.util
import sys
import time
import traceback
import numpy as np
import pandas as pd

try:
    from sales_data_generator import read_data, get_amount
except ImportError:
    import sys
    import os
    sys.path.append(os.path.dirname(os.path.abspath(__file__)))
    from sales_data_generator import read_data, get_amount

# Read the sales data
data = read_data()

# Weights
WEIGHTS = {
    "accuracy": 0.8,
    "efficiency": 0.1,
    "brevity": 0.1
}

def import_candidate_module(program_path):
    spec = importlib.util.spec_from_file_location("candidate", program_path)
    module = importlib.util.module_from_spec(spec)
    sys.modules["candidate"] = module
    spec.loader.exec_module(module)
    return module


def showCandidateCode(program_path):
    with open(program_path, 'r') as f:
        print(f.read())


def evaluate(program_path):
    try:
        # 1. Load the generated program
        candidate = import_candidate_module(program_path)
        showCandidateCode(program_path)  # For debugging purposes

        # 2. Setup Test Data
        test_input = data # .copy()
        
        # Determine target day (next day after history)
        last_day = int(test_input['day_num'].max())
        target_day = last_day + 20
        
        # 3. Calculate Ground Truth for target_day based on Linear Regression of history
        expected_rows = []
        for region in ['A', 'B']:
            for prod in ['a', 'b', 'c', 'd']:
                 # Filter history
                subset = test_input[(test_input['region'] == region) & (test_input['product'] == prod)]
                
                if subset.empty:
                    pred_amount = None # 0.0
                elif len(subset) == 1:
                    # Single point -> assume constant
                    pred_amount = None  # float(subset['amount'].iloc[0])
                else:
                    # Linear Regression: y = mx + c
                    x = subset['day_num'].values
                    y = subset['amount'].values
                    # Use numpy polyfit for simple linear regression (deg=1): This minimizes squared error
                    try:
                        slope, intercept = np.polyfit(x, y, 1)
                        pred_amount = slope * target_day + intercept
                    except Exception:
                        # Fallback if fit fails
                        pred_amount = float(y.mean())

                expected_rows.append({
                    'region': region,
                    'product': prod,
                    'amount': pred_amount
                })
        expected_df = pd.DataFrame(expected_rows)

        # 4. Measure Execution Time
        start_time = time.perf_counter()
        
        # Call the candidate function
        # Signature: forecast_sales(historical_data, predict_day)
        if hasattr(candidate, 'forecast_sales'):
            # Pass predict_day
            result_df = candidate.forecast_sales(test_input, target_day)
        else:
            raise ValueError("Candidate missing 'forecast_sales' function.")
            
        end_time = time.perf_counter()
        latency = end_time - start_time

        # 5. Validation and Scoring
        if not isinstance(result_df, pd.DataFrame):
             return {"combined_score": 0.0, "error": "Output must be a DataFrame", "accuracy": 0.0}
        
        # Check for required columns
        required_cols = {'region', 'product', 'amount'}
        if not required_cols.issubset(result_df.columns):
             return {"combined_score": 0.0, "error": f"Output keys missing. Found {result_df.columns}", "accuracy": 0.0}

        # Merge expected and result to compare
        # We merge on region/product
        merged = pd.merge(expected_df, result_df, on=['region', 'product'], how='left', suffixes=('_true', '_pred'))
        
        # Clean up any missing predictions (NaN)
        merged['amount_pred'] = merged['amount_pred'].fillna(0.0)
        
        # Calculate MAE
        # Verify columns are numeric
        try:
            merged['amount_pred'] = pd.to_numeric(merged['amount_pred'])
            mae = np.mean(np.abs(merged['amount_true'] - merged['amount_pred']))
        except Exception as e:
            return {"combined_score": 0.0, "error": f"Non-numeric prediction: {e}", "accuracy": 0.0}

        # Normalize MAE to 0-1
        # Scale: if MAE is around 50 (100% error approx), score should be low.
        accuracy = 1.0 / (1.0 + (mae / 20.0))

        # Efficiency
        efficiency_score = 1.0 / (1.0 + latency)

        # Brevity
        with open(program_path, 'r') as f:
            lines = len(f.readlines())
        brevity_score = 1.0 / (1.0 + (lines / 100.0))

        combined_score = (
            (WEIGHTS["accuracy"] * accuracy) +
            (WEIGHTS["efficiency"] * efficiency_score) +
            (WEIGHTS["brevity"] * brevity_score)
        )

        return {
            "combined_score": combined_score,
            "accuracy": accuracy,
            "mae": mae,
            "latency_sec": latency,
            "lines_of_code": lines
        }

    except Exception as e:
        return {
            "combined_score": 0.0,
            "error": str(e),
            "accuracy": 0.0
        }

if __name__ == "__main__":
    import os
    sys.path.append(os.getcwd())
    if os.path.exists("sales_forecaster.py"):
        print(evaluate("sales_forecaster.py"))