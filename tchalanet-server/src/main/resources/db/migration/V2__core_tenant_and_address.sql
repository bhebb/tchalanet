-- =====================================================================
-- DEV RECREATE SCRIPT (single file)
-- core tenant + core address (tenant-scoped, RLS) + MVP rules
-- - Ports use typed IDs in code; DB uses UUID
-- - 1 active address per tenant (deleted=false)
-- - Dedup by (tenant_id, normalized_key) on active rows
-- - NO FK cycle: address.tenant_id is NOT FK -> tenant(id)
-- =====================================================================

-- ---------------------------
-- 2) DROP (dev only)
-- ---------------------------
-- NOTE: For dev rebuild only. In Flyway you should not do this.
DROP TABLE IF EXISTS address CASCADE;
DROP TABLE IF EXISTS tenant CASCADE;

-- ---------------------------
-- 3) TENANT (create first, without address FK cycle)
-- ---------------------------
CREATE TABLE tenant (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        version bigint NOT NULL DEFAULT 0,

                        code varchar(64) NOT NULL UNIQUE,
                        name varchar(255) NOT NULL,
                        timezone varchar(64) NOT NULL,
                        currency varchar(3) NOT NULL DEFAULT 'USD',

                        status varchar(32) NOT NULL DEFAULT 'DRAFT', -- DRAFT|ACTIVE|SUSPENDED|REJECTED|ARCHIVED
                        type varchar(32) NOT NULL DEFAULT 'BORLETTE',

    -- address FK will be added AFTER address table exists
                        address_id uuid,

    -- (legacy/transition fields - keep if you still use them)
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

CREATE INDEX ix_tenant_status ON tenant(status);
CREATE INDEX ix_tenant_code ON tenant(code);

-- updated_at trigger
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_tenant_updated_at') THEN
CREATE TRIGGER trg_tenant_updated_at
    BEFORE UPDATE ON tenant
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

-- ---------------------------
-- 4) ADDRESS (tenant-scoped, RLS, NO FK -> tenant to avoid cycle)
-- ---------------------------
CREATE TABLE address (
                         id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                         tenant_id uuid NOT NULL, -- tenant scope for RLS; intentionally NOT a FK to tenant(id)

                         line1 varchar(256) NOT NULL,
                         line2 varchar(256),
                         city varchar(128) NOT NULL,
                         region varchar(128),
                         country char(2) NOT NULL,
                         postal_code varchar(16),

                         normalized_key varchar(64) NOT NULL,

                         deleted boolean NOT NULL DEFAULT false,
                         deleted_at timestamptz,

                         version bigint NOT NULL DEFAULT 1,

                         created_at timestamptz NOT NULL DEFAULT now(),
                         created_by uuid,
                         updated_at timestamptz NOT NULL DEFAULT now(),
                         updated_by uuid
);

-- updated_at trigger
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_address_updated_at') THEN
CREATE TRIGGER trg_address_updated_at
    BEFORE UPDATE ON address
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

-- ---------------------------
-- 5) TENANT -> ADDRESS FK (one-way, no cycle)
-- ---------------------------
ALTER TABLE tenant
    ADD CONSTRAINT fk_tenant_address
        FOREIGN KEY (address_id) REFERENCES address(id);

-- ---------------------------
-- 6) RLS (address only)
-- ---------------------------
ALTER TABLE address ENABLE ROW LEVEL SECURITY;

-- Policy: tenant isolation (requires app.tenant_id set per request)
-- Using current_setting(..., true) avoids errors if unset, but then policy is false.
DROP POLICY IF EXISTS address_tenant_isolation ON address;

CREATE POLICY address_tenant_isolation
  ON address
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid)
  WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

-- ---------------------------
-- 7) Indexes (MVP rules + dedup)
-- ---------------------------

-- Basic tenant lookups
CREATE INDEX idx_address_tenant_id ON address(tenant_id);

-- Dedup on active rows (allows re-create after soft-delete)
CREATE UNIQUE INDEX ux_address_tenant_normalized_active
    ON address(tenant_id, normalized_key)
    WHERE deleted = false;

-- MVP: 1 active address per tenant
CREATE UNIQUE INDEX ux_address_one_active_per_tenant
    ON address(tenant_id)
    WHERE deleted = false;

-- Useful filter index
CREATE INDEX idx_address_tenant_not_deleted
    ON address(tenant_id)
    WHERE deleted = false;

-- Optional: speed "get active address id for tenant" queries
CREATE INDEX ix_address_active_by_tenant
    ON address(tenant_id, updated_at DESC)
    WHERE deleted = false;

-- =====================================================================
-- Notes:
-- 1) This script assumes your app sets `SET LOCAL app.tenant_id = '<uuid>'`
--    (or equivalent) for each request/tx that touches tenant-scoped tables.
-- 2) We intentionally avoid FK address.tenant_id -> tenant.id to break the cycle.
-- 3) With ux_address_one_active_per_tenant, tenants cannot have 2 active addresses.
-- =====================================================================
