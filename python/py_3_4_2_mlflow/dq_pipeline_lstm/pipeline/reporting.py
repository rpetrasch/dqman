"""
Anomaly reporting
"""


def generate_anomaly_report(anomalies):
    """Generate human-readable anomaly report"""
    print("\n" + "=" * 60)
    print("ANOMALY REPORT")
    print("=" * 60)

    if len(anomalies) == 0:
        print("✓ No anomalies detected!")
        return

    for idx, row in anomalies.iterrows():
        print(f"\n⚠️  Anomaly #{idx + 1}")
        print(f"   Customer ID: {row['cid']}")
        print(f"   Sale ID: {row['sid']}")
        print(f"   Date: {row['date']}")
        print(f"   Amount: ${row['amount']:,.2f}")
        print(f"   Reconstruction Error: {row['reconstruction_error']:.4f}")
        print(f"   Threshold: {row['threshold']:.4f}")
        print(f"   Anomaly Score: {row['anomaly_score']:.2%} over threshold")

