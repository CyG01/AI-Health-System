-- ============================================================
-- Migration: Add composite index on llm_cost_log for daily cost queries.
--
-- The daily cost queries in LlmCostLogMapper filter by (user_id, create_time).
-- This composite index enables index range scans instead of full table scans,
-- especially when combined with the range-based date filter
-- (create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY).
--
-- Note: The original DDL (migration_phase4_sharding_cost.sql) defined
-- INDEX idx_user_date (user_id, create_time).
-- ============================================================

-- Drop the old index if it exists
ALTER TABLE llm_cost_log DROP INDEX idx_user_date;

-- Create composite index matching the actual column name used in queries
CREATE INDEX idx_llm_cost_log_user_date ON llm_cost_log(user_id, create_time);
