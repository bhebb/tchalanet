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
  active_theme_id uuid,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
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
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE draw_channel (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
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
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_draw_result__status CHECK (status IN ('PROVISIONAL','FINAL','ERROR'))
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
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_draw__tenant_channel_date UNIQUE (tenant_id, draw_channel_id, draw_date),
  CONSTRAINT chk_draw__status CHECK (status IN ('SCHEDULED','OPEN','CLOSED','RESULTED','SETTLED','CANCELED','ARCHIVED'))
);

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
  address_id uuid REFERENCES address(id),
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_outlet__tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE terminal (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  state varchar(255) NOT NULL,
  last_seen timestamptz,
  label varchar(128),
  inventory_tag varchar(64),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
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
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE sales_session (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  outlet_id uuid NOT NULL REFERENCES outlet(id),
  terminal_id uuid NOT NULL REFERENCES terminal(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  status varchar(16) NOT NULL,
  opened_at timestamptz NOT NULL DEFAULT now(),
  closed_at timestamptz,
  opening_float numeric(14,2),
  closing_amount numeric(14,2),
  meta jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_sales_session__status CHECK (status IN ('OPEN','CLOSED','SETTLED'))
);

CREATE TABLE sales_session_totals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  session_id uuid NOT NULL REFERENCES sales_session(id),
  total_tickets bigint NOT NULL DEFAULT 0,
  total_stake numeric(14,2) NOT NULL DEFAULT 0,
  total_payout numeric(14,2) NOT NULL DEFAULT 0,
  gross_margin numeric(14,2) NOT NULL DEFAULT 0,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_sales_session_totals__session UNIQUE (session_id)
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
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_pricing_odds__tenant_game_bet UNIQUE (tenant_id, game_code, bet_type, bet_option)
);

CREATE TABLE ticket (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  terminal_id uuid NOT NULL REFERENCES terminal(id),
  draw_id uuid NOT NULL REFERENCES draw(id),
  session_id uuid REFERENCES sales_session(id),
  ticket_code text NOT NULL,
  public_code varchar(32),
  sale_status varchar(24) NOT NULL,
  result_status varchar(24) NOT NULL,
  settlement_status varchar(24) NOT NULL,
  currency varchar(8) NOT NULL,
  total_amount numeric(14,2) NOT NULL,
  winning_amount numeric(14,2),
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
  ticket_id uuid NOT NULL REFERENCES ticket(id),
  game_code varchar(32) NOT NULL,
  selection varchar(32) NOT NULL,
  stake numeric(12,2) NOT NULL,
  odds_snapshot numeric(12,4) NOT NULL,
  potential_payout numeric(14,2) NOT NULL,
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
  outlet_id uuid REFERENCES outlet(id),
  session_id uuid REFERENCES sales_session(id),
  terminal_id uuid REFERENCES terminal(id),
  paid_by_user_id uuid REFERENCES app_user(id),
  selling_outlet_id uuid REFERENCES outlet(id),
  selling_session_id uuid REFERENCES sales_session(id),
  amount_cents bigint NOT NULL,
  currency varchar(3) NOT NULL,
  status varchar(255) NOT NULL,
  approved_at timestamptz,
  paid_at timestamptz,
  rejected_at timestamptz,
  rejected_reason text,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
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
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE page_model_template (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,
  logical_id varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  label varchar(255),
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
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_page_model__tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE audit_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  occurred_at timestamptz NOT NULL,
  actor_type varchar(32) NOT NULL,
  actor_id varchar(255) NOT NULL,
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
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE autonomy_policy_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  target_type varchar(255) NOT NULL,
  target_id uuid NOT NULL,
  level varchar(255) NOT NULL,
  require_approval_on_block boolean NOT NULL DEFAULT true,
  approval_role varchar(255),
  enabled boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE limit_definition (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  rule_key varchar(255) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  on_breach varchar(255) NOT NULL DEFAULT 'BLOCK',
  params jsonb NOT NULL,
  applies_to jsonb NOT NULL,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE limit_assignment (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  limit_definition_id uuid NOT NULL REFERENCES limit_definition(id),
  target_type varchar(16) NOT NULL,
  target_id uuid,
  enabled boolean NOT NULL DEFAULT false,
  starts_at timestamptz,
  ends_at timestamptz,
  params_override jsonb,
  applies_to_override jsonb,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
);

CREATE TABLE draw_exposure (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  draw_id uuid NOT NULL REFERENCES draw(id),
  scope_type varchar(16) NOT NULL,
  scope_id uuid NOT NULL,
  bet_type varchar(32) NOT NULL,
  selection_key varchar(64) NOT NULL,
  stake_total numeric(14,2) NOT NULL DEFAULT 0,
  sales_count bigint NOT NULL DEFAULT 0,
  potential_payout_total numeric(14,2) NOT NULL DEFAULT 0,
  last_event_id uuid,
  last_event_at timestamptz,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_draw_exposure__lookup UNIQUE (tenant_id, draw_id, scope_type, scope_id, bet_type, selection_key),
  CONSTRAINT chk_draw_exposure__scope_type CHECK (scope_type IN ('TENANT','OUTLET','TERMINAL','USER'))
);

CREATE TABLE ledger_entry (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  ref_type varchar(64) NOT NULL,
  ref_id uuid NOT NULL,
  amount numeric(18,2) NOT NULL,
  direction varchar(8) NOT NULL,
  occurred_at timestamptz NOT NULL,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_ledger_entry__direction CHECK (direction IN ('DEBIT','CREDIT'))
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
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_notification_preference__scope_type CHECK (scope_type IN ('TENANT','ROLE','USER')),
  CONSTRAINT chk_notification_preference__category CHECK (category IN ('PAGE_MODEL','TENANT_CONFIG','USER','OUTLET','TERMINAL','SESSION','SALES','DRAW','RESULT','PAYOUT','BATCH','SYSTEM','SECURITY')),
  CONSTRAINT chk_notification_preference__kind CHECK (kind IN ('INFO','WARNING','ACTION_REQUIRED','SYSTEM_ERROR')),
  CONSTRAINT chk_notification_preference__channel CHECK (channel IN ('WEB','SMS','WHATSAPP','EMAIL','PUSH')),
  CONSTRAINT uq_notification_preference__scope UNIQUE (tenant_id, scope_type, scope_value, category, kind, channel)
);
