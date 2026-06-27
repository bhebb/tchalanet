-- V246 — archive legal hold
--
-- Legal holds block archive purge and partition cleanup for disputed or legally
-- sensitive records, datasets, tenants, or periods.

CREATE TABLE archive_legal_hold
(
    id                   uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            uuid         NULL,
    dataset_code         varchar(96)  NOT NULL,
    entity_type          varchar(64)  NULL,
    entity_id            varchar(128) NULL,
    period_start         date         NULL,
    period_end           date         NULL,
    reason               text         NOT NULL,
    status               varchar(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by_actor_id  uuid         NOT NULL,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    released_by_actor_id uuid         NULL,
    released_at          timestamptz  NULL,
    release_reason       text         NULL,

    CONSTRAINT chk_archive_legal_hold__status
        CHECK (status IN ('ACTIVE', 'RELEASED', 'EXPIRED')),
    CONSTRAINT chk_archive_legal_hold__reason
        CHECK (length(trim(reason)) >= 10),
    CONSTRAINT chk_archive_legal_hold__release_reason
        CHECK (release_reason IS NULL OR length(trim(release_reason)) >= 10),
    CONSTRAINT chk_archive_legal_hold__period
        CHECK (period_start IS NULL OR period_end IS NULL OR period_start < period_end)
);

CREATE INDEX ix_archive_legal_hold__dataset_period
    ON archive_legal_hold (dataset_code, status, period_start, period_end);

CREATE INDEX ix_archive_legal_hold__tenant_dataset
    ON archive_legal_hold (tenant_id, dataset_code, status);

CREATE INDEX ix_archive_legal_hold__entity
    ON archive_legal_hold (dataset_code, entity_type, entity_id, status)
    WHERE entity_id IS NOT NULL;

ALTER TABLE archive_legal_hold ENABLE ROW LEVEL SECURITY;
ALTER TABLE archive_legal_hold FORCE ROW LEVEL SECURITY;

CREATE POLICY archive_legal_hold_platform_only ON archive_legal_hold
    FOR ALL
    USING (public.allow_platform_cross_tenant_select())
    WITH CHECK (public.allow_platform_cross_tenant_select());

GRANT SELECT, INSERT, UPDATE ON archive_legal_hold TO app_user;
