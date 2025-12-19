-- V11: feature page model
CREATE TABLE IF NOT EXISTS page_model (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  code varchar(128) NOT NULL,
  name varchar(255) NOT NULL,
  schema jsonb NOT NULL DEFAULT '{}'::jsonb,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS page_model_template (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  name varchar(255) NOT NULL,
  schema jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
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

