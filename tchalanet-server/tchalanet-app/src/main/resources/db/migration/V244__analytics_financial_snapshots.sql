-- ============================================================
-- V244 — analytics financial snapshots
--
-- Adds sale-time financial snapshot metrics used by dashboards and reports.
-- These values are projected from ticket events; they must not be recalculated
-- from current tenant or seller-terminal settings.
-- ============================================================

ALTER TABLE analytics_daily
  ADD COLUMN IF NOT EXISTS seller_commission_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS buyer_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS seller_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tenant_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS waived_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_line_count bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_priced_line_count bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_payout_base_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_potential_payout_cents bigint NOT NULL DEFAULT 0;

ALTER TABLE analytics_draw
  ADD COLUMN IF NOT EXISTS seller_commission_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS buyer_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS seller_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS tenant_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS waived_charge_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_line_count bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_priced_line_count bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_payout_base_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS promotion_potential_payout_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS net_revenue_estimated_cents bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS net_revenue_paid_basis_cents bigint NOT NULL DEFAULT 0;

COMMENT ON COLUMN analytics_daily.seller_commission_cents
  IS 'Seller-terminal commission amount projected from sale-time ticket snapshots.';

COMMENT ON COLUMN analytics_draw.seller_commission_cents
  IS 'Seller-terminal commission amount projected from sale-time ticket snapshots.';

COMMENT ON COLUMN analytics_daily.buyer_charge_cents
  IS 'Non-waived ticket charges paid by the buyer; these are buyer-facing pass-through fees.';

COMMENT ON COLUMN analytics_daily.seller_charge_cents
  IS 'Non-waived ticket charges absorbed by the seller terminal.';

COMMENT ON COLUMN analytics_daily.tenant_charge_cents
  IS 'Non-waived ticket charges absorbed by the tenant as operating expense.';

COMMENT ON COLUMN analytics_daily.waived_charge_cents
  IS 'Original amount of waived ticket charges retained for promotion/reporting visibility.';

COMMENT ON COLUMN analytics_daily.promotion_line_count
  IS 'Ticket lines added by promotion effects, for example FREE_GAME_LINE.';

COMMENT ON COLUMN analytics_daily.promotion_priced_line_count
  IS 'Ticket lines whose pricing/odds snapshot was modified by promotion effects.';

COMMENT ON COLUMN analytics_daily.promotion_payout_base_cents
  IS 'Payout base amount exposed by promotion-created lines.';

COMMENT ON COLUMN analytics_daily.promotion_potential_payout_cents
  IS 'Potential payout exposure from promotion-created lines.';

CREATE OR REPLACE FUNCTION public.upsert_analytics_daily(
  p_dimension_type varchar,
  p_dimension_id   uuid,
  p_tenant_id      uuid,
  p_ref_date       date,
  p_tickets_sold   bigint,
  p_tickets_cancelled bigint,
  p_gross_sales    bigint,
  p_stake_total    bigint,
  p_winnings_calc  bigint,
  p_payouts_paid   bigint,
  p_seller_commission bigint,
  p_buyer_charge bigint,
  p_seller_charge bigint,
  p_tenant_charge bigint,
  p_waived_charge bigint,
  p_promotion_line_count bigint,
  p_promotion_priced_line_count bigint,
  p_promotion_payout_base bigint,
  p_promotion_potential_payout bigint,
  p_sessions_opened bigint,
  p_sessions_closed bigint
) RETURNS void AS $$
DECLARE
  v_net_estimated  bigint := p_gross_sales - p_winnings_calc - p_seller_commission - p_tenant_charge;
  v_net_paid_basis bigint := p_gross_sales - p_payouts_paid - p_seller_commission - p_tenant_charge;
