-- maryaj-gratis-auto-selection-v1 — slice 6 (SalePreparation core)
-- Server-side sale preparation: the confirmed ticket must contain exactly the
-- lines seen at preview. Working trace only — the persisted ticket stays the
-- financial truth. Retention: DRAFT TTL 10 min (lazy expiry + periodic job),
-- EXPIRED/CANCELLED purged after 7 days, CONFIRMED kept 30 days or until
-- reconciliation.

CREATE TABLE sale_preparation (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  seller_id uuid NULL,
  session_id uuid NULL,
  terminal_id uuid NULL,
  draw_id uuid NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'DRAFT',
  input_hash varchar(64) NOT NULL,
  paid_lines_json jsonb NOT NULL,
  promotion_decision_id uuid NULL,
  idempotency_key varchar(96) NULL,
  ticket_id uuid NULL,
  expires_at timestamptz NOT NULL,
  confirmed_at timestamptz NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_sale_preparation_status
    CHECK (status IN ('DRAFT','CONFIRMED','EXPIRED','CANCELLED')),
  CONSTRAINT chk_sale_preparation_confirmed
    CHECK (status <> 'CONFIRMED' OR (ticket_id IS NOT NULL AND confirmed_at IS NOT NULL))
);

CREATE INDEX idx_sale_preparation_tenant_status_expires
  ON sale_preparation (tenant_id, status, expires_at);

-- Idempotent confirm: one preparation per idempotency key per tenant.
CREATE UNIQUE INDEX uq_sale_preparation_tenant_idem
  ON sale_preparation (tenant_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

CREATE TABLE sale_preparation_promotion_line (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  preparation_id uuid NOT NULL REFERENCES sale_preparation(id) ON DELETE CASCADE,
  line_ref varchar(36) NOT NULL,
  game_code varchar(64) NOT NULL,
  bet_type varchar(32) NOT NULL,
  bet_option smallint NULL,
  selection varchar(32) NOT NULL,
  payout_base_amount numeric(19,4) NOT NULL,
  promotion_decision_id uuid NULL,
  promotion_rule_id uuid NULL,
  regenerable boolean NOT NULL DEFAULT false,
  max_regenerations integer NOT NULL DEFAULT 3,
  regeneration_count integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_sale_preparation_line_ref UNIQUE (preparation_id, line_ref),
  CONSTRAINT chk_sale_preparation_line_regen
    CHECK (regeneration_count >= 0 AND regeneration_count <= max_regenerations)
);

CREATE INDEX idx_sale_preparation_promotion_line_prep
  ON sale_preparation_promotion_line (preparation_id);

-- RLS (same pattern as V105)
ALTER TABLE sale_preparation ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_preparation FORCE ROW LEVEL SECURITY;
CREATE POLICY sale_preparation_rls_all ON sale_preparation
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
    )
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());

ALTER TABLE sale_preparation_promotion_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE sale_preparation_promotion_line FORCE ROW LEVEL SECURITY;
CREATE POLICY sale_preparation_promotion_line_rls_all ON sale_preparation_promotion_line
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
    )
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());

GRANT SELECT, INSERT, UPDATE, DELETE ON sale_preparation TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON sale_preparation_promotion_line TO app_user;
