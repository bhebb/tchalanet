-- Canonical draft schema. Adapt naming/types to current project conventions.

CREATE TABLE outlet (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  name text NOT NULL,
  slug citext NOT NULL,
  day_closed boolean NOT NULL DEFAULT false,
  sales_blocked boolean NOT NULL DEFAULT false,
  sales_block_reason text,
  sales_blocked_at timestamptz,
  timezone varchar(64) NOT NULL DEFAULT 'America/Port-au-Prince',
  business_day_cutoff time,
  receipt_printing_enabled boolean NOT NULL DEFAULT true,
  receipt_header_message text,
  receipt_footer_message text,
  require_opening_float boolean NOT NULL DEFAULT true,
  auto_open_session boolean NOT NULL DEFAULT false,
  auto_close_session boolean NOT NULL DEFAULT false,
  address_id uuid REFERENCES address(id),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_outlet__tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE outlet_user_assignment (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  role_at_outlet varchar(32),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_outlet_user_assignment__tenant_outlet_user UNIQUE (tenant_id, outlet_id, user_id)
);

CREATE TABLE terminal (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  assigned_user_id uuid REFERENCES app_user(id),
  kind varchar(16) NOT NULL DEFAULT 'PHYSICAL',
  state varchar(32) NOT NULL,
  active_for_user boolean NOT NULL DEFAULT false,
  last_seen timestamptz,
  label varchar(128),
  inventory_tag varchar(64),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  sync_state varchar(32) NOT NULL DEFAULT 'ONLINE',
  registered_at timestamptz,
  unregistered_at timestamptz,
  locked_at timestamptz,
  locked_by uuid,
  lock_reason text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_terminal__kind CHECK (kind IN ('PHYSICAL','VIRTUAL')),
  CONSTRAINT chk_terminal__state CHECK (state IN ('REGISTERED','ACTIVE','LOCKED','OFFLINE','UNREGISTERED')),
  CONSTRAINT chk_terminal__sync_state CHECK (sync_state IN ('ONLINE','OFFLINE','SYNC_PENDING','SYNC_CONFLICT'))
);

CREATE UNIQUE INDEX uq_terminal__one_active_per_user
  ON terminal(tenant_id, assigned_user_id)
  WHERE assigned_user_id IS NOT NULL AND active_for_user = true AND deleted_at IS NULL;

CREATE TABLE sales_session (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  terminal_id uuid REFERENCES terminal(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  status varchar(16) NOT NULL,
  source varchar(16) NOT NULL DEFAULT 'MANUAL',
  opened_at timestamptz NOT NULL DEFAULT now(),
  closed_at timestamptz,
  opening_float_cents bigint,
  closing_amount_cents bigint,
  meta jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_sales_session__status CHECK (status IN ('OPEN','CLOSED','RECONCILED')),
  CONSTRAINT chk_sales_session__source CHECK (source IN ('MANUAL','SCHEDULER','OPS'))
);

CREATE UNIQUE INDEX uq_sales_session__one_open_per_user
  ON sales_session(tenant_id, user_id)
  WHERE status = 'OPEN' AND deleted_at IS NULL;

CREATE TABLE sales_session_totals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  session_id uuid NOT NULL REFERENCES sales_session(id),
  total_tickets bigint NOT NULL DEFAULT 0,
  total_stake_cents bigint NOT NULL DEFAULT 0,
  total_payout_cents bigint NOT NULL DEFAULT 0,
  gross_margin_cents bigint NOT NULL DEFAULT 0,
  last_recomputed_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_sales_session_totals__session UNIQUE (session_id)
);

CREATE TABLE ticket (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  terminal_id uuid REFERENCES terminal(id),
  draw_id uuid NOT NULL REFERENCES draw(id),
  session_id uuid NOT NULL REFERENCES sales_session(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  ticket_code text NOT NULL,
  public_code varchar(32) NOT NULL,
  sale_status varchar(24) NOT NULL,
  result_status varchar(24) NOT NULL,
  settlement_status varchar(24) NOT NULL,
  currency varchar(8) NOT NULL,
  total_amount_cents bigint NOT NULL,
  winning_amount_cents bigint,
  resulted_at timestamptz,
  approval_request_id uuid,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_ticket__tenant_code UNIQUE (tenant_id, ticket_code),
  CONSTRAINT uq_ticket__public_code UNIQUE (public_code)
);

CREATE TABLE ticket_line (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  ticket_id uuid NOT NULL REFERENCES ticket(id),
  game_code varchar(32) NOT NULL,
  selection varchar(32) NOT NULL,
  stake_cents bigint NOT NULL,
  odds_snapshot numeric(12,4) NOT NULL,
  potential_payout_cents bigint NOT NULL,
  bet_option smallint,
  bet_type varchar(32) NOT NULL,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE payout (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  ticket_id uuid NOT NULL REFERENCES ticket(id),
  selling_outlet_id uuid NOT NULL REFERENCES outlet(id),
  selling_session_id uuid NOT NULL REFERENCES sales_session(id),
  paying_outlet_id uuid REFERENCES outlet(id),
  paying_session_id uuid REFERENCES sales_session(id),
  terminal_id uuid REFERENCES terminal(id),
  paid_by_user_id uuid REFERENCES app_user(id),
  amount_cents bigint NOT NULL,
  currency varchar(3) NOT NULL,
  status varchar(24) NOT NULL,
  approved_at timestamptz,
  paid_at timestamptz,
  rejected_at timestamptz,
  rejected_reason text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_payout__status CHECK (status IN ('REQUESTED','APPROVED','PAID','REJECTED','CANCELLED'))
);

CREATE TABLE limit_definition (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_key varchar(64) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  on_breach varchar(32) NOT NULL,
  params jsonb NOT NULL DEFAULT '{}'::jsonb,
  applies_to jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_limit_definition__rule_key UNIQUE (rule_key)
);

CREATE TABLE limit_assignment (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  limit_definition_id uuid NOT NULL REFERENCES limit_definition(id),
  target_type varchar(16) NOT NULL,
  target_id uuid,
  enabled boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  params_override jsonb NOT NULL DEFAULT '{}'::jsonb,
  applies_to_override jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_limit_assignment__target_type CHECK (target_type IN ('TENANT','OUTLET','USER'))
);

CREATE UNIQUE INDEX uq_limit_assignment__natural
  ON limit_assignment(tenant_id, limit_definition_id, target_type, COALESCE(target_id, '00000000-0000-0000-0000-000000000000'::uuid))
  WHERE deleted_at IS NULL;

CREATE TABLE autonomy_policy_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  target_type varchar(16) NOT NULL,
  target_id uuid,
  level varchar(32) NOT NULL,
  require_approval_on_block boolean NOT NULL DEFAULT true,
  approval_role varchar(64),
  enabled boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_autonomy_policy_rule__target_type CHECK (target_type IN ('TENANT','OUTLET','USER'))
);

CREATE UNIQUE INDEX uq_autonomy_policy_rule__natural
  ON autonomy_policy_rule(tenant_id, target_type, COALESCE(target_id, '00000000-0000-0000-0000-000000000000'::uuid))
  WHERE deleted_at IS NULL;

CREATE TABLE approval_request (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  entity_type varchar(32) NOT NULL,
  entity_id uuid NOT NULL,
  reason_code varchar(64) NOT NULL,
  status varchar(16) NOT NULL,
  requested_by uuid,
  approved_by uuid,
  rejected_by uuid,
  requested_at timestamptz NOT NULL DEFAULT now(),
  decided_at timestamptz,
  details jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_approval_request__status CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED'))
);
