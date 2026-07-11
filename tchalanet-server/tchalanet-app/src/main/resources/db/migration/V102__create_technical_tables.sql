-- Baseline: technical runtime tables
CREATE TABLE processed_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  handler_key varchar(160) NOT NULL,
  event_id uuid NOT NULL,
  processed_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  CONSTRAINT uq_processed_event__tenant_handler_event UNIQUE (tenant_id, handler_key, event_id)
);

CREATE TABLE idempotency_record (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  scope varchar(80) NOT NULL,
  idem_key varchar(200) NOT NULL,
  request_hash varchar(64) NOT NULL,
  status varchar(20) NOT NULL,
  resource_id uuid,
  response_json text,
  expires_at timestamptz NOT NULL,
  created_at timestamptz DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_idempotency_record__tenant_scope_key UNIQUE (tenant_id, scope, idem_key)
);

CREATE TABLE stats_draw (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  draw_id uuid NOT NULL UNIQUE REFERENCES draw(id),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  game_code varchar(64) NOT NULL,
  scheduled_at timestamptz NOT NULL,
  tickets_count bigint NOT NULL DEFAULT 0,
  stake_sum_cents bigint NOT NULL DEFAULT 0,
  winnings_sum_cents bigint NOT NULL DEFAULT 0,
  net_revenue_cents bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid
);

CREATE TABLE stats_daily (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dimension_type varchar(255) NOT NULL,
  dimension_id uuid,
  ref_date date NOT NULL,
  tickets_count bigint NOT NULL DEFAULT 0,
  tickets_cancelled_count bigint NOT NULL DEFAULT 0,
  stake_sum_cents bigint NOT NULL DEFAULT 0,
  winnings_sum_cents bigint NOT NULL DEFAULT 0,
  net_revenue_cents bigint NOT NULL DEFAULT 0,
  payouts_count bigint NOT NULL DEFAULT 0,
  sessions_opened_count bigint NOT NULL DEFAULT 0,
  sessions_closed_count bigint NOT NULL DEFAULT 0,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid
);

CREATE TABLE stats_event_log (
  event_id uuid PRIMARY KEY,
  event_type varchar(255) NOT NULL,
  processed_at timestamptz NOT NULL
);

CREATE TABLE shedlock (
  name varchar(64) PRIMARY KEY,
  lock_until timestamptz NOT NULL,
  locked_at timestamptz NOT NULL,
  locked_by varchar(255) NOT NULL
);

-- Technical security/session tables for cross-origin portal authentication handoff.
CREATE TABLE support_access_session (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  super_admin_user_id uuid NOT NULL REFERENCES app_user(id),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  tenant_code varchar(128) NOT NULL,
  tenant_name varchar(255) NOT NULL,
  mode varchar(32) NOT NULL,
  reason varchar(1024) NOT NULL,
  granted_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  cleared_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_support_access_session__mode
    CHECK (mode IN ('SUPPORT_OVERRIDE', 'SUPPORT_READONLY'))
);

CREATE INDEX ix_support_access_session__super_admin
  ON support_access_session (super_admin_user_id);

CREATE INDEX ix_support_access_session__tenant
  ON support_access_session (tenant_id);

CREATE INDEX ix_support_access_session__current
  ON support_access_session (super_admin_user_id, granted_at DESC)
  WHERE cleared_at IS NULL;

GRANT SELECT, INSERT, UPDATE ON support_access_session TO app_user;

