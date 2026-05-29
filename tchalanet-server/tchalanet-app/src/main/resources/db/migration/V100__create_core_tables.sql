-- Baseline: core business tables only
CREATE TABLE tenant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(64) NOT NULL UNIQUE,
  name varchar(255) NOT NULL,
  type varchar(32) NOT NULL DEFAULT 'PERSONAL',
  timezone varchar(64) NOT NULL DEFAULT 'UTC',
  currency varchar(3) NOT NULL DEFAULT 'USD',
  status varchar(32) NOT NULL DEFAULT 'DRAFT',
  address_id uuid,
  config jsonb NOT NULL DEFAULT '{}'::jsonb,
  active_theme_id uuid,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_tenant__status CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','REJECTED','ARCHIVED'))
);

CREATE TABLE address (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  line1 varchar(256) NOT NULL,
  line2 varchar(256),
  city varchar(128) NOT NULL,
  region varchar(128),
  country varchar(2) NOT NULL,
  postal_code varchar(16),
  normalized_key varchar(64) NOT NULL,
  deleted boolean NOT NULL DEFAULT false,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_address__tenant_normalized UNIQUE (tenant_id, normalized_key)
);

ALTER TABLE tenant ADD CONSTRAINT fk_tenant__address FOREIGN KEY (address_id) REFERENCES address(id);

CREATE TABLE app_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  keycloak_sub uuid NOT NULL,
  username text,
  email citext,
  phone text,
  first_name text,
  last_name text,
  display_name text,
  avatar_url text,
  status varchar(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
  approved_at timestamptz,
  approved_by uuid,
  last_login_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_app_user__status CHECK (status IN ('INVITED','PENDING_APPROVAL','ACTIVE','SUSPENDED'))
);

CREATE TABLE user_preference (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id),
  theme_mode varchar(10),
  density smallint,
  locale varchar(8),
  time_zone varchar(64),
  currency varchar(3),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_user_preference__user UNIQUE (user_id)
);

