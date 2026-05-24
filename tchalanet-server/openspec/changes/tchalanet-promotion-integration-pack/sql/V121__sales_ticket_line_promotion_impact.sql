-- Impact sales.ticket_line for promotions.
-- Assumes ticket_line is tenant-scoped and already has BaseTenantEntity columns.
-- Do not add tenant/deleted filters in repositories; RLS remains source of truth.

ALTER TABLE ticket_line
  ADD COLUMN IF NOT EXISTS origin varchar(32) NOT NULL DEFAULT 'CUSTOMER',
  ADD COLUMN IF NOT EXISTS pricing_source varchar(32) NOT NULL DEFAULT 'STANDARD',
  ADD COLUMN IF NOT EXISTS selection_source varchar(32) NOT NULL DEFAULT 'CUSTOMER_SELECTED',
  ADD COLUMN IF NOT EXISTS payout_base_amount numeric(19, 2) NULL,
  ADD COLUMN IF NOT EXISTS promotion_decision_id uuid NULL REFERENCES promotion_decision(id);

-- Backfill payout_base_amount for existing lines.
UPDATE ticket_line
SET payout_base_amount = stake_amount
WHERE payout_base_amount IS NULL;

ALTER TABLE ticket_line
  ALTER COLUMN payout_base_amount SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ticket_line_promotion_decision
  ON ticket_line (tenant_id, promotion_decision_id)
  WHERE promotion_decision_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ticket_line_origin
  ON ticket_line (tenant_id, origin, pricing_source);

-- Extend audit table if managed manually. If Envers generates DDL, mirror these fields there.
ALTER TABLE ticket_line_aud
  ADD COLUMN IF NOT EXISTS origin varchar(32),
  ADD COLUMN IF NOT EXISTS pricing_source varchar(32),
  ADD COLUMN IF NOT EXISTS selection_source varchar(32),
  ADD COLUMN IF NOT EXISTS payout_base_amount numeric(19, 2),
  ADD COLUMN IF NOT EXISTS promotion_decision_id uuid;

ALTER TABLE ticket_line
  ADD CONSTRAINT ck_ticket_line_promotion_origin CHECK (
    (origin <> 'PROMOTION' AND promotion_decision_id IS NULL)
    OR (origin = 'PROMOTION' AND promotion_decision_id IS NOT NULL)
    OR (pricing_source = 'PROMOTION' AND promotion_decision_id IS NOT NULL)
  );
