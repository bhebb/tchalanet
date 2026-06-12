-- V228 — archive registry (data-lifecycle-archive-v1 Phase 4)
--
-- archive_run:          one row per archive job execution.
-- archive_object:       one row per exported compressed file.
-- archive_lookup_index: per-entity pointer into archive objects; online RLS boundary.
--
-- RLS design:
--   archive_run / archive_object  — platform scope only (SUPER_ADMIN).
--   archive_lookup_index          — tenant/admin sees own rows (tenant_id = current_tenant);
--                                   platform sees all rows including tenant_id IS NULL.

-- ────────────────────────────────────────────────
-- archive_run
-- ────────────────────────────────────────────────
CREATE TABLE archive_run
(
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    status           varchar(32)  NOT NULL,
    strategy         varchar(32)  NOT NULL,
    trigger_type     varchar(32)  NOT NULL,
    idempotency_key  varchar(160) NOT NULL,
    started_at       timestamptz  NOT NULL DEFAULT now(),
    completed_at     timestamptz  NULL,
    requested_by     uuid         NULL,
    reason           text         NULL,
    error_message    text         NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_archive_run__idem UNIQUE (idempotency_key),
    CONSTRAINT chk_archive_run__status
        CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_archive_run__strategy
        CHECK (strategy IN ('MONTHLY', 'FILL_MONITOR', 'MANUAL')),
    CONSTRAINT chk_archive_run__trigger
        CHECK (trigger_type IN ('SCHEDULED', 'MANUAL', 'ROLLOVER_MONITOR'))
);

ALTER TABLE archive_run ENABLE ROW LEVEL SECURITY;
ALTER TABLE archive_run FORCE ROW LEVEL SECURITY;

CREATE POLICY archive_run_platform_only ON archive_run
    FOR ALL
    USING (public.allow_platform_cross_tenant_select());

GRANT SELECT, INSERT, UPDATE ON archive_run TO app_user;

-- ────────────────────────────────────────────────
-- archive_object
-- ────────────────────────────────────────────────
CREATE TABLE archive_object
(
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    archive_run_id   uuid         NOT NULL REFERENCES archive_run (id),
    table_name       varchar(96)  NOT NULL,
    tenant_id        uuid         NULL,
    period_start     date         NULL,
    period_end       date         NULL,
    lower_bound_at   timestamptz  NULL,
    upper_bound_at   timestamptz  NULL,
    segment_no       int          NOT NULL DEFAULT 0,
    object_uri       text         NOT NULL,
    format           varchar(32)  NOT NULL DEFAULT 'JSONL_GZ',
    compression      varchar(32)  NOT NULL DEFAULT 'GZIP',
    row_count        bigint       NOT NULL DEFAULT 0,
    byte_size        bigint       NOT NULL DEFAULT 0,
    checksum_sha256  varchar(128) NOT NULL,
    schema_version   int          NOT NULL DEFAULT 1,
    status           varchar(32)  NOT NULL DEFAULT 'PENDING',
    created_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT chk_archive_object__status
        CHECK (status IN ('PENDING', 'VERIFIED', 'INVALID'))
);

CREATE INDEX ix_archive_object__run
    ON archive_object (archive_run_id);
CREATE INDEX ix_archive_object__table_period
    ON archive_object (table_name, period_start, period_end)
    WHERE period_start IS NOT NULL;

ALTER TABLE archive_object ENABLE ROW LEVEL SECURITY;
ALTER TABLE archive_object FORCE ROW LEVEL SECURITY;

CREATE POLICY archive_object_platform_only ON archive_object
    FOR ALL
    USING (public.allow_platform_cross_tenant_select());

GRANT SELECT, INSERT, UPDATE ON archive_object TO app_user;

-- ────────────────────────────────────────────────
-- archive_lookup_index
-- ────────────────────────────────────────────────
CREATE TABLE archive_lookup_index
(
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name        varchar(96) NOT NULL,
    tenant_id         uuid        NULL,
    entity_type       varchar(64) NULL,
    entity_id         uuid        NULL,
    public_code       varchar(96) NULL,
    business_date     date        NULL,
    occurred_at       timestamptz NULL,
    archive_object_id uuid        NOT NULL REFERENCES archive_object (id),
    -- Future-seek columns: byte offset + length within the compressed object
    -- for O(1) extraction without full decompress. Null until producer sets them.
    object_offset     bigint      NULL,
    object_length     bigint      NULL,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_archive_lookup__tenant_entity
    ON archive_lookup_index (tenant_id, entity_type, entity_id)
    WHERE entity_id IS NOT NULL;
CREATE INDEX ix_archive_lookup__tenant_code
    ON archive_lookup_index (tenant_id, public_code)
    WHERE public_code IS NOT NULL;
CREATE INDEX ix_archive_lookup__table_date
    ON archive_lookup_index (table_name, business_date)
    WHERE business_date IS NOT NULL;

ALTER TABLE archive_lookup_index ENABLE ROW LEVEL SECURITY;
ALTER TABLE archive_lookup_index FORCE ROW LEVEL SECURITY;

-- Tenant/admin: own rows only (never see tenant_id IS NULL rows)
CREATE POLICY archive_lookup_tenant_read ON archive_lookup_index
    FOR SELECT
    USING (
        public.current_tenant() IS NOT NULL
        AND tenant_id = public.current_tenant()
    );

-- Platform: all rows including global (tenant_id IS NULL)
CREATE POLICY archive_lookup_platform_read ON archive_lookup_index
    FOR SELECT
    USING (public.allow_platform_cross_tenant_select());

-- Insert: service account only (archive orchestrator)
CREATE POLICY archive_lookup_insert ON archive_lookup_index
    FOR INSERT
    WITH CHECK (true);

GRANT SELECT, INSERT ON archive_lookup_index TO app_user;
