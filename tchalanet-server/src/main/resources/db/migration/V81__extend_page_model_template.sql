-- V81: extend page_model_template to match PageModelTemplateEntity

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS logical_id text;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS label text;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS description text;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS schema_version integer NOT NULL DEFAULT 1;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS model jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS is_default boolean NOT NULL DEFAULT false;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS is_system boolean NOT NULL DEFAULT false;

ALTER TABLE page_model_template
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

-- Backfill from existing columns where appropriate
UPDATE page_model_template SET logical_id = code WHERE logical_id IS NULL AND code IS NOT NULL;
UPDATE page_model_template SET label = name WHERE label IS NULL AND name IS NOT NULL;
UPDATE page_model_template SET model = schema WHERE (model IS NULL OR model = '{}'::jsonb) AND schema IS NOT NULL;

-- Ensure trigger exists if utility function is present
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_page_model_template_updated_at') THEN
      CREATE TRIGGER trg_page_model_template_updated_at BEFORE UPDATE ON page_model_template FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
  END IF;
END $$;

-- Add index on logical_id for lookups
CREATE INDEX IF NOT EXISTS ix_page_model_template_logical_id ON page_model_template (logical_id) WHERE deleted_at IS NULL;

