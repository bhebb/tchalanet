-- V24__core__limit_definitions_and_assignments.sql
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

-- If additional limit-related functions are required, add them below.
