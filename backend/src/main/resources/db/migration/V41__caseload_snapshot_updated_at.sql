-- V41: Add updated_at column to caseload_snapshot to satisfy SimpleBaseEntity mapping.
-- The column tracks when each row was last upserted by CaseloadSnapshotJob.

ALTER TABLE caseload_snapshot
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
