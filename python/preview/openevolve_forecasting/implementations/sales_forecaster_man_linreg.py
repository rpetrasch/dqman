import pandas as pd
import numpy as np
import scipy
import scipy.stats
from pandas import DataFrame

def forecast_sales(historical_data: pd.DataFrame, predict_day: int) -> DataFrame:
    """
    Input: DataFrame with columns [day_num, region, product, amount] with
    - day_num: integer, day number (starting at 0)
    - region: string, region (A or B)
    - product: string, product (a, b, c, d)
    - amount: float, actual sales amount for that day and region/product combination
    => number of rows is arbitrary (many days per product/region), e.g.
       100 days (0..100) for one product and region and 50 days (0,2,4,6,.100) for another
    Output: DataFrame with [day_num, region, product, amount]
    - day_num: integer, one single day in the future = predict_day
    - region: string, region (A or B)
    - product: string, product (a, b, c, d)
    - amount: float, predicted sales amount for that day and region/product combination
    => number of rows is fixed to numer of products * regions (predictions for the day day_num), e.g.
       for 2 regions and 4 products, 8 rows are returned
    """
    # EVOLVE-BLOCK-START

    expected_rows = []
    # Global horizon
    last_global_day = int(historical_data["day_num"].max())
    future_days = list(range(last_global_day + 1, last_global_day + 31))

    regions = sorted(historical_data["region"].dropna().unique().tolist())
    products = sorted(historical_data["product"].dropna().unique().tolist())

    # create a prediction model (calculate the linear regression slope and intercept for each (region, product) pair)
    # and use it to predict the amount for the predict_day
    for region in regions:
        for product in products:
            # filter history
            subset = historical_data[(historical_data['region'] == region) & (historical_data['product'] == product)]
            
            if subset.empty:
                pred_amount = None # 0.0
            elif len(subset) == 1:
                # Single point -> assume constant
                pred_amount = None  # float(subset['amount'].iloc[0])
            else:
                # Linear Regression: y = mx + c
                x = subset['day_num'].values
                y = subset['amount'].values
                
                # ToDo Use numpy or scipy for simple linear regression (deg=1)
                try:
                    # 1. Calculate the means
                    mean_x = sum(x) / len(x)
                    mean_y = sum(y) / len(y)
                    # 2. Calculate the terms for the slope formula
                    numerator = 0
                    denominator = 0
                    for i in range(len(x)):
                        numerator += (x[i] - mean_x) * (y[i] - mean_y)
                        denominator += (x[i] - mean_x) ** 2
                    # 3. Final slope and intercept
                    slope = numerator / denominator
                    intercept = mean_y - (slope * mean_x)
                    # 4. Calculate predicted amount
                    pred_amount = slope * predict_day + intercept
                except Exception:
                    # Fallback if fit fails
                    pred_amount = float(y.mean())
            # Fill the result row
            expected_rows.append({
                'region': region,
                'product': product,
                'day_num': predict_day,
                'amount': pred_amount
            })

    return pd.DataFrame(expected_rows)
    # EVOLVE-BLOCK-END
    
    return predictions

if __name__ == "__main__":
    from sales_data_generator import read_data
    try:
        data = read_data()
        last_day = int(data['day_num'].max())
        target_day = last_day + 20
        predictions = forecast_sales(data, target_day)
        print(predictions)
    except Exception as e:
        print(e)