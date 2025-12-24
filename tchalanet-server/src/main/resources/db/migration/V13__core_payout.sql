-- V13__core_payout.sql (recreate table, no ALTER)
DROP TABLE IF EXISTS payout CASCADE;

CREATE TABLE payout (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version bigint NOT NULL DEFAULT 0,
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  -- Optional scoping / traceability
  outlet_id uuid REFERENCES outlet(id),
  terminal_id uuid REFERENCES terminal(id),
  session_id uuid REFERENCES pos_session(id),
  -- Ticket payout
  ticket_id uuid REFERENCES ticket(id),
  status varchar(16) NOT NULL DEFAULT 'REQUESTED'
    CHECK (status IN ('REQUESTED', 'APPROVED', 'PAID', 'REJECTED', 'CANCELLED')),
  amount numeric(14,2) NOT NULL CHECK (amount >= 0),
  currency varchar(8) NOT NULL DEFAULT 'USD',
  method varchar(16) NOT NULL DEFAULT 'CASH'
    CHECK (method IN ('CASH', 'MOBILE_MONEY', 'BANK', 'OTHER')),
  requested_at timestamptz NOT NULL DEFAULT now(),
  requested_by uuid,
  approved_at timestamptz,
  approved_by uuid,
  paid_at timestamptz,
  paid_by uuid,
  reason varchar(255),
  meta jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

CREATE INDEX ix_payout_tenant_status ON payout (tenant_id, status);
CREATE INDEX ix_payout_tenant_requested_at ON payout (tenant_id, requested_at DESC);
CREATE INDEX ix_payout_ticket ON payout (ticket_id) WHERE ticket_id IS NOT NULL;
CREATE INDEX ix_payout_session ON payout (session_id) WHERE session_id IS NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_payout_updated_at') THEN
    CREATE TRIGGER trg_payout_updated_at
      BEFORE UPDATE ON payout
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;
