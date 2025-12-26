-- V202: stats_event_log (global, no RLS)
-- Pas de CREATE EXTENSION ici (extensions dans V1)
CREATE TABLE IF NOT EXISTS stats_event_log (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type varchar(128) NOT NULL,
  payload jsonb NOT NULL,
  occurred_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_stats_event_log_type ON stats_event_log(event_type);
CREATE INDEX IF NOT EXISTS ix_stats_event_log_occurred ON stats_event_log(occurred_at);

