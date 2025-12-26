-- Add rejected columns to payout table for storing rejection metadata
ALTER TABLE payout
  ADD COLUMN IF NOT EXISTS rejected_at timestamptz;

ALTER TABLE payout
  ADD COLUMN IF NOT EXISTS rejected_reason varchar(255);

-- Optionally create index on rejected_at for tenant queries
CREATE INDEX IF NOT EXISTS ix_payout_tenant_rejected_at
  ON payout(tenant_id, rejected_at) WHERE deleted_at IS NULL;

