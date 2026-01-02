-- Add total_payout column to ticket to store realized payout per ticket
ALTER TABLE ticket
  ADD COLUMN IF NOT EXISTS total_payout numeric(14,2) NOT NULL DEFAULT 0;

-- Also add to audit table if using custom audit tables (envers will handle schema in many setups)
ALTER TABLE IF EXISTS ticket_aud
  ADD COLUMN IF NOT EXISTS total_payout numeric(14,2);

