CREATE TABLE customer (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sales (
    id SERIAL PRIMARY KEY,
    customer_id INT REFERENCES customer(id),
    amount DECIMAL(10, 2),
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO customer (name, email) VALUES
('Alice Smith', 'alice@example.com'),
('Bob Jones', 'bob@example.com'),
('Charlie Blue', 'charlie@example.com'),
('Max Wright', 'max(at)example.com'),
('Mr. White', '31905');
('Mrs. Green', null);


INSERT INTO sales (customer_id, amount, sale_date) VALUES
(1, 100.50, '2023-01-01'),
(1, 25.00, '2023-01-02'),
(1, -25.00, '2023-01-03'),
(1, 0.00001, '2023-01-04'),
(1, 25.00, '2023-01-05'),
(2, 200.00, '2023-01-06'),
(3, 50.75, '2023-01-07'),
(3, 120.00, '2023-01-08'),
(3, 120.00, '2023-01-09'),
(3, 120.00, '2023-01-10'),
(3, 120000.00, '2023-01-11');
