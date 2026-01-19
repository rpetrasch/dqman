"""
Simple file utility functions for data loading and generation.
"""
from matplotlib import pyplot as plt
from .config_loader import default_config
import pandas as pd
import os
import numpy as np

# Get config data
data_cfg = default_config.data_config
data_dir = data_cfg.get('directory', '.')
filename = data_cfg.get('filename', 'sales_data.csv')
data_path = os.path.join(data_dir, filename)

def read_csv_file(header_only=False):
    """
    Read CSV file with optional header-only option.
    :param header_only: If True, only read the header.
    :return: list of header column names (if header_only) or Pandas DataFrame (complete file content)
    """
    if header_only: # Read only header
        df_header = pd.read_csv(str(data_path), nrows=0)
        csv_columns = list(df_header.columns)
        return csv_columns
    else:
        df = pd.read_csv(str(data_path))
        return df


def generate_sales_data(plot=False):
    """
    Generation of sales data for 20 days.
    :param plot if True plots the data for all products.
    :return: Pandas DataFrame with sales data.
    """
    # Days
    days = np.arange(1, 21)
    # Product A: Linear (Start 5, End 25)
    sales_a = np.linspace(5, 25, 20)
    # Product B: Non-linear (Start 5, Peak 20, End 5)
    h = 10.5
    k = 20
    a_param = -15 / (9.5 ** 2)
    sales_b = a_param * (days - h) ** 2 + k
    # Product C: Asymptotic-like curve (Start 5, End 25)
    # Modeled as an exponential approach: y = Start + Delta * (1 - e^(-k*t)) / Normalization
    decay_rate = 0.2
    normalization_factor = 1 - np.exp(-decay_rate * (20 - 1))
    sales_c = 5 + (25 - 5) * (1 - np.exp(-decay_rate * (days - 1))) / normalization_factor
    # Combine and Save
    df_a = pd.DataFrame({'day_num': days, 'product': 'a', 'sales_amount': sales_a})
    df_b = pd.DataFrame({'day_num': days, 'product': 'b', 'sales_amount': sales_b})
    df_c = pd.DataFrame({'day_num': days, 'product': 'c', 'sales_amount': sales_c})
    df = pd.concat([df_a, df_b, df_c], ignore_index=True)
    df['sales_amount'] = df['sales_amount'].round(2)
    df.to_csv(str(data_path), index=False)
    if plot:
        # plot data for all products
        plt.figure(figsize=(10, 6))

        plt.title('Sales Data')
        plt.xlabel('Day Number')
        plt.ylabel('Sales Amount')
        plt.grid(True)
        plt.ylim(0, 30)
        plt.xlim(0, 20)
        plt.xticks(days)
        plt.plot(days, sales_a, label='Product A')
        plt.plot(days, sales_b, label='Product B')
        plt.plot(days, sales_c, label='Product C')
        plt.yticks(np.arange(0, 30, 5))
        plt.legend()
        plt.tight_layout()
        plt.show()
        plt.close()
    return df
