-- ============================================================
-- Migration: Add composite index on llm_cost_log for daily cost queries.
--
-- The daily cost queries in LlmCostLogMapper filter by (user_id, created_at).
-- This composite index enables index range scans instead of full table scans,
-- especially when combined with the range-based date filter
-- (created_at >= CURDATE() AND created_at < CURDATE() + INTERVAL 1 DAY).
--
-- Note: The original DDL (migration_phase4_sharding_cost.sql) defined
-- INDEX idx_user_date (user_id, create_time). If the column was renamed
-- to created_at, run this migration to ensure the index matches.
-- ============================================================

-- Drop the old index if it exists on the legacy column name
ALTER TABLE llm_cost_log DROP INDEX idx_user_date;

-- Create composite index matching the current column name used in queries
CREATE INDEX idx_llm_cost_log_user_date ON llm_cost_log(user_id, created_at);
