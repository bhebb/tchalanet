-- V13__core_payout.sql
-- Create payout table (full schema) in ONE migration.
-- Safe for prod: no DROP, idempotent, guards for constraints/triggers.

CREATE TABLE IF NOT EXISTS payout (
                                      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid NOT NULL REFERENCES tenant(id),

    -- --- Paying context (where the payment is executed)
    outlet_id uuid,            -- paying outlet
    session_id uuid,           -- paying session
    terminal_id uuid,
    paid_by_user_id uuid,

    -- --- Selling context (convenience, coming from ticket)
    ticket_id uuid NOT NULL REFERENCES ticket(id),
    selling_outlet_id uuid,
    selling_session_id uuid,

    -- --- Amounts
    amount numeric(14,2) NOT NULL CHECK (amount >= 0),
    amount_cents bigint,
    currency varchar(8) NOT NULL DEFAULT 'HTG',

    -- --- Status
    status varchar(24) NOT NULL DEFAULT 'REQUESTED',

    -- --- Timestamps / audit
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    requested_at timestamptz NOT NULL DEFAULT now(),
    requested_by uuid,
    approved_at timestamptz,
    approved_by uuid,
    paid_at timestamptz,
    rejected_at timestamptz,

    -- Rejection metadata
    rejected_reason varchar(255),

    reason varchar(255),
    meta jsonb NOT NULL DEFAULT '{}'::jsonb,

    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

-- -------------------------
-- Constraints (guarded)
-- -------------------------

-- One active payout per ticket per tenant
CREATE UNIQUE INDEX IF NOT EXISTS ux_payout_tenant_ticket_active
    ON payout(tenant_id, ticket_id)
    WHERE deleted_at IS NULL;

-- Status allowlist
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_payout_status') THEN
ALTER TABLE payout
    ADD CONSTRAINT chk_payout_status
        CHECK (status IN ('REQUESTED','APPROVED','PAID','REJECTED','CANCELLED'));
END IF;
END$$;

-- amount_cents positive (nullable to allow backfills)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_payout_amount_cents_positive') THEN
ALTER TABLE payout
    ADD CONSTRAINT chk_payout_amount_cents_positive
        CHECK (amount_cents IS NULL OR amount_cents >= 0);
END IF;
END$$;

-- -------------------------
-- Indexes
-- -------------------------
CREATE INDEX IF NOT EXISTS ix_payout_tenant_status
    ON payout (tenant_id, status);

CREATE INDEX IF NOT EXISTS ix_payout_tenant_requested_at
    ON payout (tenant_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS ix_payout_ticket
    ON payout (ticket_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_payout_session
    ON payout (session_id)
    WHERE session_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_payout_tenant_paid_at
    ON payout(tenant_id, paid_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_payout_tenant_outlet_created
    ON payout(tenant_id, outlet_id, created_at)
    WHERE deleted_at IS NULL AND outlet_id IS NOT NULL;

-- -------------------------
-- Trigger updated_at (if helper exists)
-- -------------------------
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_payout_set_updated_at') THEN
CREATE TRIGGER trg_payout_set_updated_at
    BEFORE UPDATE ON payout
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
END IF;
END IF;
END$$;

-- -------------------------
-- Backfill amount_cents (idempotent)
-- -------------------------
UPDATE payout
SET amount_cents = (amount * 100)::bigint
WHERE amount_cents IS NULL AND amount IS NOT NULL;
