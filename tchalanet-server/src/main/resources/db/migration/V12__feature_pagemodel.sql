-- V12: feature page model
-- level + is_system ajoutes (manquaient de la consolidation V80/V81/V82)
-- UNIQUE PUBLISHED par (tenant, logicalId) ajoute sur page_model
-- Ordre : page_model_template en premier (FK depuis page_model)

CREATE TABLE IF NOT EXISTS page_model_template (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  logical_id text,
  name varchar(255) NOT NULL,
  label text,
  description text,
  schema jsonb NOT NULL DEFAULT '{}'::jsonb,
  model jsonb NOT NULL DEFAULT '{}'::jsonb,
  schema_version integer NOT NULL DEFAULT 1,
  is_default boolean NOT NULL DEFAULT false,
  is_system boolean NOT NULL DEFAULT false,
  level varchar(16) NOT NULL DEFAULT 'GLOBAL',
  tenant_id uuid,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS page_model (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  code varchar(128) NOT NULL,
  logical_id text,
  name varchar(255) NOT NULL,
  schema jsonb NOT NULL DEFAULT '{}'::jsonb,
  model jsonb NOT NULL DEFAULT '{}'::jsonb,
  published_at timestamptz,
  schema_version integer NOT NULL DEFAULT 1,
  scope varchar(64) NOT NULL DEFAULT 'public',
  slug text,
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  template_id uuid REFERENCES page_model_template(id),
  version bigint NOT NULL DEFAULT 0,
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
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_page_model_updated_at') THEN
    CREATE TRIGGER trg_page_model_updated_at BEFORE UPDATE ON page_model FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_page_model_template_updated_at') THEN
    CREATE TRIGGER trg_page_model_template_updated_at BEFORE UPDATE ON page_model_template FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

ALTER TABLE page_model_template
    ADD CONSTRAINT ck_pmt_level_target
        CHECK (
            (level = 'GLOBAL' AND tenant_id IS NULL)
                OR
            (level = 'TENANT' AND tenant_id IS NOT NULL)
            );

-- Indexes
CREATE INDEX IF NOT EXISTS ix_page_model_template_logical_id ON page_model_template (logical_id) WHERE deleted_at IS NULL;

-- Garantit 1 seul PUBLISHED par (tenant, logicalId) -- verrou DB complementaire a PublishPolicy
CREATE UNIQUE INDEX IF NOT EXISTS ux_page_model_published_per_tenant_logical_id
  ON page_model (tenant_id, logical_id)
  WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

