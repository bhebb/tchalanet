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
  response_json jsonb,
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

