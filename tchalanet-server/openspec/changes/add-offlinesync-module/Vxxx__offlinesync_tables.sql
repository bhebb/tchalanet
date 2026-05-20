-- Skeleton migration for core.offlinesync.
-- This file is intentionally explicit but may need adaptation to current table names.

CREATE TABLE offline_grant (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  seller_user_id uuid NOT NULL,
  terminal_id uuid NOT NULL,
  outlet_id uuid NOT NULL,
  sales_session_id uuid NOT NULL,
  device_id varchar(128) NOT NULL,
  device_public_key text NOT NULL,
  key_id varchar(64) NOT NULL,
  valid_from timestamptz NOT NULL,
  valid_until timestamptz NOT NULL,
  sync_accepted_until timestamptz NOT NULL,
  max_ticket_count integer NOT NULL,
  max_total_amount_cents bigint NOT NULL,
  currency varchar(3) NOT NULL,
  consumed_ticket_count integer NOT NULL DEFAULT 0,
  consumed_total_amount_cents bigint NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT ck_offline_grant_windows CHECK (valid_from < valid_until AND valid_until < sync_accepted_until)
);

CREATE INDEX idx_offline_grant_current
  ON offline_grant (tenant_id, seller_user_id, terminal_id, device_id, status, valid_until);

CREATE TABLE offline_code_batch (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  batch_no varchar(64) NOT NULL,
  code_count integer NOT NULL,
  issued_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_offline_code_batch_no UNIQUE (tenant_id, batch_no)
);

CREATE TABLE offline_code (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  code_batch_id uuid NOT NULL REFERENCES offline_code_batch(id),
  code varchar(32) NOT NULL,
  status varchar(32) NOT NULL,
  reserved_at timestamptz NULL,
  consumed_at timestamptz NULL,
  offline_submission_id uuid NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_offline_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_offline_code_batch_status
  ON offline_code (tenant_id, code_batch_id, status);

CREATE TABLE offline_sync_batch (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  code_batch_id uuid NOT NULL REFERENCES offline_code_batch(id),
  client_batch_id varchar(128) NOT NULL,
  batch_payload_hash varchar(128) NOT NULL,
  received_at timestamptz NOT NULL,
  total_submission_count integer NOT NULL DEFAULT 0,
  accepted_count integer NOT NULL DEFAULT 0,
  rejected_count integer NOT NULL DEFAULT 0,
  duplicate_count integer NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_offline_sync_batch_client UNIQUE (tenant_id, grant_id, client_batch_id)
);

CREATE TABLE offline_submission (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  sync_batch_id uuid NOT NULL REFERENCES offline_sync_batch(id),
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  code_id uuid NOT NULL REFERENCES offline_code(id),
  offline_code varchar(32) NOT NULL,
  client_submission_id varchar(128) NOT NULL,
  payload_hash varchar(128) NOT NULL,
  client_sold_at timestamptz NOT NULL,
  received_at timestamptz NOT NULL,
  total_stake_amount_cents bigint NOT NULL,
  currency varchar(3) NOT NULL,
  line_count integer NOT NULL,
  status varchar(32) NOT NULL,
  rejection_code varchar(128) NULL,
  rejection_reason text NULL,
  promotion_attempt_id uuid NULL,
  promotion_requested_at timestamptz NULL,
  last_promotion_event_id uuid NULL,
  created_ticket_id uuid NULL,
  raw_payload jsonb NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_offline_submission_client UNIQUE (tenant_id, client_submission_id),
  CONSTRAINT uq_offline_submission_code UNIQUE (tenant_id, offline_code),
  CONSTRAINT ck_offline_submission_amount CHECK (total_stake_amount_cents >= 0),
  CONSTRAINT ck_offline_submission_line_count CHECK (line_count > 0)
);

CREATE INDEX idx_offline_submission_status
  ON offline_submission (tenant_id, status, received_at DESC);

CREATE INDEX idx_offline_submission_promotion_attempt
  ON offline_submission (tenant_id, promotion_attempt_id)
  WHERE promotion_attempt_id IS NOT NULL;

ALTER TABLE offline_code
  ADD CONSTRAINT fk_offline_code_submission
  FOREIGN KEY (offline_submission_id) REFERENCES offline_submission(id);

CREATE TABLE offline_submission_line (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  offline_submission_id uuid NOT NULL REFERENCES offline_submission(id),
  line_no integer NOT NULL,
  game_code varchar(64) NOT NULL,
  selection_json jsonb NOT NULL,
  stake_amount_cents bigint NOT NULL,
  potential_payout_cents bigint NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_offline_submission_line_no UNIQUE (tenant_id, offline_submission_id, line_no),
  CONSTRAINT ck_offline_submission_line_stake CHECK (stake_amount_cents > 0),
  CONSTRAINT ck_offline_submission_line_payout CHECK (potential_payout_cents IS NULL OR potential_payout_cents >= 0)
);

CREATE TABLE offline_submission_ticket_link (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  offline_submission_id uuid NOT NULL REFERENCES offline_submission(id),
  ticket_id uuid NULL,
  link_type varchar(32) NOT NULL,
  linked_at timestamptz NOT NULL,
  details_json jsonb NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_offline_submission_ticket_created
  ON offline_submission_ticket_link (tenant_id, offline_submission_id)
  WHERE link_type = 'CREATED';

CREATE TABLE offline_submission_decision (
  id uuid PRIMARY KEY,
  tenant_id uuid NOT NULL,
  offline_submission_id uuid NOT NULL REFERENCES offline_submission(id),
  decision_type varchar(32) NOT NULL,
  decided_by uuid NOT NULL,
  decided_at timestamptz NOT NULL,
  reason text NOT NULL,
  dry_run boolean NOT NULL DEFAULT false,
  report_json jsonb NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- RLS placeholders. Adapt policy helper functions to current project conventions.
ALTER TABLE offline_grant ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_code_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_sync_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_submission ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_submission_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_submission_ticket_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE offline_submission_decision ENABLE ROW LEVEL SECURITY;
