-- V203: stats_daily (global or tenant-scoped by dimension)
CREATE TABLE IF NOT EXISTS stats_daily (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dimension_type text NOT NULL,
  dimension_id uuid,
  ref_date date NOT NULL,
  tickets_count bigint NOT NULL DEFAULT 0,
  tickets_cancelled_count bigint NOT NULL DEFAULT 0,
  stake_sum_cents bigint NOT NULL DEFAULT 0,
  winnings_sum_cents bigint NOT NULL DEFAULT 0,
  net_revenue_cents bigint NOT NULL DEFAULT 0,
  payouts_count bigint NOT NULL DEFAULT 0,
  sessions_opened_count bigint NOT NULL DEFAULT 0,
  sessions_closed_count bigint NOT NULL DEFAULT 0,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_stats_daily_dim_date
  ON stats_daily(dimension_type, dimension_id, ref_date);
CREATE INDEX IF NOT EXISTS idx_stats_daily_ref_date ON stats_daily(ref_date);

