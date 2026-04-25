-- V201: stats_draw (tenant-scoped, RLS groupe B)
-- Pas de CREATE EXTENSION ici (extensions dans V1)
CREATE TABLE IF NOT EXISTS stats_draw (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  draw_id uuid NOT NULL,
  channel_code varchar(64) NOT NULL,
  draw_date date NOT NULL,
  sales_amount numeric(18,2) NOT NULL DEFAULT 0,
  tickets_count bigint NOT NULL DEFAULT 0,
  payouts_amount numeric(18,2) NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_stats_draw_tenant_date ON stats_draw(tenant_id, draw_date);
CREATE INDEX IF NOT EXISTS ix_stats_draw_draw_id ON stats_draw(draw_id);


