-- V22: core/tenantconfig - Tenant registry and lifecycle management
-- Table: tenant (platform-wide registry, no RLS)
-- Owns: tenant creation, lifecycle transitions, field updates
-- Related: catalog/tenant reads via rawDataSource (bypass ok)

-- =============================================================================
-- Part 1: core/tenantconfig (tenant) - PLATFORM TENANT REGISTRY
-- =============================================================================

CREATE TABLE IF NOT EXISTS tenant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Identity
  code varchar(64) NOT NULL UNIQUE,          -- normalized (lowercase, trim), functional key
  name varchar(255) NOT NULL,                 -- human-readable name
  type varchar(32) NOT NULL DEFAULT 'PERSONAL', -- COMMERCIAL|PERSONAL

  -- Locale & configuration
  timezone varchar(64) NOT NULL DEFAULT 'UTC',
  currency varchar(3) NOT NULL DEFAULT 'USD',

  -- Lifecycle status
  status varchar(32) NOT NULL DEFAULT 'DRAFT', -- DRAFT|ACTIVE|SUSPENDED|REJECTED|ARCHIVED

  -- Optional references
  address_id uuid,                            -- reference to core.address (no FK, soft reference)
  active_theme_id uuid,                       -- reference to theme preset (typed ID in domain)

  -- Audit columns
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_by uuid,
  version bigint NOT NULL DEFAULT 0
);

-- Indexes for tenant
CREATE UNIQUE INDEX IF NOT EXISTS idx_tenant_code_lower
  ON tenant(LOWER(code));
CREATE INDEX IF NOT EXISTS idx_tenant_status
  ON tenant(status);
CREATE INDEX IF NOT EXISTS idx_tenant_type
  ON tenant(type);
CREATE INDEX IF NOT EXISTS idx_tenant_created_at
  ON tenant(created_at);

-- =============================================================================
-- Part 2: Trigger (updated_at)
-- =============================================================================

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_tenant_updated_at') THEN
    CREATE TRIGGER trg_tenant_updated_at
      BEFORE UPDATE ON tenant
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- =============================================================================
-- Notes:
-- - No RLS on tenant table (platform-wide registry, access controlled at API layer)
-- - code is UNIQUE (case-insensitive via LOWER() function in index)
-- - address_id is a soft reference (no FK, allows address to be optional)
-- - active_theme_id is a soft reference (no FK, allows theme to be optional/removed)
-- - version column for optimistic locking
-- - Audit columns (created_by, updated_by) for traceability
-- =============================================================================
