-- Transformation: deduplicate (id)

CREATE OR REFRESH MATERIALIZED VIEW deduplicated_names
AS
SELECT * FROM live.clean_names_sql
-- Group by ID, Order by ingestion time to keep the newest
QUALIFY ROW_NUMBER() OVER (PARTITION BY id ORDER BY ingestion_time DESC) = 1;