BEGIN
  INSERT INTO analytics_daily (
    dimension_type, dimension_id, tenant_id, ref_date,
    tickets_sold_count, tickets_cancelled_count,
    gross_sales_cents, stake_total_cents,
    winnings_calculated_cents, payouts_paid_cents,
    seller_commission_cents,
    buyer_charge_cents, seller_charge_cents, tenant_charge_cents, waived_charge_cents,
    promotion_line_count, promotion_priced_line_count,
    promotion_payout_base_cents, promotion_potential_payout_cents,
    net_revenue_estimated_cents, net_revenue_paid_basis_cents,
    sessions_opened_count, sessions_closed_count
  ) VALUES (
    p_dimension_type, p_dimension_id, p_tenant_id, p_ref_date,
    p_tickets_sold, p_tickets_cancelled,
    p_gross_sales, p_stake_total,
    p_winnings_calc, p_payouts_paid,
    p_seller_commission,
    p_buyer_charge, p_seller_charge, p_tenant_charge, p_waived_charge,
    p_promotion_line_count, p_promotion_priced_line_count,
    p_promotion_payout_base, p_promotion_potential_payout,
    v_net_estimated, v_net_paid_basis,
    p_sessions_opened, p_sessions_closed
  )
  ON CONFLICT (
    dimension_type,
    COALESCE(dimension_id, '00000000-0000-0000-0000-000000000000'::uuid),
    COALESCE(tenant_id,    '00000000-0000-0000-0000-000000000000'::uuid),
    ref_date
  ) DO UPDATE SET
    tickets_sold_count            = analytics_daily.tickets_sold_count            + EXCLUDED.tickets_sold_count,
    tickets_cancelled_count       = analytics_daily.tickets_cancelled_count       + EXCLUDED.tickets_cancelled_count,
    gross_sales_cents             = analytics_daily.gross_sales_cents             + EXCLUDED.gross_sales_cents,
    stake_total_cents             = analytics_daily.stake_total_cents             + EXCLUDED.stake_total_cents,
    winnings_calculated_cents     = analytics_daily.winnings_calculated_cents     + EXCLUDED.winnings_calculated_cents,
    payouts_paid_cents            = analytics_daily.payouts_paid_cents            + EXCLUDED.payouts_paid_cents,
    seller_commission_cents       = analytics_daily.seller_commission_cents       + EXCLUDED.seller_commission_cents,
    buyer_charge_cents            = analytics_daily.buyer_charge_cents            + EXCLUDED.buyer_charge_cents,
    seller_charge_cents           = analytics_daily.seller_charge_cents           + EXCLUDED.seller_charge_cents,
    tenant_charge_cents           = analytics_daily.tenant_charge_cents           + EXCLUDED.tenant_charge_cents,
    waived_charge_cents           = analytics_daily.waived_charge_cents           + EXCLUDED.waived_charge_cents,
    promotion_line_count          = analytics_daily.promotion_line_count          + EXCLUDED.promotion_line_count,
    promotion_priced_line_count   = analytics_daily.promotion_priced_line_count   + EXCLUDED.promotion_priced_line_count,
    promotion_payout_base_cents   = analytics_daily.promotion_payout_base_cents   + EXCLUDED.promotion_payout_base_cents,
    promotion_potential_payout_cents = analytics_daily.promotion_potential_payout_cents + EXCLUDED.promotion_potential_payout_cents,
    net_revenue_estimated_cents   = analytics_daily.net_revenue_estimated_cents   + EXCLUDED.net_revenue_estimated_cents,
    net_revenue_paid_basis_cents  = analytics_daily.net_revenue_paid_basis_cents  + EXCLUDED.net_revenue_paid_basis_cents,
    sessions_opened_count         = analytics_daily.sessions_opened_count         + EXCLUDED.sessions_opened_count,
    sessions_closed_count         = analytics_daily.sessions_closed_count         + EXCLUDED.sessions_closed_count,
    version                       = analytics_daily.version + 1,
    updated_at                    = now();
END;
$$ LANGUAGE plpgsql;
