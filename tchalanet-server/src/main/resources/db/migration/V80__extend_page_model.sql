-- V80: extend existing page_model table to match application entity
-- Add missing columns expected by the application (logical_id, model, published_at, schema_version, scope, slug, status, template_id)

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS logical_id text;

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS model jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS published_at timestamptz;

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS schema_version integer NOT NULL DEFAULT 1;

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS scope varchar(64) NOT NULL DEFAULT 'public';

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS slug text;

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS status varchar(32) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS template_id uuid;

ALTER TABLE page_model
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

-- Backfill from existing columns where appropriate
UPDATE page_model SET logical_id = code WHERE logical_id IS NULL AND code IS NOT NULL;
UPDATE page_model SET model = schema WHERE (model IS NULL OR model = '{}'::jsonb) AND schema IS NOT NULL;
UPDATE page_model SET slug = code WHERE slug IS NULL AND code IS NOT NULL;

-- Ensure trigger exists (function set_updated_at expected from V1)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_page_model_updated_at') THEN
      CREATE TRIGGER trg_page_model_updated_at BEFORE UPDATE ON page_model FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
  END IF;
END $$;

-- Add useful index for lookups by tenant + logical_id
CREATE INDEX IF NOT EXISTS ix_page_model_tenant_logical_id ON page_model (tenant_id, logical_id) WHERE deleted_at IS NULL;

-- Note: do not add non-partial unique constraints here to preserve soft-delete semantics.

