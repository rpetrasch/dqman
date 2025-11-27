-- Transformation: capitalize name and city

CREATE OR REFRESH MATERIALIZED VIEW clean_names_sql
AS
SELECT
    id, initcap(name) as name, initcap(city) as city, status, ingestion_time
FROM live.filtered_names

