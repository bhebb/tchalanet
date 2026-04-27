-- V19__core__limit_definitions_and_assignments.sql
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

CREATE UNIQUE INDEX ux_limit_definition_tenant_rule
    ON limit_definition(tenant_id, rule_key)
    WHERE deleted_at IS NULL;

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

CREATE INDEX ix_limit_assignment_tenant_target
    ON limit_assignment(tenant_id, target_type, target_id)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_limit_assignment_limit
    ON limit_assignment(limit_definition_id)
    WHERE deleted_at IS NULL;

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


CREATE TABLE IF NOT EXISTS draw_exposure (
                                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL,

    draw_id uuid NOT NULL,

    scope_type varchar(16) NOT NULL, -- TENANT|OUTLET|AGENT|ZONE|RANGE
    scope_id uuid NULL,              -- NULL when scope_type=TENANT (decision #1)

    bet_type varchar(32) NULL,
    selection_key varchar(64) NULL,

    stake_total numeric(18,2) NOT NULL DEFAULT 0,
    sales_count bigint NOT NULL DEFAULT 0,
    potential_payout_total numeric(18,2) NOT NULL DEFAULT 0,

    updated_at timestamptz NOT NULL DEFAULT now(),

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    deleted_at timestamptz
    );

-- Unique key for upsert (soft delete aware)
CREATE UNIQUE INDEX IF NOT EXISTS ux_draw_exposure_key
    ON draw_exposure (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_draw_exposure_lookup
    ON draw_exposure (tenant_id, draw_id, scope_type, scope_id)
    WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
DROP TRIGGER IF EXISTS trg_draw_exposure_updated_at ON draw_exposure;
CREATE TRIGGER trg_draw_exposure_updated_at
    BEFORE UPDATE ON draw_exposure
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;
END $$;
