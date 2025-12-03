-- Initialize database tables

CREATE TABLE IF NOT EXISTS sales_validated (
    sid INTEGER PRIMARY KEY,
    cid INTEGER NOT NULL,
    date TIMESTAMP NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sales_anomalies (
    id SERIAL PRIMARY KEY,
    sid INTEGER NOT NULL,
    cid INTEGER NOT NULL,
    date TIMESTAMP NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reconstruction_error DECIMAL(12, 6) NOT NULL,
    threshold DECIMAL(12, 6) NOT NULL,
    anomaly_score DECIMAL(12, 6) NOT NULL,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customers (
    cid INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    status VARCHAR(50)
);

CREATE INDEX idx_sales_validated_cid ON sales_validated(cid);
CREATE INDEX idx_sales_anomalies_cid ON sales_anomalies(cid);
CREATE INDEX idx_sales_anomalies_date ON sales_anomalies(date);