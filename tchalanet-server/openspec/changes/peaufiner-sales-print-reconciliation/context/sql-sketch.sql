-- SQL sketch only. Adapt version number and common audit conventions.

CREATE TABLE reconciliation_run (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  business_date date NOT NULL,
  run_type varchar(32) NOT NULL, -- SCHEDULED, FORCED
  status varchar(32) NOT NULL,
  forced boolean NOT NULL DEFAULT false,
  reason text NULL,
  started_at timestamptz NOT NULL,
  completed_at timestamptz NULL,
  checked_draw_count bigint NOT NULL DEFAULT 0,
  checked_ticket_count bigint NOT NULL DEFAULT 0,
  anomaly_count bigint NOT NULL DEFAULT 0,
  critical_count bigint NOT NULL DEFAULT 0,
  high_count bigint NOT NULL DEFAULT 0,
  medium_count bigint NOT NULL DEFAULT 0,
  low_count bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  version bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_reconciliation_run_tenant_date
  ON reconciliation_run (tenant_id, business_date DESC)
  WHERE deleted_at IS NULL;

CREATE TABLE reconciliation_anomaly (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  run_id uuid NOT NULL REFERENCES reconciliation_run(id),
  business_date date NOT NULL,
  severity varchar(32) NOT NULL,
  anomaly_type varchar(96) NOT NULL,
  status varchar(32) NOT NULL,
  fingerprint varchar(256) NOT NULL,
  draw_id uuid NULL,
  draw_channel_id uuid NULL,
  draw_result_id uuid NULL,
  ticket_id uuid NULL,
  ticket_code varchar(96) NULL,
  public_code varchar(96) NULL,
  display_code varchar(96) NULL,
  payout_claim_id uuid NULL,
  payout_payment_id uuid NULL,
  expected_status varchar(64) NULL,
  actual_status varchar(64) NULL,
  expected_amount numeric(19,2) NULL,
  actual_amount numeric(19,2) NULL,
  currency varchar(8) NULL,
  message text NOT NULL,
  details_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  first_seen_at timestamptz NOT NULL DEFAULT now(),
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  resolved_at timestamptz NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_reconciliation_anomaly_fingerprint UNIQUE (tenant_id, fingerprint)
);

CREATE INDEX idx_reconciliation_anomaly_run
  ON reconciliation_anomaly (tenant_id, run_id)
  WHERE deleted_at IS NULL;

CREATE INDEX idx_reconciliation_anomaly_status_severity
  ON reconciliation_anomaly (tenant_id, status, severity)
  WHERE deleted_at IS NULL;

-- Add triggers consistent with existing baseline:
-- CREATE TRIGGER trg_reconciliation_run__set_updated_at BEFORE UPDATE ON reconciliation_run
--   FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
-- CREATE TRIGGER trg_reconciliation_anomaly__set_updated_at BEFORE UPDATE ON reconciliation_anomaly
--   FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
