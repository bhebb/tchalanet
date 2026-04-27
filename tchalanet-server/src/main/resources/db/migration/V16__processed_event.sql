-- V16__processed_event.sql
-- Table to track processed events per handler (idempotence for projectors)

CREATE TABLE processed_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  handler_key varchar(96) NOT NULL,
  event_id uuid NOT NULL,
  processed_at timestamptz NOT NULL DEFAULT now(),

  -- light audit
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,

  CONSTRAINT uq_processed_event UNIQUE (tenant_id, handler_key, event_id)
);

CREATE INDEX idx_processed_event_lookup
  ON processed_event (handler_key, event_id);
