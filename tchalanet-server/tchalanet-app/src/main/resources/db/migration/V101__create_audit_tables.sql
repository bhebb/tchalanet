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

-- ─── Entity revision allowlist ───────────────────────────────────────────────

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

CREATE TABLE seller_terminal_aud
(
    id                   uuid    NOT NULL,
    rev                  integer NOT NULL,
    revtype              smallint,
    tenant_id            uuid,
    terminal_code        varchar(64),
    terminal_code_mod    boolean,
    status               varchar(32),
    status_mod           boolean,
    commission_rate      numeric(5, 2),
    commission_rate_mod  boolean,
    blocked_at           timestamptz,
    blocked_at_mod       boolean,
    blocked_by           uuid,
    blocked_by_mod       boolean,
    blocked_reason       varchar(500),
    blocked_reason_mod   boolean,
    disabled_at          timestamptz,
    disabled_at_mod      boolean,
    must_change_pin      boolean,
    must_change_pin_mod  boolean,
    pin_reset_at         timestamptz,
    pin_reset_at_mod     boolean,
    created_at           timestamptz,
    created_by           uuid,
    updated_at           timestamptz,
    updated_by           uuid,
    deleted_at           timestamptz,
    deleted_by           uuid,
    version              bigint,
    CONSTRAINT pk_seller_terminal_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_seller_terminal_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
