-- Effective payout amount is distinct from the calculated winning amount.
-- Existing approved tickets remain unpaid until their historical amount is
-- explicitly corrected, so the default is zero.
ALTER TABLE sales_ticket
  ADD COLUMN IF NOT EXISTS paid_amount numeric(19,4) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS paid_amount_adjusted_at timestamptz,
  ADD COLUMN IF NOT EXISTS paid_amount_adjusted_by uuid,
  ADD COLUMN IF NOT EXISTS paid_amount_adjustment_reason varchar(500);
