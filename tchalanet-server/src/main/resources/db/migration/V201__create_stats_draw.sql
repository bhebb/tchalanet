-- Migration V201: create stats_draw table
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS stats_draw (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  draw_id UUID NOT NULL UNIQUE,
  tenant_id UUID NOT NULL,
  game_code TEXT NOT NULL,
  scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,

  tickets_count BIGINT NOT NULL DEFAULT 0,
  stake_sum_cents BIGINT NOT NULL DEFAULT 0,
  winnings_sum_cents BIGINT NOT NULL DEFAULT 0,
  net_revenue_cents BIGINT NOT NULL DEFAULT 0,

  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stats_draw_scheduled_at ON stats_draw(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_stats_draw_tenant_id ON stats_draw(tenant_id);

