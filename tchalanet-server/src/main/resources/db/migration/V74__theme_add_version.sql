-- Add missing 'version' column expected by @Version in AuditableEntity for table theme
ALTER TABLE IF EXISTS theme
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

-- Ensure existing rows have a non-null value (default applied above)
UPDATE theme SET version = COALESCE(version, 0) WHERE TRUE;

