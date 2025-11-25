-- V2__schema_core.sql
-- Schéma principal Tchalanet (multi-tenant + soft delete)

-- ===========================
-- 1. TABLE TENANT (plateforme / opérateurs)
-- ===========================

CREATE TABLE tenant (
                        id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        version         bigint NOT NULL DEFAULT 0,

                        code            varchar(64) NOT NULL UNIQUE,       -- ex: 'platform', 'demo'
                        name            varchar(255) NOT NULL,
                        timezone        varchar(64) NOT NULL,
                        currency        varchar(3)  NOT NULL DEFAULT 'USD',
                        status          varchar(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|PENDING|SUSPENDED|REJECTED

                        active_theme_id uuid, -- id d’une ligne dans theme (global ou custom)

                        created_at      timestamptz NOT NULL DEFAULT now(),
                        created_by      uuid,
                        updated_at      timestamptz NOT NULL DEFAULT now(),
                        updated_by      uuid,
                        deleted_at      timestamptz
);

CREATE TRIGGER trg_tenant_updated_at
    BEFORE UPDATE
    ON tenant
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 2. POS : OUTLET & TERMINAL
-- ===========================

CREATE TABLE outlet
(
    id         uuid PRIMARY KEY      DEFAULT gen_random_uuid(),
    version    bigint       NOT NULL DEFAULT 0,

    tenant_id  uuid         NOT NULL REFERENCES tenant (id),
    name       varchar(255) NOT NULL,
    zone       varchar(128),

    created_at timestamptz  NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE INDEX ix_outlet_tenant_name ON outlet (tenant_id, name);

CREATE TRIGGER trg_outlet_updated_at
    BEFORE UPDATE
    ON outlet
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE terminal
(
    id         uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    version    bigint      NOT NULL DEFAULT 0,

    tenant_id  uuid        NOT NULL REFERENCES tenant (id),
    outlet_id  uuid        NOT NULL REFERENCES outlet (id),
    state      varchar(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|INACTIVE|BLOCKED
    last_seen  timestamptz,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE INDEX ix_terminal_tenant_outlet ON terminal (tenant_id, outlet_id);

CREATE TRIGGER trg_terminal_updated_at
    BEFORE UPDATE
    ON terminal
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 3. CATALOGUE DE JEUX (global)
-- ===========================

CREATE TABLE game
(
    id          uuid PRIMARY KEY      DEFAULT gen_random_uuid(),
    code        varchar(32)  NOT NULL, -- BORLETTE_3, MARIAGE, LOTTO_5_40...
    name        varchar(128) NOT NULL,
    category    varchar(32)  NOT NULL, -- BORLETTE|LOTTO|MARRIAGE...
    min_digits  integer      NOT NULL,
    max_digits  integer      NOT NULL,
    combination varchar(32)  NOT NULL, -- STRAIGHT|BOX|PAIR|COMBO
    description text,
    active      boolean      NOT NULL DEFAULT true,
    sort_order  int          NOT NULL DEFAULT 0,
    version     bigint       NOT NULL DEFAULT 0,

    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid,
    deleted_at  timestamptz,

    UNIQUE (code)
);

CREATE TRIGGER trg_game_updated_at
    BEFORE UPDATE
    ON game
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 4. CONFIG PAR TENANT : TENANT_GAME
-- ===========================

CREATE TABLE tenant_game
(
    id           uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    version      bigint      NOT NULL DEFAULT 0,

    tenant_id    uuid        NOT NULL REFERENCES tenant (id),
    game_id      uuid        NOT NULL REFERENCES game (id),

    enabled      boolean     NOT NULL DEFAULT true,
    display_name varchar(128), -- nom spécifique pour ce tenant
    min_stake    numeric(12, 2),
    max_stake    numeric(12, 2),
    flags        jsonb       NOT NULL DEFAULT '{}'::jsonb,

    created_at   timestamptz NOT NULL DEFAULT now(),
    created_by   uuid,
    updated_at   timestamptz NOT NULL DEFAULT now(),
    updated_by   uuid,
    deleted_at   timestamptz,

    UNIQUE (tenant_id, game_id)
);

CREATE INDEX ix_tenant_game_tenant_enabled
    ON tenant_game (tenant_id, enabled);

CREATE TRIGGER trg_tenant_game_updated_at
    BEFORE UPDATE
    ON tenant_game
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 5. DRAW_CHANNEL : canaux de tirage
-- ===========================

CREATE TABLE draw_channel
(
    id           uuid PRIMARY KEY      DEFAULT gen_random_uuid(),
    version      bigint       NOT NULL DEFAULT 0,

    tenant_id    uuid         NOT NULL REFERENCES tenant (id),
    code         varchar(64)  NOT NULL, -- NY_MID, FL_EVE...
    name         varchar(128) NOT NULL,
    game_id      uuid         NOT NULL REFERENCES game (id),
    timezone     varchar(64)  NOT NULL,
    draw_time    time         NOT NULL, -- heure locale
    cutoff_sec   int          NOT NULL DEFAULT 120,
    days_of_week varchar(32)  NOT NULL, -- "MON-SAT", "WED-SAT-SUN"...

    active       boolean      NOT NULL DEFAULT true,
    sort_order   int          NOT NULL DEFAULT 0,

    created_at   timestamptz  NOT NULL DEFAULT now(),
    created_by   uuid,
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   uuid,
    deleted_at   timestamptz,

    UNIQUE (tenant_id, code)
);

CREATE INDEX ix_draw_channel_tenant_active
    ON draw_channel (tenant_id, active, sort_order);

CREATE TRIGGER trg_draw_channel_updated_at
    BEFORE UPDATE
    ON draw_channel
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 6. DRAW : instances de tirage
-- ===========================

CREATE TABLE draw
(
    id              uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    version         bigint      NOT NULL DEFAULT 0,

    tenant_id       uuid        NOT NULL REFERENCES tenant (id),
    draw_channel_id uuid        NOT NULL REFERENCES draw_channel (id),
    game_code       varchar(32) NOT NULL REFERENCES game (code),
    draw_source     varchar(32),              -- SYSTEM|MANUAL|IMPORT...

    scheduled_at    timestamptz NOT NULL,
    cutoff_sec      int         NOT NULL DEFAULT 120,
    status          varchar(16) NOT NULL,
    result_payload  jsonb,

    -- flags controlling system behavior vs admin overrides
    system_generated boolean NOT NULL DEFAULT true,
    locked boolean NOT NULL DEFAULT false,

    created_at      timestamptz NOT NULL DEFAULT now(),
    created_by      uuid,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    updated_by      uuid,
    deleted_at      timestamptz
);

CREATE INDEX ix_draw_tenant_scheduled
    ON draw (tenant_id, scheduled_at);

-- Indexes to speed queries that filter on admin flags
CREATE INDEX IF NOT EXISTS ix_draw_tenant_system_generated ON draw (tenant_id, system_generated);
CREATE INDEX IF NOT EXISTS ix_draw_tenant_locked ON draw (tenant_id, locked);

ALTER TABLE draw
    ADD CONSTRAINT chk_draw_status
        CHECK (status IN ('SCHEDULED', 'CLOSED', 'RESULTED', 'CANCELED'));

CREATE TRIGGER trg_draw_updated_at
    BEFORE UPDATE
    ON draw
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 7. ODDS & LIMIT_POLICY
-- ===========================

CREATE TABLE odds
(
    id                uuid PRIMARY KEY        DEFAULT gen_random_uuid(),
    version           bigint         NOT NULL DEFAULT 0,

    tenant_id         uuid           NOT NULL REFERENCES tenant (id),
    game_code         varchar(32)    NOT NULL REFERENCES game (code),
    draw_channel_code varchar(64), -- optionnel: différent par channel

    multiplier        numeric(12, 4) NOT NULL,
    valid_from        timestamptz    NOT NULL DEFAULT now(),
    valid_to          timestamptz,

    created_at        timestamptz    NOT NULL DEFAULT now(),
    created_by        uuid,
    updated_at        timestamptz    NOT NULL DEFAULT now(),
    updated_by        uuid,
    deleted_at        timestamptz
);

CREATE INDEX ix_odds_tenant_game_valid
    ON odds (tenant_id, game_code, valid_from DESC);

CREATE TRIGGER trg_odds_updated_at
    BEFORE UPDATE
    ON odds
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE limit_policy
(
    id         uuid PRIMARY KEY        DEFAULT gen_random_uuid(),
    version    bigint         NOT NULL DEFAULT 0,

    tenant_id  uuid           NOT NULL REFERENCES tenant (id),
    scope      varchar(32)    NOT NULL,                 -- NUMBER|RANGE|AGENT|TERMINAL...
    target     varchar(128)   NOT NULL,                 -- numéro/pattern/agent/terminal
    daily_cap  numeric(14, 2) NOT NULL,
    on_breach  varchar(16)    NOT NULL DEFAULT 'BLOCK', -- BLOCK|WARN|ALLOW

    created_at timestamptz    NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz    NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE INDEX ix_limit_policy_tenant_scope_target
    ON limit_policy (tenant_id, scope, target);

CREATE TRIGGER trg_limit_policy_updated_at
    BEFORE UPDATE
    ON limit_policy
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 8. POS : TICKET & TICKET_LINE
-- ===========================

CREATE TABLE ticket
(
    id           uuid PRIMARY KEY        DEFAULT gen_random_uuid(),
    version      bigint         NOT NULL DEFAULT 0,

    tenant_id    uuid           NOT NULL REFERENCES tenant (id),
    terminal_id  uuid           NOT NULL REFERENCES terminal (id),
    draw_id      uuid           REFERENCES draw (id),

    ticket_code  text           NOT NULL UNIQUE, -- KSUID/ULID lisible
    public_code  varchar(32),                   -- code public court pour vérif publique
    created_at   timestamptz    NOT NULL DEFAULT now(),
    status       varchar(16)    NOT NULL,
    total_amount numeric(14, 2) NOT NULL,

    created_by   uuid,
    updated_at   timestamptz    NOT NULL DEFAULT now(),
    updated_by   uuid,
    deleted_at   timestamptz
);

ALTER TABLE ticket
    ADD CONSTRAINT chk_ticket_status
        CHECK (status IN ('PENDING', 'WON', 'LOST', 'PAID', 'VOID'));

CREATE INDEX ix_ticket_tenant_created
    ON ticket (tenant_id, created_at);

CREATE TRIGGER trg_ticket_updated_at
    BEFORE UPDATE
    ON ticket
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE ticket_line
(
    id               uuid PRIMARY KEY        DEFAULT gen_random_uuid(),
    ticket_id        uuid           NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    game_code        varchar(32)    NOT NULL REFERENCES game (code),
    selection        text           NOT NULL, -- "12", "12-34", "xx12x"
    stake            numeric(12, 2) NOT NULL,
    odds_snapshot    numeric(12, 4) NOT NULL,
    potential_payout numeric(14, 2) NOT NULL,
    version          bigint         NOT NULL DEFAULT 0,
    created_at       timestamptz    NOT NULL DEFAULT now(),
    created_by       uuid,
    updated_at       timestamptz    NOT NULL DEFAULT now(),
    updated_by       uuid,
    deleted_at       timestamptz
);

CREATE INDEX ix_ticket_line_ticket ON ticket_line (ticket_id);


-- ===========================
-- 9. APP_USER & USER_PREFERENCE
-- ===========================

CREATE TABLE app_user
(
    id            uuid PRIMARY KEY,            -- Keycloak sub
    username      text        NOT NULL,
    email         citext,
    tenant_code   text        NOT NULL,        -- claim "tenant"
    tenant_id     uuid REFERENCES tenant (id), -- optionnel
    display_name  text,
    locale        text,
    last_login_at timestamptz,
    version       bigint      NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    created_by    uuid,
    updated_at    timestamptz NOT NULL DEFAULT now(),
    updated_by    uuid,
    deleted_at    timestamptz
);

CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE
    ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_app_user_tenant_code ON app_user (tenant_code);


CREATE TABLE user_preference
(
    id         uuid PRIMARY KEY,
    user_id    uuid  NOT NULL REFERENCES app_user (id),
    theme_mode varchar(10),
    density    smallint,
    locale     varchar(8),
    version    bigint      NOT NULL DEFAULT 0,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

CREATE TRIGGER trg_user_preference_updated_at
    BEFORE UPDATE
    ON user_preference
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- 9.b APP_ROLE & APP_USER_ROLE (rôles applicatifs)

CREATE TABLE app_role (
    id          uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    code        varchar(64) NOT NULL UNIQUE,
    name        varchar(128) NOT NULL,
    description text,
    version    bigint      NOT NULL DEFAULT 0,

    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid,
    deleted_at  timestamptz
);

CREATE TABLE app_user_role (
    user_id uuid NOT NULL REFERENCES app_user(id),
    role_id uuid NOT NULL REFERENCES app_role(id),
    PRIMARY KEY (user_id, role_id)
);


-- ===========================
-- 9.c TENANT_USER (liaison user <-> tenant)
-- ===========================

CREATE TABLE tenant_user (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      uuid NOT NULL REFERENCES tenant(id),
    user_id        uuid NOT NULL REFERENCES app_user(id),
    role           varchar(32) NOT NULL,   -- SUPER_ADMIN | TENANT_ADMIN | CASHIER | ...
    autonomy_level varchar(16) NOT NULL DEFAULT 'none',
    is_owner       boolean NOT NULL DEFAULT false,
    version        bigint      NOT NULL DEFAULT 0,
    created_at     timestamptz NOT NULL DEFAULT now(),
    created_by     uuid,
    updated_at     timestamptz NOT NULL DEFAULT now(),
    updated_by     uuid,
    deleted_at     timestamptz,

    UNIQUE (tenant_id, user_id)
);

CREATE INDEX ix_tenant_user_tenant_user ON tenant_user (tenant_id, user_id);

CREATE TRIGGER trg_tenant_user_updated_at
    BEFORE UPDATE
    ON tenant_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 10. PLAN & SUBSCRIPTION (SaaS)
-- ===========================

CREATE TABLE plan
(
    id                uuid PRIMARY KEY        DEFAULT gen_random_uuid(),
    code              varchar(64)    NOT NULL UNIQUE, -- BASIC|PRO|ENTERPRISE...
    name              varchar(128)   NOT NULL,
    description       text,
    price_amount      numeric(12, 2) NOT NULL DEFAULT 0,
    currency          varchar(3)     NOT NULL DEFAULT 'USD',
    billing_frequency varchar(16)    NOT NULL,        -- MONTH|YEAR
    public_plan       boolean        NOT NULL DEFAULT false,
    features          jsonb,
    version           bigint         NOT NULL DEFAULT 0,

    created_at        timestamptz    NOT NULL DEFAULT now(),
    created_by        uuid,
    updated_at        timestamptz    NOT NULL DEFAULT now(),
    updated_by        uuid,
    deleted_at        timestamptz
);

ALTER TABLE plan
    ADD CONSTRAINT chk_plan_billing_frequency
        CHECK (billing_frequency IN ('MONTH', 'YEAR'));

CREATE INDEX idx_plan_public ON plan (public_plan);


CREATE TABLE subscription
(
    id                   uuid PRIMARY KEY     DEFAULT gen_random_uuid(),
    version              bigint      NOT NULL DEFAULT 0,

    tenant_id            uuid        NOT NULL REFERENCES tenant (id),
    plan_id              uuid        NOT NULL REFERENCES plan (id),
    status               varchar(16) NOT NULL, -- ACTIVE|TRIALING|CANCELED|PAST_DUE|SUSPENDED
    current_period_start timestamptz,
    current_period_end   timestamptz,
    cancel_at_period_end boolean     NOT NULL DEFAULT false,
    billing_provider     varchar(16),          -- STRIPE|ADYEN|NONE
    billing_external_id  varchar(128),
    meta                 jsonb       NOT NULL DEFAULT '{}'::jsonb,

    created_at           timestamptz NOT NULL DEFAULT now(),
    created_by           uuid,
    updated_at           timestamptz NOT NULL DEFAULT now(),
    updated_by           uuid,
    deleted_at           timestamptz
);

CREATE UNIQUE INDEX ux_subscription_active_per_tenant
    ON subscription (tenant_id) WHERE status IN ('ACTIVE','TRIALING') AND cancel_at_period_end = false;

CREATE INDEX idx_subscription_tenant ON subscription (tenant_id);
CREATE INDEX idx_subscription_status ON subscription (status);

ALTER TABLE subscription
    ADD CONSTRAINT chk_subscription_status
        CHECK (status IN ('ACTIVE', 'TRIALING', 'CANCELED', 'PAST_DUE', 'SUSPENDED'));

ALTER TABLE subscription
    ADD CONSTRAINT chk_subscription_billing_provider
        CHECK (billing_provider IS NULL OR billing_provider IN ('STRIPE', 'ADYEN', 'NONE'));

CREATE TRIGGER trg_subscription_updated_at
    BEFORE UPDATE
    ON subscription
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ===========================
-- 11. THEME
-- ===========================
CREATE TABLE theme (
                       id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       version        bigint NOT NULL DEFAULT 0,

                       tenant_id      uuid REFERENCES tenant(id),  -- NULL = preset global
                       base_preset_id varchar(128) NOT NULL,
                       label          varchar(160) NOT NULL,
                       mode           varchar(10)  NOT NULL DEFAULT 'system', -- light|dark|system
                       density        smallint     NOT NULL DEFAULT 0,        -- 0|-1|-2
                       palette_json   jsonb        NOT NULL DEFAULT '{}'::jsonb,
                       tokens_json    jsonb        NOT NULL DEFAULT '{}'::jsonb,
                       css_vars_json  jsonb        NOT NULL DEFAULT '{}'::jsonb,
                       status         varchar(20)  NOT NULL DEFAULT 'draft',  -- draft|published|archived
                       theme_version  int          NOT NULL DEFAULT 1,

                       created_at     timestamptz  NOT NULL DEFAULT now(),
                       created_by     uuid,
                       updated_at     timestamptz  NOT NULL DEFAULT now(),
                       updated_by     uuid,
                       deleted_at     timestamptz
);

CREATE INDEX idx_theme_tenant_status ON theme(tenant_id, status);
CREATE INDEX idx_theme_base ON theme(base_preset_id);

CREATE TRIGGER trg_theme_updated_at
    BEFORE UPDATE ON theme
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ===========================
-- 12. AUDIT_EVENT (audit applicatif)
-- ===========================

CREATE TABLE audit_event
(
    id          uuid PRIMARY KEY      DEFAULT gen_random_uuid(),
    version     bigint       NOT NULL DEFAULT 0,

    tenant_id   uuid         NOT NULL REFERENCES tenant (id),
    occurred_at timestamptz  NOT NULL DEFAULT now(),

    actor_type  varchar(16)  NOT NULL, -- USER|TERMINAL|SYSTEM
    actor_id    varchar(128) NOT NULL,
    entity_type varchar(64)  NOT NULL, -- TICKET|DRAW|LIMIT|PLAN|...
    entity_id   varchar(128) NOT NULL,
    action      varchar(32)  NOT NULL, -- CREATE|UPDATE|DELETE|STATE_CHANGE|PAY...
    details     jsonb        NOT NULL DEFAULT '{}'::jsonb,
    ip          inet,
    user_agent  text,

    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid,
    deleted_at  timestamptz
);

CREATE INDEX ix_audit_event_tenant_time ON audit_event (tenant_id, occurred_at);

CREATE TRIGGER trg_audit_event_updated_at
    BEFORE UPDATE
    ON audit_event
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE UNIQUE INDEX IF NOT EXISTS idx_draw_tenant_channel_scheduled_unique
    ON draw (tenant_id, draw_channel_id, scheduled_at);
