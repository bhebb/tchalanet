-- V3_core__limit_definitions_and_assignments.sql
-- Add limit definitions and assignments tables

CREATE TABLE limit_definition (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version bigint NOT NULL DEFAULT 0,
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  rule_key varchar(64) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  on_breach varchar(16) NOT NULL DEFAULT 'BLOCK' CHECK (on_breach IN ('ALLOW', 'WARN', 'BLOCK')),
  params jsonb NOT NULL DEFAULT '{}'::jsonb,
  applies_to jsonb NOT NULL DEFAULT '{}'::jsonb, -- {bet_types: [], selection_pattern: "*"}
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

CREATE UNIQUE INDEX ux_limit_definition_tenant_rule ON limit_definition(tenant_id, rule_key) WHERE deleted_at IS NULL;

CREATE TABLE limit_assignment (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version bigint NOT NULL DEFAULT 0,
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  limit_definition_id uuid NOT NULL REFERENCES limit_definition(id),
  target_type varchar(16) NOT NULL CHECK (target_type IN ('TENANT', 'AGENT', 'TERMINAL', 'OUTLET', 'ZONE', 'RANGE', 'DRAWCHANNEL')),
  target_id uuid, -- NULL for TENANT
  enabled boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

CREATE INDEX ix_limit_assignment_tenant_target ON limit_assignment(tenant_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_limit_assignment_limit ON limit_assignment(limit_definition_id) WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_limit_definition_updated_at') THEN
    CREATE TRIGGER trg_limit_definition_updated_at
      BEFORE UPDATE ON limit_definition
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_limit_assignment_updated_at') THEN
    CREATE TRIGGER trg_limit_assignment_updated_at
      BEFORE UPDATE ON limit_assignment
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

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
