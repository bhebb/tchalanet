-- Create core.address table (tenant-scoped)
-- Per spec: deduplication via partial unique index on active addresses only
-- This allows re-creating the same address after soft-delete

CREATE TABLE IF NOT EXISTS address (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  line1 VARCHAR(256) NOT NULL,
  line2 VARCHAR(256),
  city VARCHAR(128) NOT NULL,
  region VARCHAR(128),
  country CHAR(2) NOT NULL,
  postal_code VARCHAR(16),
  normalized_key VARCHAR(64) NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(255),
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(255),
  deleted_at TIMESTAMP,
  CONSTRAINT fk_address_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- RLS: Row Level Security (Postgres policy)
ALTER TABLE address ENABLE ROW LEVEL SECURITY;

-- Policy: tenant isolation (via tenant_id)
-- All tenants can read/write their own addresses
CREATE POLICY address_tenant_isolation
  ON address
  USING (tenant_id = current_setting('app.tenant_id')::uuid)
  WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- Index for tenant-scoped queries
CREATE INDEX idx_address_tenant_id ON address(tenant_id);

-- Partial unique index for dedup lookups (only non-deleted addresses)
-- Per spec option 1: allows recreating same address after soft-delete
CREATE UNIQUE INDEX ux_address_tenant_normalized_active
  ON address(tenant_id, normalized_key)
  WHERE deleted = FALSE;

-- Index for soft-delete queries
CREATE INDEX idx_address_tenant_not_deleted ON address(tenant_id) WHERE deleted = FALSE;


