-- V14__draw_exposure.sql
-- Add draw_exposure read model for limit facts

CREATE TABLE draw_exposure (
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  draw_id uuid NOT NULL REFERENCES draw(id),
  scope_type varchar(16) NOT NULL CHECK (scope_type IN ('TENANT', 'AGENT', 'TERMINAL', 'OUTLET', 'ZONE', 'RANGE')),
  scope_id uuid NOT NULL, -- for TENANT, use tenant_id
  bet_type varchar(32), -- NULL for total
  selection_key varchar(64), -- NULL for total
  stake_total numeric(14,2) NOT NULL DEFAULT 0,
  sales_count bigint NOT NULL DEFAULT 0,
  potential_payout_total numeric(14,2) NOT NULL DEFAULT 0,
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)
);

-- Partial index for totals (bet_type IS NULL AND selection_key IS NULL)
CREATE INDEX ix_draw_exposure_totals ON draw_exposure(tenant_id, draw_id, scope_type, scope_id) WHERE bet_type IS NULL AND selection_key IS NULL;

-- Index for selection lookups
CREATE INDEX ix_draw_exposure_selection ON draw_exposure(tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key);

-- Function for atomic increment
CREATE OR REPLACE FUNCTION increment_draw_exposure(
  p_tenant_id uuid,
  p_draw_id uuid,
  p_scope_type varchar,
  p_scope_id uuid,
  p_bet_type varchar,
  p_selection_key varchar,
  p_stake_delta numeric,
  p_sales_delta bigint,
  p_payout_delta numeric
) RETURNS void AS $$
BEGIN
  INSERT INTO draw_exposure (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key, stake_total, sales_count, potential_payout_total)
  VALUES (p_tenant_id, p_draw_id, p_scope_type, p_scope_id, p_bet_type, p_selection_key, p_stake_delta, p_sales_delta, p_payout_delta)
  ON CONFLICT (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)
  DO UPDATE SET
    stake_total = draw_exposure.stake_total + p_stake_delta,
    sales_count = draw_exposure.sales_count + p_sales_delta,
    potential_payout_total = draw_exposure.potential_payout_total + p_payout_delta,
    updated_at = now();
END;
$$ LANGUAGE plpgsql;
