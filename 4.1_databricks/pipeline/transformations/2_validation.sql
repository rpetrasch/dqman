-- Validation step:
-- a) Filter only acceptable records (id not NULL) for subsequent steps
-- b) Create a dq_report view that lists all data quatlity issues

USE CATALOG `workspace`;
USE SCHEMA `default`;

-- Filter
-- We can use "MATERIALIZED" (save data to disk in the catalog) or "TEMPORARY LIVE" (Only exists inside the pipeline memory during the run)
CREATE OR REFRESH MATERIALIZED VIEW filtered_names
AS
SELECT * FROM live.raw_names
WHERE id IS NOT NULL;

-- DQ validation for columns that are NULL (version 1 for one column only)
/* CREATE OR REFRESH MATERIALIZED VIEW dq_report_names
AS
SELECT
    -- a) Generating a running number: Use monotonically_increasing_id() as it's faster than row_number() in distributed systems
    monotonically_increasing_id() as dq_report_id,
    -- b) The original ID (which will be NULL in this specific case)
    id as names_id,
    -- c) The name of the column that failed
    'id' as column_name,
    -- d) The value that caused the problem (Cast to string to be safe)
    cast(id as string) as value,
    -- e) The description of the problem
    'Field cannot be NULL' as problem
FROM live.names_raw
WHERE id IS NULL;
*/


-- DQ validation for columns that are NULL (version 2 with stack) or duplicates (id)
CREATE OR REFRESH MATERIALIZED VIEW dq_report_names
AS
SELECT
    monotonically_increasing_id() as dq_report_id,
    names_id,
    column_name,
    value,
    problem
FROM (
    -- DQ check logic 1: Check for NULLs
    SELECT
        id as names_id,
        col_name as column_name,
        col_value as value,
        'Field cannot be NULL' as problem
    FROM (
        SELECT
            id,
            stack(2, 'id', cast(id as string), 'name', name) as (col_name, col_value)
        FROM live.raw_names
    )
    WHERE col_value IS NULL
    UNION ALL
    -- DQ check logic 2: Check for Duplicates (Window Function)
    SELECT
        id as names_id,
        'id' as column_name,
        cast(id as string) as value,
        'Duplicate ID detected' as problem
    FROM live.raw_names
    -- Qualify filters after the window function runs: keeps all rows where the Count of that ID is greater than 1
    QUALIFY COUNT(id) OVER (PARTITION BY id) > 1
);
