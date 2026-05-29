-- Baseline: technical runtime tables
CREATE TABLE processed_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  handler_key varchar(160) NOT NULL,
  event_id uuid NOT NULL,
  processed_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  CONSTRAINT uq_processed_event__tenant_handler_event UNIQUE (tenant_id, handler_key, event_id)
);

CREATE TABLE idempotency_record (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  scope varchar(80) NOT NULL,
  idem_key varchar(200) NOT NULL,
  request_hash varchar(64) NOT NULL,
  status varchar(20) NOT NULL,
  resource_id uuid,
  response_json text,
  expires_at timestamptz NOT NULL,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_idempotency_record__tenant_scope_key UNIQUE (tenant_id, scope, idem_key)
);

CREATE TABLE stats_draw (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  draw_id uuid NOT NULL UNIQUE REFERENCES draw(id),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  game_code varchar(255) NOT NULL,
  scheduled_at timestamptz NOT NULL,
  tickets_count bigint NOT NULL DEFAULT 0,
  stake_sum_cents bigint NOT NULL DEFAULT 0,
  winnings_sum_cents bigint NOT NULL DEFAULT 0,
  net_revenue_cents bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid
);

CREATE TABLE stats_daily (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dimension_type varchar(255) NOT NULL,
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
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid
);

CREATE TABLE stats_event_log (
  event_id uuid PRIMARY KEY,
  event_type varchar(255) NOT NULL,
  processed_at timestamptz NOT NULL
);

CREATE TABLE shedlock (
  name varchar(64) PRIMARY KEY,
  lock_until timestamptz NOT NULL,
  locked_at timestamptz NOT NULL,
  locked_by varchar(255) NOT NULL
);


-- Reconciliation tables

CREATE TABLE reconciliation_run (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  business_date date NOT NULL,
  run_type varchar(32) NOT NULL,
  status varchar(32) NOT NULL,
  forced boolean NOT NULL DEFAULT false,
  reason text,
  started_at timestamptz NOT NULL,
  completed_at timestamptz,
  checked_draw_count bigint NOT NULL DEFAULT 0,
  checked_ticket_count bigint NOT NULL DEFAULT 0,
  anomaly_count bigint NOT NULL DEFAULT 0,
  critical_count bigint NOT NULL DEFAULT 0,
  high_count bigint NOT NULL DEFAULT 0,
  medium_count bigint NOT NULL DEFAULT 0,
  low_count bigint NOT NULL DEFAULT 0,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid
);

CREATE TABLE reconciliation_anomaly (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  run_id uuid NOT NULL REFERENCES reconciliation_run(id),
  business_date date NOT NULL,
  severity varchar(32) NOT NULL,
  anomaly_type varchar(96) NOT NULL,
  status varchar(32) NOT NULL,
  fingerprint varchar(256) NOT NULL,
  draw_id uuid,
  draw_channel_id uuid,
  draw_result_id uuid,
  ticket_id uuid,
  ticket_code varchar(96),
  public_code varchar(96),
  display_code varchar(96),
  payout_claim_id uuid,
  payout_payment_id uuid,
  expected_status varchar(64),
  actual_status varchar(64),
  expected_amount numeric(19,2),
  actual_amount numeric(19,2),
  currency varchar(8),
  message text NOT NULL,
  details_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  first_seen_at timestamptz NOT NULL DEFAULT now(),
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  resolved_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_reconciliation_anomaly_fingerprint UNIQUE (tenant_id, fingerprint)
);

CREATE INDEX idx_reconciliation_run_tenant_date ON reconciliation_run(tenant_id, business_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_reconciliation_anomaly_run ON reconciliation_anomaly(tenant_id, run_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reconciliation_anomaly_status_severity ON reconciliation_anomaly(tenant_id, status, severity) WHERE deleted_at IS NULL;
