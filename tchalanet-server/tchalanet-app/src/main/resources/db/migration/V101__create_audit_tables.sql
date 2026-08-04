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

-- ============================================================
-- audit_log — canonical business audit trail (data-lifecycle-archive-v1)
-- Partitioned by occurred_at (monthly RANGE). actor_id is app_user.id when present.
-- Self-contained: partitions + indexes + RLS + grant.
-- Future monthly partitions are added by the archive scheduler.
-- ============================================================
CREATE TABLE audit_log
(
    id               uuid         NOT NULL,
    tenant_id        uuid         NULL,
    occurred_at      timestamptz  NOT NULL,
    business_date    date         NULL,
    actor_id         uuid         NULL,
    actor_type       varchar(32)  NOT NULL,
    action           varchar(96)  NOT NULL,
    entity_type      varchar(64)  NOT NULL,
    entity_id        varchar(255) NULL,
    severity         varchar(16)  NOT NULL DEFAULT 'INFO',
    source           varchar(32)  NOT NULL DEFAULT 'API',
    correlation_id   varchar(96)  NULL,
    request_id       varchar(96)  NULL,
    details          jsonb        NULL,
    ip               inet         NULL,
    user_agent       text         NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_log PRIMARY KEY (id, occurred_at),
    CONSTRAINT chk_audit_log__severity
        CHECK (severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_audit_log__source
        CHECK (source IN ('API', 'SYSTEM', 'BATCH', 'SCHEDULER', 'IMPORT', 'MIGRATION'))
) PARTITION BY RANGE (occurred_at);

CREATE TABLE audit_log_2026_06
    PARTITION OF audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE audit_log_2026_07
    PARTITION OF audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE INDEX ix_audit_log__business_date
    ON audit_log (business_date);
CREATE INDEX ix_audit_log__tenant_time
    ON audit_log (tenant_id, occurred_at DESC);
CREATE INDEX ix_audit_log__tenant_entity
    ON audit_log (tenant_id, entity_type, entity_id, occurred_at DESC);
CREATE INDEX ix_audit_log__actor_time
    ON audit_log (tenant_id, actor_id, occurred_at DESC);
CREATE INDEX ix_audit_log__action_time
    ON audit_log (tenant_id, action, occurred_at DESC);

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

CREATE POLICY audit_log_rls_tenant ON audit_log
    FOR SELECT
    USING (
    public.allow_platform_cross_tenant_select()
        OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    );

CREATE POLICY audit_log_rls_insert ON audit_log
    FOR INSERT
    WITH CHECK (true);

GRANT SELECT, INSERT ON audit_log TO app_user;
