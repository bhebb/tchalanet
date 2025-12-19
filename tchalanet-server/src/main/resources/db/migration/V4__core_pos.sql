-- V4: core POS (outlet, terminal, pos_session)

CREATE TABLE IF NOT EXISTS outlet (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    name varchar(255) NOT NULL,
    type varchar(32) NOT NULL DEFAULT 'PHYSICAL', -- PHYSICAL|VIRTUAL
    address_id uuid NOT NULL REFERENCES address(id),
    active boolean NOT NULL DEFAULT true,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS ix_outlet_tenant_name ON outlet (tenant_id, name);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_outlet_updated_at') THEN
    CREATE TRIGGER trg_outlet_updated_at
      BEFORE UPDATE ON outlet
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS terminal (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),
    outlet_id uuid NOT NULL REFERENCES outlet(id),
    state varchar(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|INACTIVE|BLOCKED
    last_seen timestamptz,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE INDEX IF NOT EXISTS ix_terminal_tenant_outlet ON terminal (tenant_id, outlet_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_terminal_updated_at') THEN
    CREATE TRIGGER trg_terminal_updated_at
      BEFORE UPDATE ON terminal
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS pos_session (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL REFERENCES tenant(id),
    outlet_id uuid REFERENCES outlet(id),
    terminal_id uuid REFERENCES terminal(id),
    user_id uuid NOT NULL REFERENCES app_user(id),

    status varchar(16) NOT NULL, -- OPEN|CLOSED|SETTLED
    opened_at timestamptz NOT NULL DEFAULT now(),
    closed_at timestamptz,
    opening_float numeric(14, 2),
    closing_amount numeric(14, 2),
    total_tickets bigint,
    total_stake numeric(14, 2),
    total_payout numeric(14, 2),
    gross_margin numeric(14, 2),
    meta jsonb NOT NULL DEFAULT '{}'::jsonb,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    UNIQUE (tenant_id, terminal_id, status) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE INDEX IF NOT EXISTS ix_pos_session_tenant_terminal ON pos_session (tenant_id, terminal_id, status);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pos_session_updated_at') THEN
    CREATE TRIGGER trg_pos_session_updated_at
      BEFORE UPDATE ON pos_session
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

