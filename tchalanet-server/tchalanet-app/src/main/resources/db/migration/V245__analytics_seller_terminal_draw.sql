-- ============================================================
-- V245 — analytics seller-terminal by draw projection
--
-- Exact tenant-admin drilldown for commissions, charges and promotions
-- by seller terminal and draw. Values are derived from sale-time snapshots
-- and ticket lifecycle events.
-- ============================================================

CREATE TABLE IF NOT EXISTS analytics_seller_terminal_draw (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  seller_terminal_id uuid NOT NULL,
  draw_id uuid NOT NULL,
  ref_date date NOT NULL,
  scheduled_at timestamptz NOT NULL,
  game_code varchar(128) NOT NULL DEFAULT 'UNKNOWN',
  draw_channel_code varchar(128),

  tickets_sold_count bigint NOT NULL DEFAULT 0,
  gross_sales_cents bigint NOT NULL DEFAULT 0,
  stake_total_cents bigint NOT NULL DEFAULT 0,
  winnings_calculated_cents bigint NOT NULL DEFAULT 0,
  payouts_paid_cents bigint NOT NULL DEFAULT 0,
  seller_commission_cents bigint NOT NULL DEFAULT 0,
  buyer_charge_cents bigint NOT NULL DEFAULT 0,
  seller_charge_cents bigint NOT NULL DEFAULT 0,
  tenant_charge_cents bigint NOT NULL DEFAULT 0,
  waived_charge_cents bigint NOT NULL DEFAULT 0,
  promotion_line_count bigint NOT NULL DEFAULT 0,
  promotion_priced_line_count bigint NOT NULL DEFAULT 0,
  promotion_payout_base_cents bigint NOT NULL DEFAULT 0,
  promotion_potential_payout_cents bigint NOT NULL DEFAULT 0,
  net_revenue_estimated_cents bigint NOT NULL DEFAULT 0,
  net_revenue_paid_basis_cents bigint NOT NULL DEFAULT 0,

  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0,

  CONSTRAINT uq_analytics_seller_terminal_draw
    UNIQUE (tenant_id, seller_terminal_id, draw_id)
);

CREATE INDEX IF NOT EXISTS idx_analytics_seller_terminal_draw_tenant_date
  ON analytics_seller_terminal_draw (tenant_id, ref_date DESC);

CREATE INDEX IF NOT EXISTS idx_analytics_seller_terminal_draw_terminal_date
  ON analytics_seller_terminal_draw (tenant_id, seller_terminal_id, ref_date DESC);

CREATE INDEX IF NOT EXISTS idx_analytics_seller_terminal_draw_draw
  ON analytics_seller_terminal_draw (tenant_id, draw_id);

COMMENT ON TABLE analytics_seller_terminal_draw
  IS 'Exact financial projection by tenant, seller terminal and draw.';

COMMENT ON COLUMN analytics_seller_terminal_draw.seller_commission_cents
  IS 'Seller-terminal commission amount projected from sale-time ticket snapshots for this draw.';
