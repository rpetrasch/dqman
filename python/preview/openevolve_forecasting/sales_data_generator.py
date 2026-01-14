import logging
import os
import matplotlib
import pandas as pd
import numpy as np
from matplotlib import pyplot as plt


def get_amount(day, product, region):
    """The 'true' signal function"""
    base_amount = 0
    time_trend = 0
    if product == 'a' and region == 'A':
        base_amount = 50
    if product == 'a' and region == 'B':
        base_amount = 40

    if product == 'b' and region == 'A':
        time_trend = -day * 0.2
        base_amount = 120
    if product == 'b' and region == 'B':
        time_trend = day * 0.2
        base_amount = 50

    if product == 'c' and region == 'A':
        time_trend = day * 0.3  # Sales go up over time
        base_amount = 20
    if product == 'c' and region == 'B':
        time_trend = day * 0.5  # Sales go up over time
        base_amount = 20

    if product == 'd' and region == 'A':
        base_amount = 10
    if product == 'd' and region == 'B':
        base_amount = 70
        time_trend = -day * 0.01  # Sales go down a little over time

    # Add some random noise
    noise = np.random.normal(0, 3)
    return base_amount + time_trend + noise

def generate_data():
    """
    Generates the imbalanced dataset.
    The TRUE SIGNAL: Sales (amount) increases with time (day_num),
    but *decreases* for product 'b'. Region has NO effect.
    """
    logging.info("*** Generating data points...")
    data = []

    # Base prices for products
    price_map = {'a': 10.0, 'b': 20.0, 'c': 15.0, 'd': 10.0}

    # --- Region A  ---
    # Fix: Iterate day_num from 0 to 299
    for day_num in range(150):
        for prod in ['a', 'b', 'c']:
            data.append({
                'region': 'A',
                'product': prod,
                'price': price_map[prod] + np.random.normal(0, 1),
                'day_num': day_num,
                'amount': get_amount(day_num, prod, 'A')
            })

    # --- Region B ---
    for day_num in range(0, 120, 2):
        for prod in ['a', 'b', 'c']:
            data.append({
                'region': 'B',
                'product': prod,
                'price': price_map[prod] + np.random.normal(0, 1),
                'day_num': day_num,
                'amount': get_amount(day_num, prod, 'B')
            })
    
    # --- Region B product d ---
    prod = 'd'
    day = 20
    data.append({
        'region': 'B',
        'product': prod,
        'price': price_map[prod] + np.random.normal(0, 1),
        'day_num': day,
        'amount': get_amount(day, prod, 'B')
    })
    return pd.DataFrame(data)

def save_date(df):
    # sort product (start with prod. (first d,c,b, last a) and region (first B, then A), and day_num
    df = df.sort_values(by=['product', 'region', 'day_num'])
    df.to_csv('sales_data.csv', index=False)

def read_data(delete_first=False):
    # Force regeneration to fix bad data in CSV if it exists
    if delete_first and os.path.exists('sales_data.csv'):
        os.remove('sales_data.csv')
    
    generate_data().to_csv('sales_data.csv', index=False)
    return pd.read_csv('sales_data.csv')

if __name__ == "__main__":
    data = read_data()
    # Plot the data with matplotlib: line for each product and region
    fig, ax = plt.subplots(figsize=(12, 8))
    
    # Iterate through all combinations of region and product
    for region in ['A', 'B']:
        for product in ['a', 'b', 'c', 'd']:
            # Filter data for the specific region and product
            subset = data[(data['region'] == region) & (data['product'] == product)]
            
            # Sort by day just in case
            subset = subset.sort_values('day_num')

            # Only plot if we have data for this combination
            if not subset.empty:
                # Use day_num as x and amount as y
                # Label matches "Region X - Product Y"
                ax.plot(subset['day_num'], subset['amount'], label=f'{region} - {product}')

    ax.set_xlabel('Day Number')
    ax.set_ylabel('Sales Amount')
    ax.set_title('Sales Amount over Time by Region and Product')
    ax.legend()
    ax.grid(True)
    
    plt.show()