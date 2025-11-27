
USE CATALOG `workspace`;
USE SCHEMA `default`;

CREATE OR REFRESH MATERIALIZED VIEW raw_names
AS
SELECT
    *,
    -- Capture the current time as a real column
    current_timestamp() as ingestion_time
FROM names
WHERE id IS NOT NULL;
