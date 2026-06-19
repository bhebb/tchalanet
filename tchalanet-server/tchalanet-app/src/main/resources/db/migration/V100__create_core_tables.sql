-- Baseline: core business tables only
CREATE TABLE tenant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(64) NOT NULL UNIQUE,
  name varchar(255) NOT NULL,
  type varchar(32) NOT NULL DEFAULT 'PERSONAL',
  timezone varchar(64) NOT NULL DEFAULT 'UTC',
  currency varchar(3) NOT NULL DEFAULT 'USD',
  default_language varchar(8) NOT NULL DEFAULT 'fr',
  default_locale varchar(16) NOT NULL DEFAULT 'fr-HT',
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

CREATE TABLE app_user_external_identity (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  app_user_id uuid NOT NULL REFERENCES app_user(id),
  provider varchar(32) NOT NULL,
  issuer varchar(512) NOT NULL,
  external_subject varchar(255) NOT NULL,
  email_snapshot citext,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_app_user_external_identity__provider
    CHECK (provider IN ('KEYCLOAK','FIREBASE','LOCAL_JWT','LOCAL_PERF')),
  CONSTRAINT uq_app_user_external_identity__provider_issuer_subject
    UNIQUE (provider, issuer, external_subject)
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
  system boolean NOT NULL DEFAULT true,
  active boolean NOT NULL DEFAULT true,
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
  scope varchar(32) NOT NULL DEFAULT 'TENANT',
  system boolean NOT NULL DEFAULT true,
  custom boolean NOT NULL DEFAULT false,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_app_role__scope CHECK (scope IN ('PLATFORM','TENANT'))
);
-- Separate partial indexes for system roles (tenant_id IS NULL) and tenant roles (tenant_id IS NOT NULL)
-- because PostgreSQL ON CONFLICT target doesn't work with nullable columns in UNIQUE constraints.
CREATE UNIQUE INDEX uq_app_role__system_code ON app_role (code) WHERE tenant_id IS NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_app_role__tenant_code ON app_role (tenant_id, code) WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE role_permission (
  role_id uuid NOT NULL REFERENCES app_role(id),
  permission_code varchar(128) NOT NULL REFERENCES permission(code),
  PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE tenant_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  status varchar(32),
  is_owner boolean DEFAULT false,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_tenant_user__tenant_user UNIQUE (tenant_id, user_id)
);

-- Role assignments are stored separately from membership so a user can hold
-- multiple roles without duplicating the membership row.
CREATE TABLE tenant_user_role (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  role_id uuid NOT NULL REFERENCES app_role(id),
  assigned_at timestamptz NOT NULL DEFAULT now(),
  assigned_by uuid NULL,
  deleted_at timestamptz NULL
);
CREATE UNIQUE INDEX uq_tenant_user_role__active
  ON tenant_user_role (tenant_id, user_id, role_id)
  WHERE deleted_at IS NULL;

-- Per-user GRANT / DENY overrides on top of the role-permission defaults.
-- DENY wins over both role grants and explicit GRANTs.
CREATE TABLE user_permission_override (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  permission_code varchar(128) NOT NULL REFERENCES permission(code),
  effect varchar(16) NOT NULL,
  reason text NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  deleted_at timestamptz NULL,
  CONSTRAINT chk_user_permission_override__effect CHECK (effect IN ('GRANT','DENY'))
);
CREATE UNIQUE INDEX uq_user_permission_override__active
  ON user_permission_override (tenant_id, user_id, permission_code)
  WHERE deleted_at IS NULL;

CREATE TABLE app_setting (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  namespace varchar(255) NOT NULL,
  setting_key varchar(255) NOT NULL,
  setting_value text NOT NULL,
  value_type varchar(50) NOT NULL,
  level varchar(50) NOT NULL,
  exposure varchar(50) NOT NULL DEFAULT 'INTERNAL',
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_app_setting__level CHECK (level IN ('GLOBAL','TENANT')),
  CONSTRAINT chk_app_setting__value_type CHECK (value_type IN ('STRING','INT','LONG','DECIMAL','BOOLEAN','JSON')),
  CONSTRAINT chk_app_setting__exposure CHECK (exposure IN ('INTERNAL','PUBLIC_RUNTIME','TENANT_RUNTIME','ADMIN_RUNTIME'))
);

