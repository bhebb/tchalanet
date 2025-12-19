-- V2: core tenant & address (post-extensions)
-- Prérequis: V1__extensions_and_functions.sql (citext, pgcrypto, set_updated_at, etc.)

-- 1) ADDRESS
CREATE TABLE IF NOT EXISTS address (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    line1 text NOT NULL,
    line2 text,
    city text NOT NULL,
    region text,
    country text NOT NULL,
    postal_code text,
    latitude double precision,
    longitude double precision
);

-- 2) TENANT (no DROP CASCADE in Flyway)
CREATE TABLE IF NOT EXISTS tenant (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    code varchar(64) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    timezone varchar(64) NOT NULL,
    currency varchar(3) NOT NULL DEFAULT 'USD',

    status varchar(32) NOT NULL DEFAULT 'DRAFT', -- DRAFT|ACTIVE|SUSPENDED|REJECTED|ARCHIVED
    type varchar(32) NOT NULL DEFAULT 'BORLETTE',

    address_id uuid REFERENCES address(id),

    logo_url text,
    brand_color_primary text,
    brand_color_secondary text,
    theme_preset_id uuid,
    active_theme_id uuid,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

ALTER TABLE tenant
    ADD CONSTRAINT chk_tenant_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'REJECTED', 'ARCHIVED'));

CREATE INDEX IF NOT EXISTS ix_tenant_status ON tenant (status);
CREATE INDEX IF NOT EXISTS ix_tenant_code ON tenant (code);

-- trigger MAJ updated_at (fonction set_updated_at fournie en V1)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_tenant_updated_at'
  ) THEN
    CREATE TRIGGER trg_tenant_updated_at
      BEFORE UPDATE ON tenant
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

