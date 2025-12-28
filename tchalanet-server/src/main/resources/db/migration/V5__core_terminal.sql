-- V5__core_terminal.sql (recreate table, no ALTER)
DROP TABLE IF EXISTS terminal CASCADE;

CREATE TABLE terminal (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version bigint NOT NULL DEFAULT 0,
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  -- Device state machine
  state varchar(16) NOT NULL DEFAULT 'ACTIVE'
    CHECK (state IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
  -- Optional human label / inventory id
  label varchar(128),
  inventory_tag varchar(64),
  -- Last heartbeat
  last_seen timestamptz,
  -- Registration lifecycle
  registered_at timestamptz NOT NULL DEFAULT now(),
  unregistered_at timestamptz,
  -- Lock info (admin action)
  locked_at timestamptz,
  locked_by uuid,
  lock_reason varchar(255),
  -- Device metadata / capabilities / app versions
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

-- Useful indexes
CREATE INDEX ix_terminal_tenant_outlet ON terminal (tenant_id, outlet_id);
CREATE INDEX ix_terminal_tenant_state ON terminal (tenant_id, state);
CREATE INDEX ix_terminal_tenant_last_seen ON terminal (tenant_id, last_seen DESC);

-- Ensure one active terminal label per outlet (optional)
-- CREATE UNIQUE INDEX ux_terminal_outlet_label
--   ON terminal(tenant_id, outlet_id, label)
--   WHERE deleted_at IS NULL AND label IS NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_terminal_updated_at') THEN
    CREATE TRIGGER trg_terminal_updated_at
      BEFORE UPDATE ON terminal
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;
