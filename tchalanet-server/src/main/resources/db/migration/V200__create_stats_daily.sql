-- Migration V200: create stats_daily table
-- Uses pgcrypto gen_random_uuid(); ensure extension exists
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS stats_daily (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  dimension_type TEXT NOT NULL,
  dimension_id UUID,
  ref_date DATE NOT NULL,

  tickets_count BIGINT NOT NULL DEFAULT 0,
  tickets_cancelled_count BIGINT NOT NULL DEFAULT 0,
  stake_sum_cents BIGINT NOT NULL DEFAULT 0,
  winnings_sum_cents BIGINT NOT NULL DEFAULT 0,
  net_revenue_cents BIGINT NOT NULL DEFAULT 0,
  payouts_count BIGINT NOT NULL DEFAULT 0,

  sessions_opened_count BIGINT NOT NULL DEFAULT 0,
  sessions_closed_count BIGINT NOT NULL DEFAULT 0,

  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_stats_daily_dim_date
  ON stats_daily(dimension_type, dimension_id, ref_date);

CREATE INDEX IF NOT EXISTS idx_stats_daily_ref_date ON stats_daily(ref_date);

