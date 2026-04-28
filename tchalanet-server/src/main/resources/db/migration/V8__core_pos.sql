-- V4__sales_session.sql (recreate table, no ALTER)
DROP TABLE IF EXISTS sales_session CASCADE;

CREATE TABLE sales_session (
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
CREATE UNIQUE INDEX ux_sales_session_open_per_terminal
    ON sales_session(tenant_id, terminal_id)
    WHERE status = 'OPEN' AND deleted_at IS NULL;

CREATE INDEX ix_sales_session_tenant_terminal
    ON sales_session(tenant_id, terminal_id);

CREATE INDEX ix_sales_session_tenant_opened_at
    ON sales_session(tenant_id, opened_at DESC);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_sales_session_updated_at') THEN
CREATE TRIGGER trg_sales_session_updated_at
    BEFORE UPDATE ON sales_session
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

DROP TABLE IF EXISTS sales_session_totals CASCADE;

CREATE TABLE sales_session_totals (
                                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                    session_id uuid NOT NULL REFERENCES sales_session(id) ON DELETE CASCADE,
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

 CREATE UNIQUE INDEX IF NOT EXISTS ux_sales_session_totals_session_id ON sales_session_totals(session_id);

 CREATE INDEX ix_sales_session_totals_tenant ON sales_session_totals(tenant_id);
 CREATE INDEX ix_sales_session_totals_tenant_session ON sales_session_totals(tenant_id, session_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_sales_session_totals_updated_at') THEN
CREATE TRIGGER trg_sales_session_totals_updated_at
    BEFORE UPDATE ON sales_session_totals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;
