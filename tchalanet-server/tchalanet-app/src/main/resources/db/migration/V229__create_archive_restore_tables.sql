-- V229 — temporary archive restore tables (data-lifecycle-archive-v1 Phase 6)
--
-- Used by SUPER_ADMIN restore operations only.
-- Rows are TTL-cleaned by the archive maintenance job.
-- No RLS — outside RLS by design (may contain cross-tenant investigation data).
-- All access enforced at service layer with SUPER_ADMIN permission + mandatory reason.

CREATE TABLE archive_restore_run
(
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by     uuid        NOT NULL,
    reason           text        NOT NULL,
    status           varchar(32) NOT NULL DEFAULT 'ACTIVE',
    row_count        bigint      NOT NULL DEFAULT 0,
    archive_run_ids  jsonb       NOT NULL DEFAULT '[]',
    expires_at       timestamptz NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_archive_restore_run__status
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLEANED'))
);

CREATE INDEX ix_archive_restore_run__expires
    ON archive_restore_run (expires_at)
    WHERE status = 'ACTIVE';

GRANT SELECT, INSERT, UPDATE ON archive_restore_run TO app_user;

-- Restored ticket rows (denormalized: ticket + lines + charges in JSON)
CREATE TABLE archive_restore_ticket
(
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    restore_run_id      uuid        NOT NULL REFERENCES archive_restore_run (id),
    tenant_id           uuid        NOT NULL,
    ticket_id           uuid        NOT NULL,
    public_code         varchar(64) NULL,
    business_date       date        NULL,
    archive_object_id   uuid        NOT NULL,
    payload             jsonb       NOT NULL,
    schema_version      int         NOT NULL DEFAULT 1,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_archive_restore_ticket__run
    ON archive_restore_ticket (restore_run_id);
CREATE INDEX ix_archive_restore_ticket__tenant_id
    ON archive_restore_ticket (tenant_id, ticket_id);

GRANT SELECT, INSERT ON archive_restore_ticket TO app_user;

-- Restored audit_log rows
CREATE TABLE archive_restore_audit_log
(
    id                  uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    restore_run_id      uuid        NOT NULL REFERENCES archive_restore_run (id),
    tenant_id           uuid        NULL,
    original_id         uuid        NOT NULL,
    occurred_at         timestamptz NOT NULL,
    archive_object_id   uuid        NOT NULL,
    payload             jsonb       NOT NULL,
    schema_version      int         NOT NULL DEFAULT 1,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_archive_restore_audit__run
    ON archive_restore_audit_log (restore_run_id);
CREATE INDEX ix_archive_restore_audit__tenant_time
    ON archive_restore_audit_log (tenant_id, occurred_at DESC);

GRANT SELECT, INSERT ON archive_restore_audit_log TO app_user;