CREATE TABLE permission (
  code varchar(128) PRIMARY KEY,
  name varchar(128) NOT NULL,
  category varchar(64),
  description text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE app_role (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  description text,
  parent_role_id uuid REFERENCES app_role(id),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE role_permission (
  role_id uuid NOT NULL REFERENCES app_role(id),
  permission_code varchar(128) NOT NULL REFERENCES permission(code),
  PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE tenant_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  role_id uuid REFERENCES app_role(id),
  status varchar(32),
  is_owner boolean DEFAULT false,
  outlet_id uuid,
  terminal_id uuid,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_tenant_user__tenant_user UNIQUE (tenant_id, user_id)
);

CREATE TABLE app_setting (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  outlet_id uuid,
  terminal_id uuid,
  namespace varchar(255) NOT NULL,
  setting_key varchar(255) NOT NULL,
  setting_value text NOT NULL,
  value_type varchar(50) NOT NULL,
  level varchar(50) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_app_setting__level CHECK (level IN ('GLOBAL','TENANT','OUTLET','TERMINAL')),
  CONSTRAINT chk_app_setting__value_type CHECK (value_type IN ('STRING','INT','LONG','DECIMAL','BOOLEAN','JSON'))
);

CREATE TABLE i18n_override (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  level varchar(32) NOT NULL DEFAULT 'TENANT',
  tenant_id uuid,
  locale varchar(10) NOT NULL,
  i18n_key varchar(255) NOT NULL,
  i18n_value text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_i18n_override__level CHECK (level IN ('GLOBAL','TENANT'))
);

CREATE TABLE theme_preset (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  vendor varchar(128),
  config text NOT NULL,
  label_key varchar(255),
  active boolean NOT NULL DEFAULT true,
  is_default boolean NOT NULL DEFAULT false,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE tenant_theme (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  preset_code varchar(128) NOT NULL,
  metadata jsonb,
  is_default boolean NOT NULL DEFAULT false,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE game (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(32) NOT NULL UNIQUE,
  name varchar(128) NOT NULL,
  category varchar(32) NOT NULL,
  combination varchar(32) NOT NULL,
  min_digits integer NOT NULL,
  max_digits integer NOT NULL,
  description text,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE tenant_game (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  game_id uuid NOT NULL REFERENCES game(id),
  enabled boolean NOT NULL DEFAULT true,
  display_name varchar(128),
  min_stake numeric(12,2),
  max_stake numeric(12,2),
  flags jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_tenant_game__tenant_game UNIQUE (tenant_id, game_id)
);

CREATE TABLE result_slot (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  slot_key varchar(32) NOT NULL UNIQUE,
  provider varchar(16) NOT NULL,
  timezone varchar(64) NOT NULL,
  draw_time time NOT NULL,
  days_of_week varchar(32) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  source_cfg jsonb NOT NULL DEFAULT '{}'::jsonb,
  projection_cfg jsonb NOT NULL DEFAULT '{}'::jsonb,
  notes text,
  label_key varchar(256),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE draw_channel (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  period varchar(32),
  timezone varchar(64) NOT NULL,
  draw_time time NOT NULL,
  sales_open_time time,
  cutoff_sec integer NOT NULL DEFAULT 120,
  days_of_week varchar(32) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order integer NOT NULL DEFAULT 0,
  flags jsonb NOT NULL DEFAULT '{}'::jsonb,
  notes text,
  result_slot_id uuid NOT NULL REFERENCES result_slot(id),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_draw_channel__tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE draw_channel_game (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  draw_channel_id uuid NOT NULL REFERENCES draw_channel(id),
  game_id uuid NOT NULL REFERENCES game(id),
  enabled boolean NOT NULL DEFAULT true,
  flags jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_draw_channel_game__tenant_channel_game UNIQUE (tenant_id, draw_channel_id, game_id)
);

CREATE TABLE draw_result (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  result_slot_id uuid NOT NULL REFERENCES result_slot(id),
  occurred_at timestamptz NOT NULL,
  source_result jsonb NOT NULL,
  haiti_result jsonb NOT NULL,
  raw_payload jsonb,
  flags jsonb NOT NULL DEFAULT '{}'::jsonb,
  status varchar(16) NOT NULL DEFAULT 'PROVISIONAL',
  quality varchar(16),
  source varchar(32),
  source_hash varchar(64),
  fetched_at timestamptz NOT NULL DEFAULT now(),
  override_reason text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_draw_result__status CHECK (status IN ('PROVISIONAL','CONFIRMED','OVERRIDDEN','ERROR')),
  CONSTRAINT uq_draw_result__slot_occurred UNIQUE (result_slot_id, occurred_at)
);

CREATE TABLE draw (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  draw_channel_id uuid NOT NULL REFERENCES draw_channel(id),
  draw_date date NOT NULL,
  scheduled_at timestamptz NOT NULL,
  cutoff_at timestamptz NOT NULL,
  opened_at timestamptz,
  closed_at timestamptz,
  resulted_at timestamptz,
  settled_at timestamptz,
  canceled_at timestamptz,
  cancel_reason text,
  status varchar(16) NOT NULL,
  draw_result_id uuid REFERENCES draw_result(id),
  system_generated boolean NOT NULL DEFAULT true,
  locked boolean NOT NULL DEFAULT false,
  result_source varchar(16),
  result_override_reason text,
  result_overridden_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_draw__status CHECK (status IN ('SCHEDULED','OPEN','CLOSED','RESULTED','SETTLED','CANCELED','ARCHIVED'))
);

-- =========================================================
-- SALES ZONE
-- =========================================================

CREATE TABLE sales_zone (
  id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   uuid         NOT NULL REFERENCES tenant(id),
  code        varchar(80)  NOT NULL,
  label       varchar(160) NOT NULL,
  active      boolean      NOT NULL DEFAULT true,
  parent_id   uuid         NULL REFERENCES sales_zone(id),
  created_at  timestamptz  NOT NULL DEFAULT now(),
  created_by  uuid         NULL,
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  updated_by  uuid         NULL,
  deleted_at  timestamptz  NULL,
  deleted_by  uuid         NULL,
  version     bigint       NOT NULL DEFAULT 0,
  CONSTRAINT uq_sales_zone_tenant_code UNIQUE (tenant_id, code)
);

-- =========================================================
-- OUTLET
-- =========================================================

CREATE TABLE outlet (
  id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   uuid         NOT NULL,

  name        varchar(255) NOT NULL,
  slug        varchar(128) NOT NULL,

  -- Classification
  kind        varchar(40)  NOT NULL DEFAULT 'OWNED_SHOP',
  partner_ref varchar(120) NULL,
  zone_id     uuid         NULL REFERENCES sales_zone(id),
  metadata_json jsonb      NULL,

  -- Day / lifecycle
  day_closed  boolean      NOT NULL DEFAULT false,
  status      varchar(40)  NOT NULL DEFAULT 'DRAFT',

  -- Outlet-level block (global override — blocks everything)
  outlet_blocked      boolean      NOT NULL DEFAULT false,
  outlet_block_reason text         NULL,
  outlet_blocked_at   timestamptz  NULL,
  outlet_blocked_by   uuid         NULL,

  -- Sales block
  sales_blocked       boolean      NOT NULL DEFAULT false,
  sales_block_reason  text         NULL,
  sales_blocked_at    timestamptz  NULL,
  sales_blocked_by    uuid         NULL,

  -- Payout block
  payout_blocked      boolean      NOT NULL DEFAULT false,
  payout_block_reason text         NULL,
  payout_blocked_at   timestamptz  NULL,
  payout_blocked_by   uuid         NULL,

  -- Offline sales block
  offline_sales_blocked      boolean     NOT NULL DEFAULT false,
  offline_sales_block_reason text        NULL,
  offline_sales_blocked_at   timestamptz NULL,
  offline_sales_blocked_by   uuid        NULL,

  -- Config
  timezone                   varchar(64) NOT NULL DEFAULT 'America/Port-au-Prince',
  receipt_printing_enabled   boolean     NOT NULL DEFAULT true,
  receipt_header_message     text        NULL,
  receipt_footer_message     text        NULL,
  require_opening_float      boolean     NOT NULL DEFAULT true,
  auto_session_open_enabled  boolean     NOT NULL DEFAULT false,
  auto_session_close_enabled boolean     NOT NULL DEFAULT false,
  session_open_time          time        NULL,
  session_close_time         time        NULL,
  default_opening_float_cents bigint     NULL,

  address_id  uuid         NULL,

  created_at  timestamptz  NOT NULL DEFAULT now(),
  created_by  uuid         NULL,
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  updated_by  uuid         NULL,
  deleted_at  timestamptz  NULL,
  deleted_by  uuid         NULL,
  version     bigint       NOT NULL DEFAULT 0,

  CONSTRAINT ck_outlet_kind CHECK (kind IN (
    'OWNED_SHOP','KIOSK','MOBILE_POINT','BANK_BRANCH',
    'PARTNER_INSTITUTION','PARTNER_TENANT','REGIONAL_OFFICE')),
  CONSTRAINT ck_outlet_status CHECK (status IN (
    'DRAFT','ACTIVE','SUSPENDED','CLOSED','ARCHIVED'))
);

COMMENT ON COLUMN outlet.kind IS 'Outlet classification: OWNED_SHOP, KIOSK, MOBILE_POINT, BANK_BRANCH, PARTNER_INSTITUTION, PARTNER_TENANT, REGIONAL_OFFICE. Immutable after creation.';
COMMENT ON COLUMN outlet.partner_ref IS 'External commercial reference (distributor code, commerce code, imported ref). Unique per tenant.';
COMMENT ON COLUMN outlet.outlet_blocked IS 'Global operational block — overrides sales/payout/offline blocks. Use for emergencies or audits.';
COMMENT ON COLUMN outlet.day_closed IS 'Closes the outlet operationally for the current business day (POS concept).';
COMMENT ON COLUMN outlet.timezone IS 'Local timezone used for business date and auto-session scheduling.';
comment on column outlet.auto_session_close_enabled is 'Enables automatic closing of open sessions.';
comment on column outlet.session_open_time is 'Local outlet time used by auto-open scheduler.';
comment on column outlet.session_close_time is 'Local outlet time used by auto-close scheduler.';
comment on column outlet.default_opening_float_cents is 'Default opening float used for automatic session creation.';

-- =========================================================
-- TERMINAL
-- =========================================================

create table terminal (
                          id uuid primary key default gen_random_uuid(),
                          tenant_id uuid not null,

                          outlet_id uuid not null,
                          assigned_user_id uuid,

                          kind varchar(16) not null default 'PHYSICAL',
                          state varchar(32) not null,
                          auto_session_enabled boolean not null default false,

                          sync_state varchar(32) not null default 'ONLINE',
                          last_seen timestamptz,

                          label varchar(128),
                          inventory_tag varchar(64),
                          metadata jsonb not null default '{}'::jsonb,

                          registered_at timestamptz,
                          unregistered_at timestamptz,

                          locked_at timestamptz,
                          locked_by uuid,
                          lock_reason text,

                          created_at timestamptz not null,
                          created_by uuid,
                          updated_at timestamptz not null,
                          updated_by uuid,
                          deleted_at timestamptz,
                          deleted_by uuid,
                          version bigint not null default 0,

                          value varchar(80),
                          sales_blocked boolean not null default false,
                          sales_block_reason text,
                          sales_blocked_at timestamptz,
                          sales_blocked_by uuid,
                          payout_blocked boolean not null default false,
                          payout_block_reason text,
                          payout_blocked_at timestamptz,
                          payout_blocked_by uuid,
                          offline_blocked boolean not null default false,
                          offline_block_reason text,
                          offline_blocked_at timestamptz,
                          offline_blocked_by uuid
);

comment on column terminal.assigned_user_id is 'User/agent currently assigned to this terminal.';
comment on column terminal.kind is 'Terminal kind: PHYSICAL POS or VIRTUAL phone/device.';
comment on column terminal.auto_session_enabled is 'Whether this terminal is eligible for auto-session.';
comment on column terminal.state is 'Operational state of the terminal.';
comment on column terminal.sync_state is 'Connectivity/sync state for POS/mobile clients.';
comment on column terminal.metadata is 'Flexible device metadata stored as JSONB.';

create table terminal_capability (
                                     id uuid primary key default gen_random_uuid(),
                                     tenant_id uuid not null,
                                     terminal_id uuid not null references terminal(id),
                                     capability varchar(64) not null,

                                     created_at timestamptz not null,
                                     created_by uuid,
                                     updated_at timestamptz not null,
                                     updated_by uuid,
                                     deleted_at timestamptz,
                                     deleted_by uuid,
                                     version bigint not null default 0,

                                     constraint uq_terminal_capability__tenant_terminal_capability
                                       unique (tenant_id, terminal_id, capability)
);

create table terminal_assignment (
                                     id uuid primary key default gen_random_uuid(),
                                     tenant_id uuid not null,
                                     terminal_id uuid not null references terminal(id),
                                     user_id uuid not null references app_user(id),
                                     status varchar(32) not null,
                                     assigned_at timestamptz not null,
                                     revoked_at timestamptz,

                                     created_at timestamptz not null,
                                     created_by uuid,
                                     updated_at timestamptz not null,
                                     updated_by uuid,
                                     deleted_at timestamptz,
                                     deleted_by uuid,
                                     version bigint not null default 0
);

create table terminal_binding (
                                  id uuid primary key default gen_random_uuid(),
                                  tenant_id uuid not null,
                                  terminal_id uuid not null references terminal(id),
                                  binding_type varchar(32) not null,
                                  status varchar(32) not null,
                                  binding_public_key text,
                                  binding_secret_hash text,
                                  device_fingerprint_hash text,
                                  bound_at timestamptz not null,
                                  expires_at timestamptz,
                                  revoked_at timestamptz,
                                  last_seen_at timestamptz,

                                  created_at timestamptz not null,
                                  created_by uuid,
                                  updated_at timestamptz not null,
                                  updated_by uuid,
                                  deleted_at timestamptz,
                                  deleted_by uuid,
                                  version bigint not null default 0
);

create table terminal_challenge (
                                    id uuid primary key default gen_random_uuid(),
                                    tenant_id uuid not null,
                                    terminal_id uuid not null references terminal(id),
                                    user_id uuid not null references app_user(id),
                                    challenge_type varchar(32) not null,
                                    channel varchar(32) not null,
                                    code_hash text not null,
                                    expires_at timestamptz not null,
                                    attempt_count integer not null default 0,
                                    max_attempts integer not null,
                                    status varchar(32) not null,
                                    consumed_at timestamptz,
                                    cancelled_at timestamptz,

                                    created_at timestamptz not null,
                                    created_by uuid,
                                    updated_at timestamptz not null,
                                    updated_by uuid,
                                    deleted_at timestamptz,
                                    deleted_by uuid,
                                    version bigint not null default 0
);

comment on table terminal_capability is 'Authorized/capable actions for a terminal. User permissions remain a separate gate.';
comment on table terminal_assignment is 'Terminal-to-user assignment lifecycle. Outlet remains owned by terminal/session.';
comment on table terminal_binding is 'Trusted device/app binding for a terminal. Secrets and fingerprints are stored only as hashes.';
comment on table terminal_challenge is 'Short-lived activation proof. Clear challenge codes are never stored.';

-- =========================================================
-- SALES SESSION
-- =========================================================

create table sales_session (
                               id uuid primary key default gen_random_uuid(),
                               tenant_id uuid not null,

                               outlet_id uuid not null,
                               terminal_id uuid not null,

                               opened_by uuid not null,
                               opened_at timestamptz not null,
                               business_date date not null,

                               status varchar(32) not null,

                               closed_by uuid,
                               closed_at timestamptz,
                               close_reason text,

                               opening_float_cents bigint,
                               expected_closing_amount_cents bigint,
                               declared_closing_amount_cents bigint,
                               variance_cents bigint,

                               finalized_at timestamptz,
                               finalized_by uuid,
                               finalize_reason text,

                               created_at timestamptz not null,
                               created_by uuid,
                               updated_at timestamptz not null,
                               updated_by uuid,
                               deleted_at timestamptz,
                               deleted_by uuid,
                               version bigint not null default 0
);

comment on column sales_session.business_date is 'Local business date based on outlet timezone at session opening.';
comment on column sales_session.opening_float_cents is 'Initial float for the session.';
comment on column sales_session.expected_closing_amount_cents is 'System-calculated expected closing cash.';
comment on column sales_session.declared_closing_amount_cents is 'Cash amount declared by user during manual close; null for auto-close.';
comment on column sales_session.variance_cents is 'Declared closing minus expected closing.';

CREATE TABLE sales_session_offline_adjustment (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  sales_session_id uuid NOT NULL REFERENCES sales_session(id),
  ticket_id uuid NOT NULL,
  amount_cents bigint NOT NULL,
  currency varchar(3) NOT NULL,
  source varchar(64) NOT NULL,
  reason varchar(255) NOT NULL,
  occurred_at_device timestamptz NOT NULL,
  recorded_at timestamptz NOT NULL,
  version bigint NOT NULL DEFAULT 0
);


CREATE TABLE pricing_odds (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  game_code varchar(32) NOT NULL,
  bet_type varchar(32) NOT NULL,
  bet_option smallint,
  odds numeric(12,4) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_pricing_odds__tenant_game_bet UNIQUE (tenant_id, game_code, bet_type, bet_option)
);


-- =========================================================
-- PAYOUT
-- =========================================================

create table payout (
  id              uuid         primary key default gen_random_uuid(),
  tenant_id       uuid         not null,

  -- claim identity
  ticket_id       uuid         not null,
  draw_id         uuid,
  source_event_id uuid,
  source          varchar(32)  not null,

  amount_cents    bigint       not null,
  currency        varchar(3)   not null default 'HTG',
  status          varchar(32)  not null,

  -- claim opening
  opened_at       timestamptz  not null,

  -- selling context (immutable, set at claim creation)
  selling_outlet_id  uuid,
  selling_session_id uuid,

  -- payment execution
  paying_outlet_id   uuid,
  paying_session_id  uuid,
  paying_terminal_id uuid,
  paid_by            uuid,
  paid_at            timestamptz,

  -- block (admin correction)
  blocked_by    uuid,
  blocked_at    timestamptz,
  block_reason  text,

  -- cancel (admin correction)
  cancelled_by  uuid,
  cancelled_at  timestamptz,
  cancel_reason text,

  -- reversal (admin correction)
  reversed_by   uuid,
  reversed_at   timestamptz,
  reverse_reason text,

  -- base audit columns (BaseTenantEntity / AuditableEntity)
  created_at  timestamptz not null,
  created_by  uuid,
  updated_at  timestamptz not null,
  updated_by  uuid,
  deleted_at  timestamptz,
  deleted_by  uuid,
  version     bigint      not null default 0,

  -- one claim per winning ticket per tenant (idempotence guard)
  constraint uq_payout_tenant_ticket unique (tenant_id, ticket_id)
);

comment on column payout.ticket_id       is 'Winning ticket this claim covers.';
comment on column payout.draw_id         is 'Draw that produced the winning ticket.';
comment on column payout.source_event_id is 'EventId of the TicketWinningSettlementCreatedEvent that triggered this claim.';
comment on column payout.source          is 'Claim origin: SALES_SETTLEMENT | OPS_RECONCILIATION | MANUAL_ADMIN_CORRECTION.';
comment on column payout.opened_at       is 'Timestamp when the claim was first opened.';
comment on column payout.status          is 'Claim lifecycle status: OPEN | BLOCKED | PAID | CANCELLED | REVERSED.';
comment on column payout.selling_session_id  is 'Session that sold the original ticket.';
comment on column payout.paying_session_id   is 'Session that paid the winning ticket.';
comment on column payout.block_reason        is 'Reason provided when an admin blocks the claim.';
comment on column payout.cancel_reason       is 'Reason provided when the claim is cancelled.';
comment on column payout.reverse_reason      is 'Reason provided when a paid claim is reversed.';


CREATE TABLE billing_plan (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  name varchar(255) NOT NULL,
  description text,
  price_amount numeric(19,2) NOT NULL DEFAULT 0,
  currency varchar(3) NOT NULL DEFAULT 'USD',
  billing_period varchar(50) NOT NULL DEFAULT 'MONTHLY',
  limits_json jsonb,
  features_json jsonb,
  active boolean NOT NULL DEFAULT true,
  is_default boolean NOT NULL DEFAULT false,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE tenant_subscription (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  plan_code varchar(128) NOT NULL,
  status varchar(50) NOT NULL,
  started_at timestamptz,
  ends_at timestamptz,
  trial_ends_at timestamptz,
  canceled_at timestamptz,
  metadata_json jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE page_model_template (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  logical_id varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  label varchar(255),
  scope varchar(32),
  slug varchar(128),
  description text,
  schema jsonb NOT NULL,
  model jsonb NOT NULL,
  schema_version integer NOT NULL DEFAULT 1,
  is_default boolean NOT NULL DEFAULT false,
  level varchar(16) NOT NULL DEFAULT 'GLOBAL',
  tenant_id uuid,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_page_model_template__level CHECK (level IN ('GLOBAL','TENANT'))
);

CREATE TABLE page_model (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  code varchar(128) NOT NULL,
  logical_id varchar(255),
  name varchar(255) NOT NULL,
  schema jsonb NOT NULL,
  scope varchar(255) NOT NULL,
  slug varchar(255),
  schema_version integer NOT NULL,
  model jsonb NOT NULL,
  status varchar(255) NOT NULL,
  published_at timestamptz,
  template_id uuid REFERENCES page_model_template(id),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_page_model__tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE audit_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  occurred_at timestamptz NOT NULL,
  actor_type varchar(32) NOT NULL,
  actor_id varchar(255),
  entity_type varchar(64) NOT NULL,
  entity_id varchar(255) NOT NULL,
  action varchar(64) NOT NULL,
  details jsonb,
  ip inet,
  user_agent text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS limit_assignment (
                                                id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id uuid NOT NULL,

    rule_key varchar(80) NOT NULL,

    scope_type varchar(32) NOT NULL,
    scope_id uuid NOT NULL,

    enabled boolean NOT NULL DEFAULT true,
    on_breach varchar(32) NOT NULL DEFAULT 'BLOCK',

    params jsonb NOT NULL DEFAULT '{}'::jsonb,

    starts_at timestamptz NULL,
    ends_at timestamptz NULL,

    version bigint NOT NULL DEFAULT 0,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz NULL,
    deleted_by uuid,

    CONSTRAINT ck_limit_assignment_scope_type
    CHECK (scope_type IN ('TENANT', 'DRAW_CHANNEL', 'OUTLET', 'AGENT')),

    CONSTRAINT ck_limit_assignment_on_breach
    CHECK (on_breach IN ('ALLOW', 'WARN', 'REQUIRE_APPROVAL', 'BLOCK')),

    CONSTRAINT ck_limit_assignment_window
    CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
    );

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
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_autonomy_policy_rule__target_type CHECK (target_type IN ('TENANT','OUTLET','USER'))
);


CREATE TABLE IF NOT EXISTS draw_exposure (
                                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id uuid NOT NULL,

    draw_id uuid NOT NULL,

    scope_type varchar(32) NOT NULL,
    scope_id uuid NOT NULL,

    bet_type varchar(32) NOT NULL,
    selection_key varchar(64) NOT NULL,

    stake_total numeric(14, 2) NOT NULL DEFAULT 0,
    sales_count bigint NOT NULL DEFAULT 0,
    potential_payout_total numeric(14, 2) NOT NULL DEFAULT 0,

    last_event_id uuid NULL,
    last_event_at timestamptz NULL,

    version bigint NOT NULL DEFAULT 0,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz NULL,
    deleted_by uuid,

    CONSTRAINT ck_draw_exposure_scope_type
    CHECK (scope_type IN ('TENANT', 'DRAW_CHANNEL', 'OUTLET', 'AGENT')),

    CONSTRAINT ck_draw_exposure_amounts_non_negative
    CHECK (
              stake_total >= 0
              AND sales_count >= 0
              AND potential_payout_total >= 0
          )
    );

CREATE TABLE ledger_entry (
                              id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              tenant_id uuid NOT NULL,

                              ref_type varchar(64) NOT NULL,
                              ref_id uuid NOT NULL,
                              operation_type varchar(64) NOT NULL,

                              amount_cents bigint NOT NULL,
                              currency varchar(3) NOT NULL,
                              direction varchar(8) NOT NULL,

                              occurred_at timestamptz NOT NULL,
                              reversal_of_entry_id uuid NULL,
                              reason varchar(255) NULL,

                              version bigint NOT NULL DEFAULT 0,
                              created_at timestamptz NOT NULL DEFAULT now(),
                              created_by uuid,
                              updated_at timestamptz NOT NULL DEFAULT now(),
                              updated_by uuid,
                              deleted_at timestamptz NULL,
                              deleted_by uuid,

                              CONSTRAINT ck_ledger_entry_amount_positive CHECK (amount_cents > 0),
                              CONSTRAINT ck_ledger_entry_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
                              CONSTRAINT uq_ledger_entry_tenant_ref_op UNIQUE (
                                                                               tenant_id,
                                                                               ref_type,
                                                                               ref_id,
                                                                               operation_type
                                  )
);


CREATE TABLE tchala_entry (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  lang varchar(8) NOT NULL,
  dream varchar(200) NOT NULL,
  dedupe_key varchar(240) NOT NULL,
  note text NOT NULL DEFAULT '',
  status varchar(32) NOT NULL,
  source varchar(32) NOT NULL,
  conflict_with_entry_id uuid,
  canonical_entry_id uuid,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_tchala_entry__lang_dedupe UNIQUE (lang, dedupe_key)
);

CREATE TABLE tchala_entry_number (
  entry_id uuid NOT NULL REFERENCES tchala_entry(id),
  number smallint NOT NULL,
  lang varchar(8) NOT NULL,
  PRIMARY KEY (entry_id, number),
  CONSTRAINT chk_tchala_entry_number__number CHECK (number >= 0 AND number <= 99)
);

CREATE TABLE notification (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  source_type varchar(96),
  source_id varchar(160),
  dedupe_key varchar(240),
  audience_type varchar(32) NOT NULL,
  audience_value varchar(160) NOT NULL,
  severity varchar(32) NOT NULL,
  kind varchar(32) NOT NULL,
  category varchar(48) NOT NULL,
  title_key varchar(255),
  message_key varchar(255),
  title_text varchar(512),
  message_text varchar(4000),
  payload jsonb,
  action_type varchar(96),
  action_url varchar(1024),
  status varchar(32) NOT NULL DEFAULT 'UNREAD',
  read_at timestamptz,
  archived_at timestamptz,
  expires_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_notification__audience_type CHECK (audience_type IN ('USER','ROLE','TENANT','OUTLET','TERMINAL','PLATFORM')),
  CONSTRAINT chk_notification__severity CHECK (severity IN ('INFO','WARNING','ERROR','CRITICAL')),
  CONSTRAINT chk_notification__kind CHECK (kind IN ('INFO','WARNING','ACTION_REQUIRED','SYSTEM_ERROR')),
  CONSTRAINT chk_notification__category CHECK (category IN ('PAGE_MODEL','TENANT_CONFIG','USER','OUTLET','TERMINAL','SESSION','SALES','DRAW','RESULT','PAYOUT','BATCH','SYSTEM','SECURITY')),
  CONSTRAINT chk_notification__status CHECK (status IN ('UNREAD','READ','ARCHIVED','EXPIRED'))
);

CREATE TABLE notification_delivery (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  notification_id uuid NOT NULL REFERENCES notification(id),
  channel varchar(32) NOT NULL,
  recipient varchar(255),
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  attempt_count integer NOT NULL DEFAULT 0,
  next_attempt_at timestamptz,
  last_attempt_at timestamptz,
  provider varchar(96),
  provider_message_id varchar(160),
  error_code varchar(96),
  error_message varchar(1000),
  payload jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_notification_delivery__channel CHECK (channel IN ('WEB','SMS','WHATSAPP','EMAIL','PUSH')),
  CONSTRAINT chk_notification_delivery__status CHECK (status IN ('PENDING','SENT','DELIVERED','FAILED','SKIPPED','CANCELLED'))
);

CREATE TABLE notification_preference (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  scope_type varchar(32) NOT NULL,
  scope_value varchar(160) NOT NULL,
  category varchar(48) NOT NULL,
  kind varchar(32) NOT NULL,
  channel varchar(32) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_notification_preference__scope_type CHECK (scope_type IN ('TENANT','ROLE','USER')),
  CONSTRAINT chk_notification_preference__category CHECK (category IN ('PAGE_MODEL','TENANT_CONFIG','USER','OUTLET','TERMINAL','SESSION','SALES','DRAW','RESULT','PAYOUT','BATCH','SYSTEM','SECURITY')),
  CONSTRAINT chk_notification_preference__kind CHECK (kind IN ('INFO','WARNING','ACTION_REQUIRED','SYSTEM_ERROR')),
  CONSTRAINT chk_notification_preference__channel CHECK (channel IN ('WEB','SMS','WHATSAPP','EMAIL','PUSH')),
  CONSTRAINT uq_notification_preference__scope UNIQUE (tenant_id, scope_type, scope_value, category, kind, channel)
);

CREATE TABLE notification_template (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  template_key varchar(120) NOT NULL,
  locale varchar(20) NOT NULL,
  title_template text NOT NULL,
  body_template text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_notification_template__scope UNIQUE (tenant_id, template_key, locale)
);

CREATE TABLE outbound_message (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  source_event_id uuid,
  channel varchar(32) NOT NULL,
  recipient_type varchar(32) NOT NULL,
  recipient_value varchar(255) NOT NULL,
  template_key varchar(120) NOT NULL,
  locale varchar(20),
  subject varchar(255),
  body text NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  priority varchar(32) NOT NULL DEFAULT 'NORMAL',
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  correlation_key varchar(180),
  next_attempt_at timestamptz,
  sent_at timestamptz,
  failed_at timestamptz,
  failure_reason text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_outbound_message__channel CHECK (channel IN ('SLACK','SLACK_INTERNAL','SLACK_TENANT_WEBHOOK','EMAIL','SMS','WHATSAPP','PUSH')),
  CONSTRAINT chk_outbound_message__priority CHECK (priority IN ('LOW','NORMAL','HIGH','CRITICAL')),
  CONSTRAINT chk_outbound_message__status CHECK (status IN ('PENDING','DISPATCHING','SENT','FAILED','SKIPPED','CANCELLED'))
);

CREATE TABLE message_delivery_attempt (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  message_id uuid NOT NULL REFERENCES outbound_message(id),
  attempted_at timestamptz NOT NULL,
  status varchar(32) NOT NULL,
  provider varchar(80) NOT NULL,
  provider_message_id varchar(255),
  error_code varchar(120),
  error_message text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_message_delivery_attempt__status CHECK (status IN ('PENDING','DISPATCHING','SENT','FAILED','SKIPPED','CANCELLED'))
);

CREATE TABLE message_template (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  template_key varchar(120) NOT NULL,
  channel varchar(32) NOT NULL,
  locale varchar(20) NOT NULL,
  subject_template text,
  body_template text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_message_template__channel CHECK (channel IN ('SLACK','SLACK_INTERNAL','SLACK_TENANT_WEBHOOK','EMAIL','SMS','WHATSAPP','PUSH')),
  CONSTRAINT uq_message_template__scope UNIQUE (tenant_id, template_key, channel, locale)
);

CREATE TABLE tenant_communication_settings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  email_enabled boolean NOT NULL DEFAULT true,
  sms_enabled boolean NOT NULL DEFAULT false,
  tenant_slack_enabled boolean NOT NULL DEFAULT false,
  tenant_slack_webhook_secret_ref varchar(255),
  critical_alert_email varchar(255),
  ops_alert_email varchar(255),
  default_locale varchar(20) NOT NULL DEFAULT 'fr',
  quiet_hours_start time,
  quiet_hours_end time,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_tenant_communication_settings__tenant UNIQUE (tenant_id)
);

-- =========================================================
-- SALES TICKET (replaces legacy ticket/ticket_line)
-- =========================================================

CREATE TABLE sales_ticket (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  outlet_id uuid NOT NULL,
  terminal_id uuid NOT NULL,
  seller_user_id uuid NOT NULL,
  sales_session_id uuid NOT NULL,
  draw_id uuid NOT NULL,
  draw_channel_id uuid NOT NULL,
  payout_id uuid,
  ticket_code varchar(64) NOT NULL,
  public_code varchar(32) NOT NULL,
  verification_code varchar(32) NOT NULL,
  currency varchar(3) NOT NULL,
  stake_amount numeric(19,4) NOT NULL,
  total_amount numeric(19,4) NOT NULL,
  potential_payout_amount numeric(19,4) NOT NULL DEFAULT 0,
  winning_amount numeric(19,4) NOT NULL DEFAULT 0,
  sale_status varchar(32) NOT NULL,
  sold_at timestamptz NOT NULL,
  placed_at timestamptz NOT NULL,
  approval_request_id uuid,
  approval_requested_by uuid,
  approval_requested_at timestamptz,
  approved_at timestamptz,
  approved_by uuid,
  rejected_at timestamptz,
  rejected_by uuid,
  rejection_reason varchar(500),
  cancelled_at timestamptz,
  cancelled_by uuid,
  cancellation_reason varchar(500),
  voided_at timestamptz,
  voided_by uuid,
  void_reason varchar(500),
  result_status varchar(32) NOT NULL,
  resulted_at timestamptz,
  resulted_by uuid,
  result_override_reason varchar(500),
  settlement_status varchar(32) NOT NULL,
  settled_at timestamptz,
  settled_by uuid,
  paid_at timestamptz,
  paid_by uuid,
  sale_channel varchar(32) NOT NULL,
  offline_submission_id uuid,
  offline_batch_id uuid,
  offline_code_batch_id uuid,
  offline_code varchar(64),
  offline_client_sale_id varchar(64),
  offline_local_sequence bigint,
  offline_sold_at_device timestamptz,
  offline_sync_status varchar(48),
  seller_id uuid NULL,
  seller_assignment_id uuid NULL,
  print_status varchar(16) NOT NULL,
  print_count integer NOT NULL DEFAULT 0,
  first_printed_at timestamptz,
  last_printed_at timestamptz,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_sales_ticket__tenant_code UNIQUE (tenant_id, ticket_code),
  CONSTRAINT uk_sales_ticket__public_code UNIQUE (tenant_id, public_code),
  CONSTRAINT uk_sales_ticket__verification_code UNIQUE (tenant_id, verification_code),
  CONSTRAINT chk_sales_ticket__offline_ref CHECK (
    (sale_channel = 'POS_OFFLINE_SYNCED'
      AND offline_submission_id IS NOT NULL AND offline_batch_id IS NOT NULL
      AND offline_code_batch_id IS NOT NULL AND offline_code IS NOT NULL
      AND offline_client_sale_id IS NOT NULL AND offline_local_sequence IS NOT NULL
      AND offline_sold_at_device IS NOT NULL AND offline_sync_status IS NOT NULL)
    OR
    (sale_channel <> 'POS_OFFLINE_SYNCED'
      AND offline_submission_id IS NULL AND offline_batch_id IS NULL
      AND offline_code_batch_id IS NULL AND offline_code IS NULL
      AND offline_client_sale_id IS NULL AND offline_local_sequence IS NULL
      AND offline_sold_at_device IS NULL AND offline_sync_status IS NULL)
  )
);

CREATE TABLE sales_ticket_line (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  ticket_id uuid NOT NULL REFERENCES sales_ticket(id),
  draw_id uuid NOT NULL,
  line_number integer NOT NULL,
  game_code varchar(64) NOT NULL,
  bet_type varchar(64) NOT NULL,
  bet_option smallint,
  selection_key varchar(128) NOT NULL,
  display_selection varchar(256) NOT NULL,
  stake_amount numeric(19,4) NOT NULL,
  payout_base_amount numeric(19,4) NOT NULL,
  odds_snapshot numeric(19,6),
  potential_payout_amount numeric(19,4) NOT NULL DEFAULT 0,
  origin varchar(16) NOT NULL DEFAULT 'CUSTOMER',
  pricing_source varchar(16) NOT NULL DEFAULT 'STANDARD',
  selection_source varchar(32) NOT NULL DEFAULT 'CUSTOMER_SELECTED',
  promotion_decision_id uuid,
  promotion_label varchar(128),
  promotion_effect_type varchar(32),
  result_status varchar(16) NOT NULL,
  payout_amount numeric(19,4) NOT NULL DEFAULT 0,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_ticket_line__number UNIQUE (tenant_id, ticket_id, line_number),
  CONSTRAINT chk_ticket_line__amounts CHECK (
    stake_amount >= 0 AND potential_payout_amount >= 0 AND payout_amount >= 0
  )
);

CREATE TABLE sales_ticket_charge (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  sales_ticket_id uuid NOT NULL REFERENCES sales_ticket(id) ON DELETE CASCADE,
  charge_type varchar(48) NOT NULL,
  paid_by varchar(16) NOT NULL,
  amount numeric(19,4) NOT NULL,
  currency varchar(3) NOT NULL,
  waived_by_rule_id uuid,
  waived_by_decision_id uuid,
  waived_effect_type varchar(64),
  waived_label varchar(256),
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uk_sales_ticket_charge__type UNIQUE (sales_ticket_id, charge_type, paid_by),
  CONSTRAINT chk_sales_ticket_charge__type CHECK (charge_type IN ('SMS_DELIVERY','WHATSAPP_DELIVERY','EMAIL_DELIVERY')),
  CONSTRAINT chk_sales_ticket_charge__paid_by CHECK (paid_by IN ('BUYER','SELLER','TENANT')),
  CONSTRAINT chk_sales_ticket_charge__amount CHECK (amount >= 0),
  CONSTRAINT chk_sales_ticket_charge__currency CHECK (currency ~ '^[A-Z]{3}$')
);

-- =========================================================
-- OFFLINE SYNC
-- =========================================================

CREATE TABLE offline_grant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  terminal_id uuid NOT NULL,
  outlet_id uuid NOT NULL,
  seller_user_id uuid NOT NULL,
  sales_session_id uuid NOT NULL,
  device_id uuid NOT NULL,
  device_public_key text NOT NULL,
  key_id varchar(64) NOT NULL,
  code_batch_id uuid,
  status varchar(32) NOT NULL,
  valid_from timestamptz NOT NULL,
  valid_until timestamptz NOT NULL,
  sync_accepted_until timestamptz NOT NULL,
  max_ticket_count integer,
  max_total_amount numeric(18,2),
  currency varchar(3) NOT NULL,
  consumed_ticket_count integer NOT NULL DEFAULT 0,
  consumed_total_amount numeric(18,2) NOT NULL DEFAULT 0,
  token_hash varchar(255) NOT NULL,
  issued_at timestamptz NOT NULL,
  revoked_at timestamptz,
  revoked_reason varchar(255),
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT ck_offline_grant_windows CHECK (valid_from < valid_until AND valid_until <= sync_accepted_until)
);

CREATE TABLE offline_sync_batch (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  terminal_id uuid NOT NULL,
  outlet_id uuid NOT NULL,
  seller_user_id uuid NOT NULL,
  sales_session_id uuid NOT NULL,
  device_id uuid NOT NULL,
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  code_batch_id uuid,
  client_batch_id varchar(255) NOT NULL,
  received_at timestamptz NOT NULL,
  processed_at timestamptz,
  status varchar(32) NOT NULL,
  submission_count integer NOT NULL DEFAULT 0,
  technical_reject_count integer NOT NULL DEFAULT 0,
  sales_accept_count integer NOT NULL DEFAULT 0,
  sales_reject_count integer NOT NULL DEFAULT 0,
  review_count integer NOT NULL DEFAULT 0,
  raw_manifest jsonb,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE offline_code_batch (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  terminal_id uuid NOT NULL,
  outlet_id uuid,
  seller_user_id uuid,
  allocated_count integer NOT NULL,
  consumed_count integer NOT NULL DEFAULT 0,
  status varchar(32) NOT NULL,
  issued_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE offline_submission (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  sync_batch_id uuid REFERENCES offline_sync_batch(id),
  grant_id uuid NOT NULL REFERENCES offline_grant(id),
  code_batch_id uuid REFERENCES offline_code_batch(id),
  offline_code varchar(255),
  client_submission_id varchar(255) NOT NULL,
  device_id uuid NOT NULL,
  seller_user_id uuid NOT NULL,
  terminal_id uuid NOT NULL,
  outlet_id uuid NOT NULL,
  sales_session_id uuid NOT NULL,
  client_sold_at timestamptz NOT NULL,
  received_at timestamptz NOT NULL,
  processed_at timestamptz,
  status varchar(32) NOT NULL,
  rejection_code varchar(64),
  rejection_reason varchar(500),
  draw_id uuid NOT NULL REFERENCES draw(id),
  total_stake_amount numeric(18,2) NOT NULL,
  currency varchar(3) NOT NULL,
  line_count integer NOT NULL,
  payload_hash varchar(255) NOT NULL,
  signature varchar(255),
  promotion_attempt_id uuid,
  promotion_requested_at timestamptz,
  last_promotion_event_id uuid,
  created_ticket_id uuid REFERENCES sales_ticket(id),
  raw_payload jsonb NOT NULL,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_offline_submission__grant_client UNIQUE (tenant_id, grant_id, client_submission_id)
);

CREATE TABLE offline_submission_line (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  submission_id uuid NOT NULL REFERENCES offline_submission(id),
  line_no integer NOT NULL,
  game_code varchar(64) NOT NULL,
  bet_type varchar(64) NOT NULL,
  bet_option varchar(64) NOT NULL,
  selection_key varchar(255) NOT NULL,
  stake_amount numeric(18,2) NOT NULL,
  potential_payout numeric(18,2),
  status varchar(32) NOT NULL,
  rejection_code varchar(64),
  rejection_reason varchar(500),
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE offline_code (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  code_batch_id uuid NOT NULL REFERENCES offline_code_batch(id),
  grant_id uuid REFERENCES offline_grant(id),
  offline_submission_id uuid REFERENCES offline_submission(id),
  ticket_id uuid REFERENCES sales_ticket(id),
  code varchar(64) NOT NULL,
  status varchar(32) NOT NULL,
  reserved_at timestamptz,
  consumed_at timestamptz,
  expires_at timestamptz,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_offline_code__tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE offline_submission_ticket_link (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  submission_id uuid NOT NULL REFERENCES offline_submission(id),
  ticket_id uuid REFERENCES sales_ticket(id),
  link_type varchar(32) NOT NULL,
  linked_at timestamptz NOT NULL,
  details_json jsonb,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE offline_submission_decision (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  submission_id uuid NOT NULL REFERENCES offline_submission(id),
  decision_type varchar(32) NOT NULL,
  decided_by uuid NOT NULL,
  decided_at timestamptz NOT NULL,
  reason text NOT NULL,
  dry_run boolean NOT NULL DEFAULT false,
  report_json jsonb,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0
);

-- Per-tenant override of the offline limit policy. When absent, the global defaults from
-- tch.limitpolicy.offline.* properties apply. Exactly one row per tenant.
CREATE TABLE tenant_offline_policy (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  offline_enabled boolean NOT NULL,
  batch_size integer NOT NULL,
  validity_duration_iso varchar(64) NOT NULL,
  sync_accepted_extension_iso varchar(64) NOT NULL,
  max_ticket_count integer NOT NULL,
  max_total_amount numeric(18,2) NOT NULL,
  currency varchar(3) NOT NULL,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_tenant_offline_policy__tenant UNIQUE (tenant_id)
);

-- Outbox for offlinesync domain events (TechValidated, AdminApproved). Events are written
-- in the same tx as the business write; a drainer scheduler picks them up after commit and
-- publishes them via DomainEventPublisher. Guarantees at-least-once delivery across pod
-- restarts and decouples publication latency from the request thread.
CREATE TABLE offline_event_outbox (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  event_id uuid NOT NULL,
  event_class varchar(255) NOT NULL,
  payload_json jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  published_at timestamptz,
  attempts integer NOT NULL DEFAULT 0,
  last_error text,
  next_attempt_at timestamptz,
  CONSTRAINT uq_offline_event_outbox__event_id UNIQUE (tenant_id, event_id)
);

-- =========================================================
-- PROMOTION DOMAIN
-- =========================================================
-- BaseTenantEntity columns are physically present in every tenant table:
-- id, tenant_id, created_at, created_by, updated_at, updated_by, deleted_at, deleted_by, version.
-- Repositories must not filter tenant_id or deleted_at manually; RLS handles tenant isolation.

CREATE TABLE promotion_campaign (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  code varchar(96) NOT NULL,
  name varchar(160) NOT NULL,
  status varchar(32) NOT NULL,
  priority integer NOT NULL DEFAULT 100,
  starts_at timestamptz NULL,
  ends_at timestamptz NULL,
  config_version varchar(48) NOT NULL DEFAULT 'v1',
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_campaign_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE promotion_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  campaign_id uuid NOT NULL REFERENCES promotion_campaign(id),
  rule_key varchar(96) NOT NULL,
  priority integer NOT NULL DEFAULT 100,
  min_paid_total numeric(19,4) NULL,
  before_local_time time NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_rule_tenant_campaign_key UNIQUE (tenant_id, campaign_id, rule_key)
);

CREATE TABLE promotion_rule_effect (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  rule_id uuid NOT NULL REFERENCES promotion_rule(id),
  effect_type varchar(32) NOT NULL,
  game_code varchar(64) NULL,
  payout_base_amount numeric(19,4) NULL,
  quantity integer NULL,
  odds_override numeric(19,6) NULL,
  charge_type varchar(64) NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_promotion_rule_effect_type CHECK (effect_type IN ('FREE_GAME_LINE','BOOST_ODDS','WAIVE_CHARGE')),
  CONSTRAINT chk_promotion_rule_effect_quantity CHECK (quantity IS NULL OR quantity > 0),
  CONSTRAINT chk_promotion_rule_effect_amount CHECK (payout_base_amount IS NULL OR payout_base_amount > 0),
  CONSTRAINT chk_promotion_rule_effect_odds CHECK (odds_override IS NULL OR odds_override > 0)
);

CREATE TABLE promotion_rule_eligibility_line (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  rule_id uuid NOT NULL REFERENCES promotion_rule(id),
  game_code varchar(64) NOT NULL,
  min_count integer NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_promotion_rule_eligibility_line_min_count CHECK (min_count > 0)
);

CREATE TABLE promotion_decision (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  decision_status varchar(32) NOT NULL,
  evaluation_phase varchar(48) NOT NULL,
  evaluated_at timestamptz NOT NULL,
  context_hash varchar(128) NOT NULL,
  engine_version varchar(48) NOT NULL,
  decision_json jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_decision_tenant_hash_phase UNIQUE (tenant_id, context_hash, evaluation_phase)
);

CREATE TABLE applied_promotion_snapshot (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  ticket_id uuid NOT NULL,
  promotion_decision_id uuid NOT NULL,
  decision_status varchar(32) NOT NULL,
  applied_at timestamptz NOT NULL,
  snapshot_json jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_applied_promotion_tenant_ticket_decision UNIQUE (tenant_id, ticket_id, promotion_decision_id)
);

-- FK from sales_ticket_charge.waived_by_rule_id to promotion_rule
-- (column already exists in sales_ticket_charge; both tables are defined in this migration)
ALTER TABLE sales_ticket_charge
  ADD CONSTRAINT fk_sales_ticket_charge__waived_by_rule
    FOREIGN KEY (waived_by_rule_id) REFERENCES promotion_rule(id);

ALTER TABLE sales_ticket_charge
  ADD CONSTRAINT fk_sales_ticket_charge__waived_by_decision
    FOREIGN KEY (waived_by_decision_id) REFERENCES promotion_decision(id);

-- =========================================================
-- SELLER DOMAIN
-- =========================================================

CREATE TABLE seller (
  id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    uuid         NOT NULL REFERENCES tenant(id),
  user_id      uuid         NULL,
  code         varchar(80)  NOT NULL,
  display_name varchar(180) NOT NULL,
  status       varchar(24)  NOT NULL,
  created_at   timestamptz  NOT NULL DEFAULT now(),
  created_by   uuid         NULL,
  updated_at   timestamptz  NOT NULL DEFAULT now(),
  updated_by   uuid         NULL,
  deleted_at   timestamptz  NULL,
  deleted_by   uuid         NULL,
  version      bigint       NOT NULL DEFAULT 0,
  CONSTRAINT uq_seller_tenant_code UNIQUE (tenant_id, code),
  CONSTRAINT ck_seller_status CHECK (status IN ('ACTIVE','SUSPENDED','INACTIVE'))
);

CREATE TABLE seller_outlet_assignment (
  id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   uuid        NOT NULL REFERENCES tenant(id),
  seller_id   uuid        NOT NULL REFERENCES seller(id),
  outlet_id   uuid        NOT NULL REFERENCES outlet(id),
  starts_at   timestamptz NOT NULL,
  ends_at     timestamptz NULL,
  status      varchar(24) NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  created_by  uuid        NULL,
  updated_at  timestamptz NOT NULL DEFAULT now(),
  updated_by  uuid        NULL,
  deleted_at  timestamptz NULL,
  deleted_by  uuid        NULL,
  version     bigint      NOT NULL DEFAULT 0,
  CONSTRAINT ck_seller_assignment_status CHECK (status IN ('ACTIVE','ENDED','SUSPENDED'))
);

CREATE TABLE seller_commission_policy (
  id              uuid          PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       uuid          NOT NULL REFERENCES tenant(id),
  seller_id       uuid          NOT NULL REFERENCES seller(id),
  commission_type varchar(40)   NOT NULL,
  commission_base varchar(40)   NOT NULL,
  rate_percent    numeric(8,4)  NULL,
  fixed_amount    numeric(18,4) NULL,
  currency        varchar(8)    NOT NULL,
  starts_at       timestamptz   NOT NULL,
  ends_at         timestamptz   NULL,
  status          varchar(24)   NOT NULL,
  created_at      timestamptz   NOT NULL DEFAULT now(),
  created_by      uuid          NULL,
  updated_at      timestamptz   NOT NULL DEFAULT now(),
  updated_by      uuid          NULL,
  deleted_at      timestamptz   NULL,
  deleted_by      uuid          NULL,
  version         bigint        NOT NULL DEFAULT 0,
  CONSTRAINT ck_seller_commission_type CHECK (commission_type IN ('NONE','PERCENT','FIXED_PER_TICKET','FIXED_PLUS_PERCENT')),
  CONSTRAINT ck_seller_commission_base CHECK (commission_base IN ('GROSS_SALES','NET_SALES','PROFIT','TICKET_COUNT')),
  CONSTRAINT ck_seller_commission_status CHECK (status IN ('ACTIVE','ENDED','SUSPENDED'))
);

-- FK snapshots on sales_ticket back to seller tables (deferred; seller is defined after sales_ticket above)
ALTER TABLE sales_ticket
  ADD CONSTRAINT fk_sales_ticket__seller            FOREIGN KEY (seller_id)            REFERENCES seller(id),
  ADD CONSTRAINT fk_sales_ticket__seller_assignment FOREIGN KEY (seller_assignment_id) REFERENCES seller_outlet_assignment(id);

-- ─────────────────────────────────────────────────────────────────────────────
-- business_day_override
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE business_day_override (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid        NOT NULL REFERENCES tenant(id),
    outlet_id     uuid        NULL     REFERENCES outlet(id),   -- NULL = tenant-level
    business_date date        NOT NULL,
    open          boolean     NOT NULL,
    reason_code   varchar(96) NULL,
    label         varchar(255) NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    created_by    uuid        NULL,
    updated_at    timestamptz NOT NULL DEFAULT now(),
    updated_by    uuid        NULL,
    deleted_at    timestamptz NULL,
    deleted_by    uuid        NULL,
    version       bigint      NOT NULL DEFAULT 0
);

COMMENT ON TABLE business_day_override IS
    'Tenant or outlet-level business day open/close overrides (holidays, exceptional closures/openings).
     outlet_id IS NULL = tenant-level rule. outlet_id IS NOT NULL = outlet-level override (wins over tenant).';
