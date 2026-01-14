# EVOLVE-BLOCK-START
import pandas as pd
import numpy as np
import scipy
import scipy.stats
from pandas import DataFrame

def forecast_sales(historical_data: pd.DataFrame, predict_day: int) -> DataFrame:
    """
    Input: DataFrame with columns [day_num, region, product, amount]
    Output: DataFrame with [day_num, region, product, amount]
    """
    
    predictions = []
    
    regions = sorted(historical_data["region"].unique())
    products = sorted(historical_data["product"].unique())

    for region in regions:
        for product in products:
            subset = historical_data[(historical_data['region'] == region) & (historical_data['product'] == product)]
            
            if not subset.empty:
                # DUMB PREDICTION: Just the mean
                predicted_amount = subset['amount'].mean()
            else:
                predicted_amount = 0.0
                
            predictions.append({
                'region': region,
                'product': product,
                'day_num': predict_day,
                'amount': float(predicted_amount)
            })

    return pd.DataFrame(predictions)
# EVOLVE-BLOCK-END

if __name__ == "__main__":
    from sales_data_generator import read_data
    try:
        data = read_data()
        # Test runs
        last_day = int(data['day_num'].max())
        pred = forecast_sales(data, last_day + 20)
        print(pred.head())
    except Exception as e:
        print(e)