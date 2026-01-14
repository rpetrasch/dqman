# EVOLVE-BLOCK-START
import pandas as pd
from scipy.stats import linregress

def forecast_sales(historical_data: pd.DataFrame, predict_day: int) -> pd.DataFrame:
    """
    Input: DataFrame with columns [day_num, region, product, amount]
    Output: DataFrame with [day_num, region, product, amount]
    """
    def _predict_group(g):
        subset_clean = g.dropna(subset=['amount'])
        if len(subset_clean) >= 2:
            slope, intercept, _, _, _ = linregress(subset_clean['day_num'], subset_clean['amount'])
            pred = slope * predict_day + intercept
            return float(pred) if pred >= 0 else 0.0
        elif len(subset_clean) == 1:
            return float(subset_clean['amount'].iloc[0])
        else:
            return 0.0

    pred_df = historical_data.groupby(['region', 'product'], as_index=False).apply(
        lambda g: pd.Series({'amount': _predict_group(g)})
    )
    pred_df['day_num'] = predict_day
    return pred_df[['region', 'product', 'day_num', 'amount']]
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