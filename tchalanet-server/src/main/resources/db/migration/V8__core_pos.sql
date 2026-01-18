-- V4__pos_session.sql (recreate table, no ALTER)
DROP TABLE IF EXISTS pos_session CASCADE;

CREATE TABLE pos_session (
                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             version bigint NOT NULL DEFAULT 0,

                             tenant_id uuid NOT NULL REFERENCES tenant(id),
                             outlet_id uuid NOT NULL REFERENCES outlet(id),
                             terminal_id uuid NOT NULL REFERENCES terminal(id),
                             user_id uuid NOT NULL REFERENCES app_user(id),

                             status varchar(16) NOT NULL CHECK (status IN ('OPEN','CLOSED','SETTLED')),
                             opened_at timestamptz NOT NULL DEFAULT now(),
                             closed_at timestamptz,

                             opening_float numeric(14,2) NOT NULL DEFAULT 0,
                             closing_amount numeric(14,2) NOT NULL DEFAULT 0,

                             meta jsonb NOT NULL DEFAULT '{}'::jsonb,

                             created_at timestamptz NOT NULL DEFAULT now(),
                             created_by uuid,
                             updated_at timestamptz NOT NULL DEFAULT now(),
                             updated_by uuid,
                             deleted_at timestamptz
);

-- Un seul OPEN par terminal (unique partiel) -> mieux que UNIQUE(tenant, terminal, status)
CREATE UNIQUE INDEX ux_pos_session_open_per_terminal
    ON pos_session(tenant_id, terminal_id)
    WHERE status = 'OPEN' AND deleted_at IS NULL;

CREATE INDEX ix_pos_session_tenant_terminal
    ON pos_session(tenant_id, terminal_id);

CREATE INDEX ix_pos_session_tenant_opened_at
    ON pos_session(tenant_id, opened_at DESC);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pos_session_updated_at') THEN
CREATE TRIGGER trg_pos_session_updated_at
    BEFORE UPDATE ON pos_session
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

DROP TABLE IF EXISTS pos_session_totals CASCADE;

CREATE TABLE pos_session_totals (
                                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                    session_id uuid NOT NULL REFERENCES pos_session(id) ON DELETE CASCADE,
                                    tenant_id uuid NOT NULL REFERENCES tenant(id),

                                    total_tickets bigint NOT NULL DEFAULT 0,
                                    total_stake numeric(14,2) NOT NULL DEFAULT 0,
                                    total_payout numeric(14,2) NOT NULL DEFAULT 0,
                                    gross_margin numeric(14,2) NOT NULL DEFAULT 0,

                                    version bigint NOT NULL DEFAULT 0,

                                    created_at timestamptz NOT NULL DEFAULT now(),
                                    created_by uuid,
                                    updated_at timestamptz NOT NULL DEFAULT now(),
                                    updated_by uuid,
                                    deleted_at timestamptz
 );

 CREATE UNIQUE INDEX IF NOT EXISTS ux_pos_session_totals_session_id ON pos_session_totals(session_id);

 CREATE INDEX ix_pos_session_totals_tenant ON pos_session_totals(tenant_id);
 CREATE INDEX ix_pos_session_totals_tenant_session ON pos_session_totals(tenant_id, session_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pos_session_totals_updated_at') THEN
CREATE TRIGGER trg_pos_session_totals_updated_at
    BEFORE UPDATE ON pos_session_totals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;
