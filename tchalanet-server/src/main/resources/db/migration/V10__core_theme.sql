-- V9: core theme
CREATE TABLE IF NOT EXISTS theme (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  version bigint NOT NULL DEFAULT 0,
  definition jsonb NOT NULL DEFAULT '{}'::jsonb,
  active boolean NOT NULL DEFAULT true,
  base_preset_id varchar(128) DEFAULT '',
  label varchar(160) DEFAULT '',
  mode varchar(10) NOT NULL DEFAULT 'LIGHT',
  density smallint NOT NULL DEFAULT 0,
  palette_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  tokens_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  css_vars_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  status varchar(20) NOT NULL DEFAULT 'DRAFT',
  theme_version int4 NOT NULL DEFAULT 1,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  UNIQUE (code)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_theme_updated_at') THEN
    CREATE TRIGGER trg_theme_updated_at BEFORE UPDATE ON theme FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;


-- Tenant-specific themes table (mapped from TenantThemeJpaEntity)
CREATE TABLE IF NOT EXISTS tenant_theme (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id), -- RLS will scope rows; tenant_id kept for admin writes
  preset_code varchar(128) NOT NULL,
  metadata jsonb DEFAULT '{}'::jsonb,
  is_default boolean NOT NULL DEFAULT false,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_tenant_theme_updated_at') THEN
    CREATE TRIGGER trg_tenant_theme_updated_at BEFORE UPDATE ON tenant_theme FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- Optional unique constraint: one preset_code per tenant
CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_theme_tenant_preset ON tenant_theme (tenant_id, preset_code) WHERE deleted_at IS NULL;