CREATE TABLE i18n_override (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  level varchar(32) NOT NULL DEFAULT 'TENANT',
  tenant_id uuid,
  surface varchar(50) NOT NULL DEFAULT 'INTERNAL',
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
  CONSTRAINT chk_i18n_override__level CHECK (level IN ('GLOBAL','TENANT')),
  CONSTRAINT chk_i18n_override__surface CHECK (surface IN (
    'PUBLIC_HOME','PUBLIC_RESULTS','PUBLIC_TICKET_CHECK','COMMON_PUBLIC_ERROR',
    'AUTH','CASHIER','TENANT_ADMIN','PLATFORM_ADMIN','COMMON_PRIVATE_ERROR','INTERNAL'
  ))
);

CREATE TABLE theme_preset (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  vendor varchar(128),
  config text NOT NULL,
  label_key varchar(255),
  description text,
  sort_order integer NOT NULL DEFAULT 0,
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
  default_mode varchar(16) NOT NULL DEFAULT 'SYSTEM',
  active boolean NOT NULL DEFAULT true,
  is_default boolean NOT NULL DEFAULT false,
  token_overrides jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 1
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
  game_code varchar(32) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  visible_in_pos boolean NOT NULL DEFAULT true,
  display_name varchar(128),
  display_order integer NOT NULL DEFAULT 0,
  min_stake numeric(12,2),
  max_stake numeric(12,2),
  availability_enabled boolean NOT NULL DEFAULT false,
  availability_days varchar(64),
  start_local_time time,
  end_local_time time,
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
  result_date date NOT NULL,
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
  cancel_reason_code varchar(96),
  cancel_reason_label varchar(255),
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
    CHECK (scope_type IN ('TENANT', 'DRAW_CHANNEL', 'AGENT')),

    CONSTRAINT ck_limit_assignment_on_breach
    CHECK (on_breach IN ('ALLOW', 'WARN', 'REQUIRE_APPROVAL', 'BLOCK')),

    CONSTRAINT ck_limit_assignment_window
    CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
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
    CHECK (scope_type IN ('TENANT', 'DRAW_CHANNEL', 'AGENT')),

    CONSTRAINT ck_draw_exposure_amounts_non_negative
    CHECK (
              stake_total >= 0
              AND sales_count >= 0
              AND potential_payout_total >= 0
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
  CONSTRAINT chk_notification__audience_type CHECK (audience_type IN ('USER','ROLE','TENANT','TERMINAL','PLATFORM')),
  CONSTRAINT chk_notification__severity CHECK (severity IN ('INFO','WARNING','ERROR','CRITICAL')),
  CONSTRAINT chk_notification__kind CHECK (kind IN ('INFO','WARNING','ACTION_REQUIRED','SYSTEM_ERROR')),
  CONSTRAINT chk_notification__category CHECK (category IN ('PAGE_MODEL','TENANT_CONFIG','USER','TERMINAL','SESSION','SALES','DRAW','RESULT','PAYOUT','BATCH','SYSTEM','SECURITY')),
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
  CONSTRAINT chk_notification_preference__category CHECK (category IN ('PAGE_MODEL','TENANT_CONFIG','USER','TERMINAL','SESSION','SALES','DRAW','RESULT','PAYOUT','BATCH','SYSTEM','SECURITY')),
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
-- SELLER TERMINAL (unified operational seller)
-- =========================================================

CREATE TABLE seller_terminal (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  terminal_code varchar(64) NOT NULL,
  first_name varchar(120),
  last_name varchar(120),
  display_name varchar(180) NOT NULL,
  phone_number varchar(64),
  address_id uuid REFERENCES address(id),
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  commission_rate numeric(5, 2) NOT NULL DEFAULT 15.00,
  last_seen_at timestamptz,
  activated_at timestamptz,
  blocked_at timestamptz,
  blocked_by uuid,
  blocked_reason varchar(500),
  disabled_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_seller_terminal_code UNIQUE (tenant_id, terminal_code),
  CONSTRAINT chk_seller_terminal__status CHECK (status IN ('PENDING','ACTIVE','BLOCKED','DISABLED'))
);

CREATE TABLE seller_terminal_external_identity (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  seller_terminal_id uuid NOT NULL REFERENCES seller_terminal(id),
  provider varchar(32) NOT NULL,
  issuer varchar(512) NOT NULL,
  external_subject varchar(255) NOT NULL,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_seller_terminal_external_identity__provider
    CHECK (provider IN ('FIREBASE')),
  CONSTRAINT uq_seller_terminal_ext_identity
    UNIQUE (provider, issuer, external_subject)
);

-- =========================================================
-- SALES TICKET (replaces legacy ticket/ticket_line)
-- =========================================================

CREATE TABLE sales_ticket (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  seller_terminal_id uuid,
  draw_id uuid NOT NULL,
  draw_channel_id uuid NOT NULL,
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
  print_status varchar(16) NOT NULL,
  print_count integer NOT NULL DEFAULT 0,
  first_printed_at timestamptz,
  last_printed_at timestamptz,
  seller_commission_rate_snapshot numeric(5, 2),
  seller_commission_amount_snapshot numeric(12, 2),
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
  CONSTRAINT fk_sales_ticket__seller_terminal FOREIGN KEY (seller_terminal_id) REFERENCES seller_terminal(id)
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

-- ─────────────────────────────────────────────────────────────────────────────
-- business_day_override
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE business_day_override (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     uuid        NOT NULL REFERENCES tenant(id),
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
    'Tenant-level business day open/close overrides (holidays, exceptional closures/openings).';

-- ─────────────────────────────────────────────────────────────────────────────
-- result_slot_calendar_override
-- Global (no tenant_id) — reflects provider closures per result_slot.
-- Two mutually-exclusive shapes (XOR, see CHECK):
--   * slot_local_date  : a specific dated occurrence (movable feasts, one-offs).
--   * recurring_md      : a year-less 'MM-dd' annual rule (fixed holidays).
-- Both are evaluated in the result_slot timezone.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE result_slot_calendar_override (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    result_slot_id  uuid        NOT NULL REFERENCES result_slot(id),
    slot_local_date date        NULL,           -- specific occurrence (Easter, one-offs)
    recurring_md    varchar(5)  NULL,           -- 'MM-dd' annual rule (e.g. '12-25')
    available       boolean     NOT NULL,
    reason_code     varchar(96) NOT NULL,
    reason_label    varchar(255) NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    created_by      uuid        NULL,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    updated_by      uuid        NULL,
    deleted_at      timestamptz NULL,
    deleted_by      uuid        NULL,
    version         bigint      NOT NULL DEFAULT 0,
    -- Exclusive-or: exactly one shape populated. Both-set OR neither-set is rejected.
    CONSTRAINT chk_result_slot_calendar_override__shape CHECK (
        (slot_local_date IS NOT NULL AND recurring_md IS NULL)
        OR
        (slot_local_date IS NULL
         AND recurring_md ~ '^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$')
    )
);

COMMENT ON TABLE result_slot_calendar_override IS
    'Global provider calendar overrides per result_slot. available=false marks a no-draw day.
     XOR shape: slot_local_date (specific dated occurrence) vs recurring_md (year-less MM-dd
     annual rule). Both dates are in result_slot.timezone. A specific dated row overrides a
     recurring rule for the same day. Runtime truth (SUPER_ADMIN managed); seeds are bootstrap.';
