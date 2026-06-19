-- V226 — partitioned audit_log table (data-lifecycle-archive-v1)
--
-- Replaces the simple audit_event table over time.
-- Partitioned by occurred_at (monthly RANGE partitions).
-- actor_id is always app_user.id (UUID), never a provider subject.
-- Two bootstrap partitions seeded here; future months added by archive scheduler.

-- ────────────────────────────────────────────────
-- Root table
-- ────────────────────────────────────────────────
CREATE TABLE audit_log
(
    id               uuid         NOT NULL,
    tenant_id        uuid         NULL,
    occurred_at      timestamptz  NOT NULL,
    business_date    date         NULL,
    actor_id         uuid         NULL,
    actor_type       varchar(32)  NULL,
    action           varchar(96)  NOT NULL,
    entity_type      varchar(64)  NOT NULL,
    entity_id        uuid         NULL,
    severity         varchar(16)  NOT NULL DEFAULT 'INFO',
    source           varchar(32)  NOT NULL DEFAULT 'API',
    correlation_id   varchar(96)  NULL,
    request_id       varchar(96)  NULL,
    details          jsonb        NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_log PRIMARY KEY (id, occurred_at),
    CONSTRAINT chk_audit_log__severity
        CHECK (severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_audit_log__source
        CHECK (source IN ('API', 'SYSTEM', 'BATCH', 'SCHEDULER', 'IMPORT', 'MIGRATION'))
) PARTITION BY RANGE (occurred_at);

-- ────────────────────────────────────────────────
-- Bootstrap partitions — current + next month
-- ────────────────────────────────────────────────
CREATE TABLE audit_log_2026_06
    PARTITION OF audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE audit_log_2026_07
    PARTITION OF audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

-- ────────────────────────────────────────────────
-- Indexes (on root table — inherited by partitions)
-- ────────────────────────────────────────────────
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

-- ────────────────────────────────────────────────
-- RLS
-- ────────────────────────────────────────────────
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

-- Tenant/admin: own rows only
CREATE POLICY audit_log_rls_tenant ON audit_log
    FOR SELECT
    USING (
    public.allow_platform_cross_tenant_select()
        OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant())
    );

-- Insert: service account writes directly (no tenant check needed at DB level)
CREATE POLICY audit_log_rls_insert ON audit_log
    FOR INSERT
    WITH CHECK (true);

-- ────────────────────────────────────────────────
-- Grants
-- ────────────────────────────────────────────────
GRANT SELECT, INSERT ON audit_log TO app_user;
