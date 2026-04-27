-- Ensure logical_id is unique among non-deleted rows to avoid race conditions
-- and provide a lower-case index to speed case-insensitive LIKE searches.

DO $$
BEGIN
  -- create unique partial index if not exists
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relname = 'ux_page_model_template_logical_id_not_deleted'
  ) THEN
    CREATE UNIQUE INDEX ux_page_model_template_logical_id_not_deleted
      ON page_model_template (logical_id)
      WHERE deleted_at IS NULL;
  END IF;

  -- create functional index for case-insensitive searches (improves LIKE lower(...))
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relname = 'ix_page_model_template_logical_id_lower'
  ) THEN
    CREATE INDEX ix_page_model_template_logical_id_lower
      ON page_model_template (lower(logical_id))
      WHERE deleted_at IS NULL;
  END IF;
END $$;
