-- Sketch only — not production ready without aligning naming, audit, RLS, indexes.

CREATE TABLE seller (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  user_id uuid NULL,
  code varchar(64) NULL,
  display_name varchar(160) NOT NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL,
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  version bigint NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_seller_tenant_code_active
  ON seller (tenant_id, code)
  WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_seller_tenant_user
  ON seller (tenant_id, user_id)
  WHERE deleted_at IS NULL;

CREATE TABLE seller_outlet_assignment (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  seller_id uuid NOT NULL REFERENCES seller(id),
  outlet_id uuid NOT NULL,
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL,
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  version bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_seller_outlet_assignment_seller
  ON seller_outlet_assignment (tenant_id, seller_id, starts_at DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX idx_seller_outlet_assignment_outlet
  ON seller_outlet_assignment (tenant_id, outlet_id, starts_at DESC)
  WHERE deleted_at IS NULL;

CREATE TABLE seller_commission_policy (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  seller_id uuid NOT NULL REFERENCES seller(id),
  type varchar(48) NOT NULL,
  base varchar(48) NOT NULL,
  rate_percent numeric(9,4) NULL,
  fixed_amount numeric(19,4) NULL,
  currency varchar(3) NULL,
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL,
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  version bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_seller_commission_policy_current
  ON seller_commission_policy (tenant_id, seller_id, starts_at DESC)
  WHERE deleted_at IS NULL;
