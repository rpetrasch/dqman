"""
Advanced SHAP visualization module
"""

import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import shap
import plotly.graph_objects as go
from plotly.subplots import make_subplots


class AnomalyVisualizer:
    """Advanced visualizations for anomaly detection and explainability"""

    def __init__(self, style='seaborn-v0_8-darkgrid'):
        """Initialize visualizer with plot style"""
        plt.style.use('default')
        sns.set_palette("husl")

    def plot_reconstruction_error(self, original, reconstructed, title="Reconstruction Comparison"):
        """
        Plot original vs reconstructed sequence

        Args:
            original: Original sequence (1D array)
            reconstructed: Reconstructed sequence (1D array)
            title: Plot title
        """
        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 8))

        # Time series plot
        x = np.arange(len(original))
        ax1.plot(x, original, 'b-', label='Original', linewidth=2)
        ax1.plot(x, reconstructed, 'r--', label='Reconstructed', linewidth=2)
        ax1.fill_between(x, original, reconstructed, alpha=0.3, color='gray')
        ax1.set_xlabel('Time Step')
        ax1.set_ylabel('Value')
        ax1.set_title(title)
        ax1.legend()
        ax1.grid(True, alpha=0.3)

        # Error plot
        error = np.abs(original - reconstructed)
        ax2.bar(x, error, color='red', alpha=0.6)
        ax2.set_xlabel('Time Step')
        ax2.set_ylabel('Absolute Error')
        ax2.set_title('Reconstruction Error per Timestep')
        ax2.grid(True, alpha=0.3)

        plt.tight_layout()
        return fig

    def plot_shap_waterfall(self, shap_values, feature_names, max_display=10):
        """
        Create SHAP waterfall plot showing feature contributions

        Args:
            shap_values: SHAP values array
            feature_names: List of feature names
            max_display: Maximum features to display
        """
        # Create explanation object
        explanation = shap.Explanation(
            values=shap_values[0],
            base_values=0,
            feature_names=feature_names
        )

        # Create waterfall plot
        fig = plt.figure(figsize=(10, 6))
        shap.plots.waterfall(explanation, max_display=max_display, show=False)
        plt.title("SHAP Waterfall Plot: Feature Contributions to Anomaly")
        plt.tight_layout()

        return fig

    def plot_shap_force(self, shap_values, base_value, feature_names, feature_values):
        """
        Create SHAP force plot

        Args:
            shap_values: SHAP values for single prediction
            base_value: Base/expected value
            feature_names: List of feature names
            feature_values: Actual feature values
        """
        # Create force plot
        fig = shap.force_plot(
            base_value,
            shap_values[0],
            feature_values,
            feature_names=feature_names,
            matplotlib=True,
            show=False
        )

        return fig

    def plot_anomaly_timeline(self, sales_data, anomalies, customer_id):
        """
        Interactive timeline showing sales and detected anomalies

        Args:
            sales_data: DataFrame with sales data
            anomalies: DataFrame with detected anomalies
            customer_id: Customer ID to visualize
        """
        customer_sales = sales_data[sales_data['cid'] == customer_id].sort_values('date')
        customer_anomalies = anomalies[anomalies['cid'] == customer_id]

        fig = go.Figure()

        # Normal sales
        fig.add_trace(go.Scatter(
            x=customer_sales['date'],
            y=customer_sales['amount'],
            mode='lines+markers',
            name='Normal Sales',
            line=dict(color='blue', width=2),
            marker=dict(size=6)
        ))

        # Anomalies
        if len(customer_anomalies) > 0:
            fig.add_trace(go.Scatter(
                x=customer_anomalies['date'],
                y=customer_anomalies['amount'],
                mode='markers',
                name='Anomalies',
                marker=dict(
                    size=15,
                    color='red',
                    symbol='x',
                    line=dict(width=2)
                )
            ))

        fig.update_layout(
            title=f'Sales Timeline with Anomalies - Customer {customer_id}',
            xaxis_title='Date',
            yaxis_title='Amount ($)',
            hovermode='x unified',
            template='plotly_white',
            height=500
        )

        return fig

    def plot_anomaly_heatmap(self, anomalies):
        """
        Heatmap showing anomaly scores by customer and date

        Args:
            anomalies: DataFrame with anomaly data
        """
        if len(anomalies) == 0:
            print("No anomalies to visualize")
            return None

        # Pivot table
        pivot_data = anomalies.pivot_table(
            values='anomaly_score',
            index='cid',
            columns=anomalies['date'].dt.date,
            aggfunc='mean'
        )

        fig, ax = plt.subplots(figsize=(14, 6))
        sns.heatmap(
            pivot_data,
            cmap='YlOrRd',
            annot=True,
            fmt='.2f',
            cbar_kws={'label': 'Anomaly Score'},
            ax=ax
        )
        ax.set_title('Anomaly Score Heatmap by Customer and Date')
        ax.set_xlabel('Date')
        ax.set_ylabel('Customer ID')
        plt.tight_layout()

        return fig

    def plot_reconstruction_error_distribution(self, errors, threshold):
        """
        Distribution of reconstruction errors with threshold

        Args:
            errors: Array of reconstruction errors
            threshold: Anomaly threshold
        """
        fig, ax = plt.subplots(figsize=(10, 6))

        # Histogram
        ax.hist(errors, bins=50, alpha=0.7, color='blue', edgecolor='black')

        # Threshold line
        ax.axvline(threshold, color='red', linestyle='--', linewidth=2,
                   label=f'Threshold: {threshold:.4f}')

        # Statistics
        mean_error = np.mean(errors)
        std_error = np.std(errors)
        ax.axvline(mean_error, color='green', linestyle='--', linewidth=2,
                   label=f'Mean: {mean_error:.4f}')

        ax.set_xlabel('Reconstruction Error')
        ax.set_ylabel('Frequency')
        ax.set_title('Distribution of Reconstruction Errors')
        ax.legend()
        ax.grid(True, alpha=0.3)

        plt.tight_layout()
        return fig

    def create_dashboard(self, customer_sales, anomalies, model, scaler, config):
        """
        Create comprehensive dashboard for a customer

        Args:
            customer_sales: Sales data for customer
            anomalies: Detected anomalies for customer
            model: Trained LSTM model
            scaler: Data scaler
            config: Configuration object
        """
        fig = make_subplots(
            rows=2, cols=2,
            subplot_titles=(
                'Sales Timeline',
                'Reconstruction Error Distribution',
                'Anomaly Scores Over Time',
                'Top Anomalous Transactions'
            ),
            specs=[
                [{"type": "scatter"}, {"type": "histogram"}],
                [{"type": "scatter"}, {"type": "bar"}]
            ]
        )

        # Sales timeline
        fig.add_trace(
            go.Scatter(
                x=customer_sales['date'],
                y=customer_sales['amount'],
                mode='lines+markers',
                name='Sales'
            ),
            row=1, col=1
        )

        # Add anomalies to timeline
        if len(anomalies) > 0:
            fig.add_trace(
                go.Scatter(
                    x=anomalies['date'],
                    y=anomalies['amount'],
                    mode='markers',
                    marker=dict(size=12, color='red', symbol='x'),
                    name='Anomalies'
                ),
                row=1, col=1
            )

        # Reconstruction error distribution
        amounts = customer_sales['amount'].values.reshape(-1, 1)
        amounts_scaled = scaler.transform(amounts)

        errors = []
        for i in range(len(amounts_scaled) - config.WINDOW_SIZE + 1):
            seq = amounts_scaled[i:i + config.WINDOW_SIZE].reshape(1, config.WINDOW_SIZE, 1)
            recon = model.predict(seq, verbose=0)
            error = np.mean(np.power(seq - recon, 2))
            errors.append(error)

        fig.add_trace(
            go.Histogram(x=errors, nbinsx=30, name='Error Distribution'),
            row=1, col=2
        )

        # Anomaly scores over time
        if len(anomalies) > 0:
            fig.add_trace(
                go.Scatter(
                    x=anomalies['date'],
                    y=anomalies['anomaly_score'],
                    mode='lines+markers',
                    name='Anomaly Score'
                ),
                row=2, col=1
            )

        # Top anomalous transactions
        if len(anomalies) > 0:
            top_anomalies = anomalies.nlargest(5, 'anomaly_score')
            fig.add_trace(
                go.Bar(
                    x=top_anomalies['date'].astype(str),
                    y=top_anomalies['anomaly_score'],
                    name='Top Anomalies'
                ),
                row=2, col=2
            )

        fig.update_layout(
            height=800,
            showlegend=True,
            title_text=f"Customer Analytics Dashboard"
        )

        return fig
