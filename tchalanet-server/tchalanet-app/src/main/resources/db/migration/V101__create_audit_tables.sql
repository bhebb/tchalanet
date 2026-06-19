-- Envers revision table (custom — carries request context)
CREATE SEQUENCE tch_revinfo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE revinfo
(
    rev               integer PRIMARY KEY DEFAULT nextval('tch_revinfo_seq'),
    rev_timestamp     bigint  NOT NULL,
    tenant_id         uuid,
    user_id           uuid,
    request_id        varchar(128),
    actor_type        varchar(32),
    api_scope         varchar(32),
    tenant_overridden boolean NOT NULL DEFAULT false
);

-- ─── Draw / Schedule ─────────────────────────────────────────────────────────

CREATE TABLE result_slot_aud
(
    id             uuid    NOT NULL,
    rev            integer NOT NULL,
    revtype        smallint,
    tenant_id      uuid,
    slot_key       varchar(32),
    provider       varchar(16),
    timezone       varchar(64),
    draw_time      time,
    days_of_week   varchar(32),
    active         boolean,
    sort_order     integer,
    source_cfg     jsonb,
    projection_cfg jsonb,
    notes          text,
    label_key      varchar(256),
    created_at     timestamptz,
    created_by     uuid,
    updated_at     timestamptz,
    updated_by     uuid,
    deleted_at     timestamptz,
    deleted_by     uuid,
    version        bigint,
    CONSTRAINT pk_result_slot_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_result_slot_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE draw_channel_aud
(
    id              uuid    NOT NULL,
    rev             integer NOT NULL,
    revtype         smallint,
    tenant_id       uuid,
    code            varchar(64),
    name            varchar(128),
    period          varchar(32),
    timezone        varchar(64),
    draw_time       time,
    sales_open_time time,
    cutoff_sec      integer,
    days_of_week    varchar(32),
    active          boolean,
    sort_order      integer,
    flags           jsonb,
    notes           text,
    result_slot_id  uuid,
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    deleted_at      timestamptz,
    deleted_by      uuid,
    version         bigint,
    CONSTRAINT pk_draw_channel_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_draw_channel_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE draw_channel_game_aud
(
    id              uuid    NOT NULL,
    rev             integer NOT NULL,
    revtype         smallint,
    tenant_id       uuid,
    draw_channel_id uuid,
    game_id         uuid,
    enabled         boolean,
    flags           jsonb,
    created_at      timestamptz,
    created_by      uuid,
    updated_at      timestamptz,
    updated_by      uuid,
    deleted_at      timestamptz,
    deleted_by      uuid,
    version         bigint,
    CONSTRAINT pk_draw_channel_game_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_draw_channel_game_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE draw_result_aud
(
    id             uuid    NOT NULL,
    rev            integer NOT NULL,
    revtype        smallint,
    tenant_id      uuid,
    result_slot_id uuid,
    occurred_at    timestamptz,
    result_date    date,
    source_result  jsonb,
    haiti_result   jsonb,
    raw_payload    jsonb,
    flags          jsonb,
    status         varchar(16),
    quality        varchar(16),
    source         varchar(32),
    source_hash    varchar(64),
    fetched_at     timestamptz,
    override_reason text,
    created_at     timestamptz,
    created_by     uuid,
    updated_at     timestamptz,
    updated_by     uuid,
    deleted_at     timestamptz,
    deleted_by     uuid,
    version        bigint,
    CONSTRAINT pk_draw_result_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_draw_result_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE draw_aud
(
    id                     uuid    NOT NULL,
    rev                    integer NOT NULL,
    revtype                smallint,
    tenant_id              uuid,
    draw_channel_id        uuid,
    draw_date              date,
    scheduled_at           timestamptz,
    cutoff_at              timestamptz,
    opened_at              timestamptz,
    closed_at              timestamptz,
    resulted_at            timestamptz,
    settled_at             timestamptz,
    canceled_at            timestamptz,
    cancel_reason_code     varchar(96),
    cancel_reason_label    varchar(255),
    status                 varchar(16),
    draw_result_id         uuid,
    system_generated       boolean,
    locked                 boolean,
    result_source          varchar(16),
    result_override_reason text,
    result_overridden_at   timestamptz,
    created_at             timestamptz,
    created_by             uuid,
    updated_at             timestamptz,
    updated_by             uuid,
    deleted_at             timestamptz,
    deleted_by             uuid,
    version                bigint,
    CONSTRAINT pk_draw_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_draw_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ─── Games / Catalog ─────────────────────────────────────────────────────────

CREATE TABLE tenant_game_aud
(
    id                   uuid    NOT NULL,
    rev                  integer NOT NULL,
    revtype              smallint,
    tenant_id            uuid,
    game_id              uuid,
    game_code            varchar(32),
    enabled              boolean,
    visible_in_pos       boolean,
    display_name         varchar(128),
    display_order        integer,
    min_stake            numeric(12, 2),
    max_stake            numeric(12, 2),
    availability_enabled boolean,
    availability_days    varchar(64),
    start_local_time     time,
    end_local_time       time,
    created_at           timestamptz,
    created_by           uuid,
    updated_at           timestamptz,
    updated_by           uuid,
    deleted_at           timestamptz,
    deleted_by           uuid,
    version              bigint,
    CONSTRAINT pk_tenant_game_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_tenant_game_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ─── Pricing / Limits ────────────────────────────────────────────────────────

CREATE TABLE pricing_odds_aud
(
    id         uuid    NOT NULL,
    rev        integer NOT NULL,
    revtype    smallint,
    tenant_id  uuid,
    game_code  varchar(32),
    bet_type   varchar(32),
    bet_option smallint,
    odds       numeric(12, 4),
    active     boolean,
    created_at timestamptz,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version    bigint,
    CONSTRAINT pk_pricing_odds_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_pricing_odds_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE limit_assignment_aud
(
    id         uuid    NOT NULL,
    rev        integer NOT NULL,
    revtype    smallint,
    tenant_id  uuid,
    rule_key   varchar(80),
    scope_type varchar(32),
    scope_id   uuid,
    enabled    boolean,
    on_breach  varchar(32),
    params     jsonb,
    starts_at  timestamptz,
    ends_at    timestamptz,
    created_at timestamptz,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version    bigint,
    CONSTRAINT pk_limit_assignment_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_limit_assignment_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ─── Promotions ──────────────────────────────────────────────────────────────

CREATE TABLE promotion_campaign_aud
(
    id             uuid    NOT NULL,
    tenant_id      uuid    NOT NULL,
    rev            integer NOT NULL,
    revtype        smallint,
    code           varchar(96),
    name           varchar(160),
    status         varchar(32),
    priority       integer,
    starts_at      timestamptz,
    ends_at        timestamptz,
    config_version varchar(48),
    created_at     timestamptz,
    created_by     uuid,
    updated_at     timestamptz,
    updated_by     uuid,
    deleted_at     timestamptz,
    deleted_by     uuid,
    version        bigint,
    CONSTRAINT pk_promotion_campaign_aud PRIMARY KEY (id, rev)
);

CREATE TABLE promotion_rule_aud
(
    id                uuid    NOT NULL,
    tenant_id         uuid    NOT NULL,
    rev               integer NOT NULL,
    revtype           smallint,
    campaign_id       uuid,
    rule_key          varchar(96),
    priority          integer,
    min_paid_total    numeric(19, 4),
    before_local_time time,
    created_at        timestamptz,
    created_by        uuid,
    updated_at        timestamptz,
    updated_by        uuid,
    deleted_at        timestamptz,
    deleted_by        uuid,
    version           bigint,
    CONSTRAINT pk_promotion_rule_aud PRIMARY KEY (id, rev)
);

CREATE TABLE promotion_rule_effect_aud
(
    id                 uuid    NOT NULL,
    tenant_id          uuid    NOT NULL,
    rev                integer NOT NULL,
    revtype            smallint,
    rule_id            uuid,
    effect_type        varchar(32),
    game_code          varchar(64),
    payout_base_amount numeric(19, 4),
    quantity           integer,
    odds_override      numeric(19, 6),
    charge_type        varchar(64),
    created_at         timestamptz,
    created_by         uuid,
    updated_at         timestamptz,
    updated_by         uuid,
    deleted_at         timestamptz,
    deleted_by         uuid,
    version            bigint,
    CONSTRAINT pk_promotion_rule_effect_aud PRIMARY KEY (id, rev)
);

CREATE TABLE promotion_rule_eligibility_line_aud
(
    id         uuid    NOT NULL,
    tenant_id  uuid    NOT NULL,
    rev        integer NOT NULL,
    revtype    smallint,
    rule_id    uuid,
    game_code  varchar(64),
    min_count  integer,
    created_at timestamptz,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version    bigint,
    CONSTRAINT pk_promotion_rule_eligibility_line_aud PRIMARY KEY (id, rev)
);