CREATE TABLE portal_auth_handoff (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code_hash varchar(64) NOT NULL,
  subject_user_id uuid NOT NULL REFERENCES app_user(id),
  target_portal varchar(32) NOT NULL,
  entry_route varchar(512) NOT NULL,
  support_access_session_id uuid REFERENCES support_access_session(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  CONSTRAINT chk_portal_auth_handoff__target
    CHECK (target_portal IN ('ADMIN', 'PLATFORM')),
  CONSTRAINT chk_portal_auth_handoff__entry_relative
    CHECK (entry_route LIKE '/%' AND entry_route NOT LIKE '//%')
);

CREATE INDEX ix_portal_auth_handoff__expires_at
  ON portal_auth_handoff (expires_at);

CREATE INDEX ix_portal_auth_handoff__subject_user
  ON portal_auth_handoff (subject_user_id);

CREATE INDEX ix_portal_auth_handoff__unconsumed
  ON portal_auth_handoff (id, expires_at)
  WHERE consumed_at IS NULL;

GRANT SELECT, INSERT, UPDATE ON portal_auth_handoff TO app_user;

-- ============================================================
-- ARCHIVE DOMAIN (data-lifecycle-archive-v1) — self-contained: tables + indexes + RLS + grants
--   archive_run / archive_object / archive_lookup_index : registry (platform scope)
--   archive_restore_*                                   : SUPER_ADMIN restore (no RLS by design)
--   archive_legal_hold                                  : purge blocks (platform scope)
--   archive_* partition helpers                         : monthly partition maintenance
-- ============================================================

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
CREATE POLICY archive_lookup_tenant_read ON archive_lookup_index
    FOR SELECT
    USING (
        public.current_tenant() IS NOT NULL
        AND tenant_id = public.current_tenant()
    );
CREATE POLICY archive_lookup_platform_read ON archive_lookup_index
    FOR SELECT
    USING (public.allow_platform_cross_tenant_select());
CREATE POLICY archive_lookup_insert ON archive_lookup_index
    FOR INSERT
    WITH CHECK (true);
GRANT SELECT, INSERT ON archive_lookup_index TO app_user;

-- Restore tables — no RLS by design (SUPER_ADMIN-only, may hold cross-tenant data).
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

-- ── Partition maintenance helpers (safe for already-partitioned tables e.g. audit_log) ──
CREATE OR REPLACE FUNCTION public.archive_month_partition_name(
  p_parent_table text,
  p_month date
)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT lower(regexp_replace(p_parent_table, '[^a-zA-Z0-9_]', '_', 'g'))
         || '_' || to_char(date_trunc('month', p_month)::date, 'YYYY_MM')
$$;

CREATE OR REPLACE FUNCTION public.archive_ensure_month_partition(
  p_parent_table regclass,
  p_month date
)
RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
  v_parent_name text := p_parent_table::text;
  v_partition_name text;
  v_from date := date_trunc('month', p_month)::date;
  v_to date := (date_trunc('month', p_month)::date + INTERVAL '1 month')::date;
  v_relkind "char";
BEGIN
  SELECT c.relkind
    INTO v_relkind
    FROM pg_class c
   WHERE c.oid = p_parent_table;

  IF v_relkind IS DISTINCT FROM 'p' THEN
    RAISE EXCEPTION 'archive partition helper refused: % is not a partitioned table', v_parent_name;
  END IF;

  v_partition_name := public.archive_month_partition_name(v_parent_name, v_from);

  IF to_regclass(v_partition_name) IS NULL THEN
    EXECUTE format(
      'CREATE TABLE %I PARTITION OF %s FOR VALUES FROM (%L) TO (%L)',
      v_partition_name,
      p_parent_table,
      v_from,
      v_to
    );
  END IF;

  RETURN v_partition_name;
END;
$$;

CREATE OR REPLACE FUNCTION public.archive_ensure_month_partitions(
  p_parent_table regclass,
  p_start_month date,
  p_month_count integer
)
RETURNS TABLE(partition_name text)
LANGUAGE plpgsql
AS $$
DECLARE
  i integer;
BEGIN
  IF p_month_count < 1 OR p_month_count > 120 THEN
    RAISE EXCEPTION 'archive partition helper refused: p_month_count must be between 1 and 120';
  END IF;

  FOR i IN 0..(p_month_count - 1) LOOP
    partition_name := public.archive_ensure_month_partition(
      p_parent_table,
      (date_trunc('month', p_start_month)::date + (i || ' months')::interval)::date
    );
    RETURN NEXT;
  END LOOP;
END;
$$;

-- Keep audit_log partition coverage ahead of the current month.
SELECT public.archive_ensure_month_partitions(
  'audit_log'::regclass,
  date_trunc('month', CURRENT_DATE)::date,
  13
);
