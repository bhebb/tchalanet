-- Sketch only — adapt to existing sales schema.

-- Ticket-level seller snapshot fields:
-- ALTER TABLE ticket ADD COLUMN sold_by_user_id uuid NULL;
-- ALTER TABLE ticket ADD COLUMN seller_id uuid NULL;
-- ALTER TABLE ticket ADD COLUMN seller_assignment_id uuid NULL;
-- ALTER TABLE ticket ADD COLUMN seller_display_name_snapshot varchar(160) NULL;
-- ALTER TABLE ticket ADD COLUMN seller_code_snapshot varchar(64) NULL;
-- ALTER TABLE ticket ADD COLUMN seller_commission_snapshot jsonb NULL;

-- TicketLine promotion fields:
-- ALTER TABLE ticket_line ADD COLUMN origin varchar(48) NULL;
-- ALTER TABLE ticket_line ADD COLUMN pricing_source varchar(48) NULL;
-- ALTER TABLE ticket_line ADD COLUMN selection_source varchar(48) NULL;
-- ALTER TABLE ticket_line ADD COLUMN payout_base_amount numeric(19,4) NULL;
-- ALTER TABLE ticket_line ADD COLUMN promotion_decision_id uuid NULL;
-- ALTER TABLE ticket_line ADD COLUMN odds_override numeric(12,4) NULL;

-- Charge snapshot table if not already modeled in MoneyBreakdown:
-- CREATE TABLE ticket_charge_snapshot (...);
