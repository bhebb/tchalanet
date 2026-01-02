-- V9: core theme
CREATE TABLE IF NOT EXISTS theme (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id), -- NULL = global
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  version bigint NOT NULL DEFAULT 0,
  definition jsonb NOT NULL DEFAULT '{}'::jsonb,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  UNIQUE (tenant_id, code)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_theme_updated_at') THEN
    CREATE TRIGGER trg_theme_updated_at BEFORE UPDATE ON theme FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;
