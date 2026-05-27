-- ============================================================
-- V109 — core.analytics tables
-- create-core-analytics (OpenSpec)
--
-- Tables:
--   analytics_daily   — daily KPI projections by dimension
--   analytics_draw    — per-draw aggregate projections
--
-- Design rules (proposal §SQL baseline):
--   - Every table with updated_at has a trg_<table>__set_updated_at trigger.
--   - Unique index drives the ON CONFLICT upsert in Java projectors.
--   - RLS enabled; analytics rows are NOT tenant-row-filtered at DB level
--     (global reads needed for platform dashboard) — tenant column present
--     for application-level filtering.
--   - app_user gets INSERT/UPDATE/SELECT; no DELETE (purge uses DELETE priv).
-- ============================================================

-- ────────────────────────────────────────────────
-- analytics_daily
-- ────────────────────────────────────────────────
CREATE TABLE analytics_daily (
  id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),

  -- dimension (PLATFORM / TENANT / OUTLET / SELLER / GAME / DRAW_CHANNEL)
  dimension_type  varchar(64)  NOT NULL,
  dimension_id    uuid,          -- null for PLATFORM
  tenant_id       uuid         REFERENCES tenant(id),

  -- business date in tenant-local timezone (UTC for PLATFORM)
  ref_date        date         NOT NULL,

  -- ticket counts
  tickets_sold_count       bigint NOT NULL DEFAULT 0,
  tickets_cancelled_count  bigint NOT NULL DEFAULT 0,

  -- money (cents)
  gross_sales_cents        bigint NOT NULL DEFAULT 0,
  stake_total_cents        bigint NOT NULL DEFAULT 0,
  winnings_calculated_cents bigint NOT NULL DEFAULT 0,
  payouts_paid_cents       bigint NOT NULL DEFAULT 0,
  net_revenue_estimated_cents bigint NOT NULL DEFAULT 0,
  net_revenue_paid_basis_cents bigint NOT NULL DEFAULT 0,

  -- session counts (V1: updated when session events are available via public API)
  sessions_opened_count    bigint NOT NULL DEFAULT 0,
  sessions_closed_count    bigint NOT NULL DEFAULT 0,

  -- optimistic lock
  version         bigint       NOT NULL DEFAULT 0,

  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now()
);

-- Unique constraint drives ON CONFLICT upsert
CREATE UNIQUE INDEX uix_analytics_daily__dimension_date
  ON analytics_daily (dimension_type, COALESCE(dimension_id, '00000000-0000-0000-0000-000000000000'::uuid), COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid), ref_date);

-- Tenant+date range scan (tenant admin dashboard)
CREATE INDEX ix_analytics_daily__tenant_date
  ON analytics_daily (tenant_id, ref_date)
  WHERE tenant_id IS NOT NULL;

-- Dimension+date scan (outlet/seller drilldown)
CREATE INDEX ix_analytics_daily__dimension_id_date
  ON analytics_daily (dimension_id, ref_date)
  WHERE dimension_id IS NOT NULL;

-- Global date scan (platform dashboard)
CREATE INDEX ix_analytics_daily__date
  ON analytics_daily (ref_date);

-- ────────────────────────────────────────────────
-- analytics_draw
-- ────────────────────────────────────────────────
CREATE TABLE analytics_draw (
  id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),

  draw_id         uuid         NOT NULL UNIQUE REFERENCES draw(id),
  tenant_id       uuid         NOT NULL REFERENCES tenant(id),
  game_code       varchar(255) NOT NULL,
  draw_channel_code varchar(255),
  scheduled_at    timestamptz  NOT NULL,
  ref_date        date         NOT NULL,

  -- ticket counts
  tickets_sold_count       bigint NOT NULL DEFAULT 0,
  tickets_cancelled_count  bigint NOT NULL DEFAULT 0,

  -- money (cents)
  gross_sales_cents        bigint NOT NULL DEFAULT 0,
  stake_total_cents        bigint NOT NULL DEFAULT 0,
  winnings_calculated_cents bigint NOT NULL DEFAULT 0,
  payouts_paid_cents       bigint NOT NULL DEFAULT 0,

  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now()
);

-- Tenant+date range
CREATE INDEX ix_analytics_draw__tenant_date
  ON analytics_draw (tenant_id, ref_date);

-- Game+date (game performance queries)
CREATE INDEX ix_analytics_draw__game_date
  ON analytics_draw (game_code, ref_date);

-- ────────────────────────────────────────────────
-- Triggers (updated_at)
-- ────────────────────────────────────────────────
CREATE TRIGGER trg_analytics_daily__set_updated_at
  BEFORE UPDATE ON analytics_daily
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_analytics_draw__set_updated_at
  BEFORE UPDATE ON analytics_draw
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ────────────────────────────────────────────────
-- RLS — enable but no policy (no row-level tenant filter;
-- app-level filtering via tenant_id column is sufficient for
-- cross-tenant platform reads; tenant-scoped reads filter
-- by tenant_id in Java query handlers).
-- ────────────────────────────────────────────────
ALTER TABLE analytics_daily  ENABLE ROW LEVEL SECURITY;
ALTER TABLE analytics_draw   ENABLE ROW LEVEL SECURITY;

-- Allow app_user full read+write (purge needs DELETE on analytics)
GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_daily  TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_draw   TO app_user;

-- ────────────────────────────────────────────────
-- Atomic upsert helper function (inspired by increment_draw_exposure)
-- Java projectors call this for concurrent-safe increments.
-- ────────────────────────────────────────────────
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
  p_sessions_opened bigint,
  p_sessions_closed bigint
) RETURNS void AS $$
DECLARE
  v_net_estimated  bigint := p_gross_sales - p_winnings_calc;
  v_net_paid_basis bigint := p_gross_sales - p_payouts_paid;
BEGIN
  INSERT INTO analytics_daily (
    dimension_type, dimension_id, tenant_id, ref_date,
    tickets_sold_count, tickets_cancelled_count,
    gross_sales_cents, stake_total_cents,
    winnings_calculated_cents, payouts_paid_cents,
    net_revenue_estimated_cents, net_revenue_paid_basis_cents,
    sessions_opened_count, sessions_closed_count
  ) VALUES (
    p_dimension_type, p_dimension_id, p_tenant_id, p_ref_date,
    p_tickets_sold, p_tickets_cancelled,
    p_gross_sales, p_stake_total,
    p_winnings_calc, p_payouts_paid,
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
    net_revenue_estimated_cents   = analytics_daily.net_revenue_estimated_cents   + EXCLUDED.net_revenue_estimated_cents,
    net_revenue_paid_basis_cents  = analytics_daily.net_revenue_paid_basis_cents  + EXCLUDED.net_revenue_paid_basis_cents,
    sessions_opened_count         = analytics_daily.sessions_opened_count         + EXCLUDED.sessions_opened_count,
    sessions_closed_count         = analytics_daily.sessions_closed_count         + EXCLUDED.sessions_closed_count,
    version                       = analytics_daily.version + 1,
    updated_at                    = now();
END;
$$ LANGUAGE plpgsql;
