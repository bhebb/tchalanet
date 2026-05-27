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


-- Reconciliation tables (init-ops-reconciliation)

CREATE TABLE reconciliation_run (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  scope varchar(128) NOT NULL,
  business_date date,
  started_at timestamptz,
  completed_at timestamptz,
  status varchar(32),
  triggered_by varchar(128),
  triggered_by_user_id uuid,
  reason varchar(512),
  summary_json jsonb,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid
);

CREATE TABLE reconciliation_check_result (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  run_id uuid NOT NULL REFERENCES reconciliation_run(id),
  tenant_id uuid REFERENCES tenant(id),
  check_key varchar(256) NOT NULL,
  status varchar(32),
  severity varchar(32),
  expected_count bigint,
  actual_count bigint,
  anomaly_count bigint,
  summary_json jsonb,
  started_at timestamptz,
  completed_at timestamptz,
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
  run_id uuid NOT NULL REFERENCES reconciliation_run(id),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  check_key varchar(256) NOT NULL,
  anomaly_type varchar(128) NOT NULL,
  severity varchar(32),
  status varchar(32),
  resource_type varchar(128),
  resource_id uuid,
  related_resource_type varchar(128),
  related_resource_id uuid,
  message_key varchar(256),
  details_json jsonb,
  detected_at timestamptz NOT NULL DEFAULT now(),
  resolved_at timestamptz,
  resolved_by uuid,
  resolution_reason varchar(512),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE reconciliation_repair_action (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  anomaly_id uuid NOT NULL REFERENCES reconciliation_anomaly(id),
  run_id uuid NOT NULL REFERENCES reconciliation_run(id),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  action_type varchar(128) NOT NULL,
  status varchar(32),
  command_name varchar(256),
  command_payload_json jsonb,
  executed_at timestamptz,
  executed_by uuid,
  failure_message varchar(1024),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

-- Indexes to speed common lookups
CREATE INDEX idx_reconciliation_run_tenant_businessdate ON reconciliation_run(tenant_id, business_date);
CREATE INDEX idx_reconciliation_check_result_run_key ON reconciliation_check_result(run_id, check_key);
CREATE INDEX idx_reconciliation_anomaly_run_key ON reconciliation_anomaly(run_id, check_key);
CREATE INDEX idx_reconciliation_anomaly_tenant_detected ON reconciliation_anomaly(tenant_id, detected_at);
CREATE INDEX idx_reconciliation_repair_action_anomaly ON reconciliation_repair_action(anomaly_id);

