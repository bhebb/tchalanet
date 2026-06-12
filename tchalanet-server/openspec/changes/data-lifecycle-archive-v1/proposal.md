# Spec — Tchalanet Data Lifecycle, Audit Retention & Low-Cost Archive V1

**Status:** READY FOR IMPLEMENTATION AFTER REVIEW
**Scope:** `tchalanet-server` — `platform.archive`, `audit_log`, stats projections, partitioning,
retention, archive lookup
**Owner module:** `platform.archive`

---

## Firebase migration note

The Keycloak → Firebase migration is in progress (`tch.identity.provider`). This spec is
provider-neutral by design:

- `audit_log.actor_id` is always the internal `app_user.id` (UUID), never a Firebase UID or
  Keycloak subject. The identity layer resolves `ExternalAuthenticatedUser` → `UserId` before
  any audit write.
- `archive_run.requested_by` is likewise the internal `app_user.id`.
- `app_user_external_identity` (CLASS A) stores the provider↔internal-user mapping; no archive
  or audit schema changes are required when switching providers.

---

## 1. Goal

Define a pragmatic data lifecycle strategy so high-volume transactional data can grow without
degrading operational performance.

The strategy must support:

- fast writes for ticket sales, payouts, settlements, audit logs and operational events;
- fast dashboards through projections, not heavy live queries;
- selective Envers usage only where historical state reconstruction is valuable;
- functional audit logs with controlled volume;
- low-cost archival for old data;
- ability to retrieve archived tickets, payouts and audit records when needed;
- no expensive external analytics platform required for V1.

---

## 2. Core principle

Tchalanet data is split into four lifecycle classes:

```
CLASS A — Permanent operational master data
CLASS B — Config/security data with historical versioning
CLASS C — High-volume transactional data
CLASS D — Derived/read-model data
```

Lifecycle rules differ per class.

---

## 3. Data classes

### 3.1 CLASS A — Permanent operational master data

Not archived by default.

Examples:

```
tenant
app_user
app_user_external_identity    ← includes Firebase UID ↔ app_user.id mapping
tenant_membership
role_assignment
permission mapping
tenant subscription baseline
```

Rules:

- keep in primary PostgreSQL;
- soft-delete where appropriate;
- Envers allowed for security-sensitive or admin-managed data;
- no monthly archival by default;
- must remain queryable online.

### 3.2 CLASS B — Config/security data with historical versioning

Low/medium volume, sensitive, needs historical reconstruction.

Examples:

```
tenant_settings
tenant_game_settings
outlet
terminal
limit_policy
promotion_campaign
promotion_rule
promotion_effect
pricing / commission policy
business_day_override
draw_channel tenant configuration
```

Rules:

- keep online in primary PostgreSQL;
- use Envers selectively;
- functional audit log for admin actions;
- no cold archive in V1 unless tenant is deleted or data is legally expired.

### 3.3 CLASS C — High-volume transactional data

Grows continuously; archival after retention period.

Examples:

```
ticket
ticket_line
ticket_charge
ticket_promotion_snapshot
payout
payout_workflow_history
audit_log
notification_delivery_log
provider_raw_payload
settlement detail
sales_session history
```

Rules:

- no Envers by default on high-volume rows;
- use immutable snapshots for business proof;
- use functional audit logs for user/admin actions;
- partition by time where possible;
- archive after retention threshold;
- archived data must remain retrievable through explicit archive lookup paths.

### 3.4 CLASS D — Derived/read-model data

Projections rebuilt from events or transactional sources.

Examples:

```
sales_daily_stats
sales_session_stats
sales_draw_stats
sales_selection_stats
dashboard projections
processed_event
search/index helper tables
```

Rules:

- no Envers;
- no functional audit except ops/admin rebuild actions;
- can be deleted/rebuilt;
- short-to-medium retention depending on dashboard/reporting needs;
- not treated as legal source of truth.

---

## 4. Audit strategy

### 4.1 Functional audit

Functional audit answers:

> Who did what, on which entity, when, for which tenant, from which operational context?

**Naming rule:**

```
action = <DOMAIN>_<VERB>
```

The action string is stable across the codebase and is reused in:
`@AuditLog(action = ...)`, `audit_log.action`, archive-search filters, ops screens.

Do not introduce variants for lifecycle statuses. Use `ARCHIVE_RUN` as the action and put
`COMPLETED` in `archive_run.status` and `audit_log.details`.

| Action             | Emitted by                     |
|--------------------|-------------------------------|
| TICKET_SELL        | core.sales                    |
| TICKET_VOID        | core.sales                    |
| PAYOUT_REQUEST     | core.payout                   |
| PAYOUT_APPROVE     | core.payout                   |
| PAYOUT_REJECT      | core.payout                   |
| PAYOUT_PAID        | core.payout                   |
| LIMIT_UPDATE       | core.limitpolicy              |
| PROMOTION_ACTIVATE | core.promotion                |
| PROMOTION_PAUSE    | core.promotion                |
| ROLE_ASSIGN        | platform.accesscontrol        |
| OUTLET_LOCK        | core.outlet                   |
| OUTLET_UNLOCK      | core.outlet                   |
| TERMINAL_LOCK      | core.terminal                 |
| TERMINAL_UNLOCK    | core.terminal                 |
| OPS_FORCE_JOB      | ops/scheduler                 |
| TENANT_OVERRIDE    | context/super-admin override  |
| CACHE_CLEAR        | ops                           |
| ARCHIVE_RUN        | platform.archive              |
| ARCHIVE_RESTORE    | platform.archive              |

Rules:

- one business action = one audit row;
- do not audit each ticket line;
- do not audit normal dashboard reads;
- do not audit cache misses;
- do not audit projector internal events;
- store important searchable fields as columns;
- keep `details` JSONB small;
- partition `audit_log` by month;
- archive old partitions.

### 4.2 Envers

Envers is limited to low/medium-volume tables where state reconstruction matters.

**Envers ON allowlist:**

```
tenant
app_user
tenant_membership
role_assignment
outlet
terminal
limit_policy
promotion_campaign
promotion_rule
promotion_effect
tenant_game_settings
pricing / commission policy
business_day_override
tenant settings critical to money/security
```

**Envers OFF blocklist:**

```
ticket_line
ticket_charge
audit_log
notification_delivery_log
idempotency_record
processed_event
dashboard stats
sales projections
provider_raw_payload
```

Ticket and payout rely on immutable snapshots + functional workflow audit, not full Envers.

---

## 5. Archive architecture

### 5.1 Strategy: monthly archival + fill-based safety monitor

**Primary strategy:** calendar-based monthly archival of closed periods after retention delay.

**Safety strategy:** fill-based threshold monitor when a partition exceeds row/byte thresholds.

**V1 decision:** Monthly partitioning is the physical DB strategy. Fill-based threshold is a
safety monitor/alert only — do not implement complex mid-month rollover DDL in V1.

**Monthly archive targets:**
`audit_log`, `ticket`, `ticket_line`, `payout`, settlement detail, `notification_delivery_log`,
`provider_raw_payload`

**Fill-based watch targets:**
`ticket_line`, `provider_raw_payload`, `audit_log`

---

## 6. PostgreSQL partitioning

### 6.1 Hot tables to partition

| Table                    | Physical partition key |
|--------------------------|----------------------|
| audit_log                | occurred_at          |
| ticket                   | sold_at              |
| ticket_line              | sold_at              |
| payout                   | created_at           |
| notification_delivery_log| created_at           |
| provider_raw_payload     | fetched_at           |

Physical partition by `occurred_at`/`sold_at`/`created_at`. Keep `business_date` as an indexed
reporting column. Always persist both the event timestamp and the tenant business date.

### 6.2 Example audit_log table

```sql
CREATE TABLE audit_log (
  id uuid NOT NULL,
  tenant_id uuid NULL,
  occurred_at timestamptz NOT NULL,
  business_date date NULL,
  actor_id uuid NULL,           -- always app_user.id, never provider subject
  actor_type varchar(32) NULL,
  action varchar(96) NOT NULL,
  entity_type varchar(64) NOT NULL,
  entity_id uuid NULL,
  severity varchar(16) NOT NULL,
  source varchar(32) NOT NULL,
  outlet_id uuid NULL,
  terminal_id uuid NULL,
  sales_session_id uuid NULL,
  correlation_id varchar(96) NULL,
  request_id varchar(96) NULL,
  details jsonb NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);
```

### 6.3 Example monthly partition

```sql
CREATE TABLE audit_log_2026_06
PARTITION OF audit_log
FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
```

### 6.4 Minimal indexes

```sql
CREATE INDEX ix_audit_log_business_date
  ON audit_log (business_date);
CREATE INDEX ix_audit_log_tenant_time
  ON audit_log (tenant_id, occurred_at DESC);
CREATE INDEX ix_audit_log_tenant_entity
  ON audit_log (tenant_id, entity_type, entity_id, occurred_at DESC);
CREATE INDEX ix_audit_log_actor_time
  ON audit_log (tenant_id, actor_id, occurred_at DESC);
CREATE INDEX ix_audit_log_action_time
  ON audit_log (tenant_id, action, occurred_at DESC);
```

Rules:

- avoid GIN index on `details` JSONB in V1;
- promote searchable fields to columns;
- keep write path cheap.

---

## 7. Archive storage

V1 uses low-cost object storage: S3-compatible (MinIO for self-host/dev; AWS/GCS/Azure later).

**Archive format:** `jsonl.gz` for V1; parquet optional in V2.

**Path convention:**

```
archive/{env}/{table}/{tenant_or_global}/{yyyy}/{mm}/{segment_id}.jsonl.gz
```

Examples:

```
archive/prod/ticket/tenant-123/2026/01/ticket_2026_01_part_001.jsonl.gz
archive/prod/audit_log/global/2026/01/audit_log_2026_01_part_001.jsonl.gz
```

Target compressed object size: 256 MB to 512 MB. Do not generate multi-GB objects in V1.

---

## 8. Archive tenant and RLS model

### 8.1 Core rule

All tenant-scoped archived data keeps `tenant_id`. The system must never rely on a
client-provided `tenant_id` as source of truth.

### 8.2 RLS by storage layer

| Storage layer              | tenant_id     | RLS?               | Access model                                      |
|----------------------------|---------------|--------------------|---------------------------------------------------|
| Hot PostgreSQL tables      | yes           | yes                | normal tenant/admin/platform context              |
| archive_lookup_index       | yes/null      | yes, scope-aware   | tenant/admin sees own rows; platform sees all     |
| archive_object             | optional      | restricted service | not exposed directly to tenant users              |
| Archive files (object store)| yes in payload| no DB RLS         | backend service only, after permission + lookup   |
| archive_restore_* tables   | may be cross  | no                 | SUPER_ADMIN only, mandatory reason + audit        |

### 8.3 Archive files

- Every row in tenant-scoped files must include `tenant_id`.
- Object URIs are never returned to tenant/admin users.
- Archive files are fetched only by backend services after permission and lookup checks.
- Tenant/admin lookups must first pass through `archive_lookup_index` under RLS.
- Platform restore requires `SUPER_ADMIN` permission, reason, and audit.

### 8.4 Archive lookup index RLS

`archive_lookup_index` is the online security boundary for archive discovery.

- Tenant/admin scope: only rows where `tenant_id = current tenant`.
- Platform scope: all rows including `tenant_id IS NULL`.
- `tenant_id IS NULL` means global/platform-only metadata.
- Tenant/admin callers must never see global rows.

```sql
ALTER TABLE archive_lookup_index ENABLE ROW LEVEL SECURITY;

CREATE POLICY archive_lookup_tenant_read ON archive_lookup_index
  FOR SELECT
  USING (
    current_setting('tch.scope', true) IN ('TENANT','ADMIN')
    AND tenant_id = current_setting('tch.tenant_id', true)::uuid
  );

CREATE POLICY archive_lookup_platform_read ON archive_lookup_index
  FOR SELECT
  USING (
    current_setting('tch.scope', true) = 'PLATFORM'
  );
```

### 8.5 Future WARM archive tables

If WARM archive tables are introduced in PostgreSQL later:
- Tenant-scoped tables must have `tenant_id` + RLS.
- Platform-only cross-tenant restore tables remain outside RLS.

---

## 9. Module boundaries

### 9.1 Owner module

Archive orchestration lives in `platform.archive`:

```
platform/archive/
  api/
    ArchiveApi.java
    ArchiveDatasetProvider.java
    model/
  internal/
    service/
    persistence/
    web/
    storage/
    batch/
    config/
```

Rules:

- `platform.archive` exposes only `api/`;
- `platform.archive.internal` must not import `core.*.internal`;
- `platform.archive.internal` must not import repositories owned by `core.sales`, `core.payout`, etc.;
- `platform.archive` owns run orchestration, archive registry, object storage, restore workflow
  and platform endpoints.

### 9.2 Dataset provider pattern

Owning modules provide dataset-specific export/lookup logic through an API interface.

```java
// platform.archive.api
public interface ArchiveDatasetProvider {
  ArchiveDatasetKey key();
  ArchiveDatasetPlan plan(ArchivePeriod period);
  ArchiveExportResult export(ArchiveExportRequest request);
  ArchiveLookupResult lookup(ArchiveLookupRequest request);
}
```

Implementations live in owning modules:

```
core.sales.infra.archive.SalesTicketArchiveDatasetProvider
core.payout.infra.archive.PayoutArchiveDatasetProvider
platform.audit.internal.archive.AuditLogArchiveDatasetProvider
platform.notification.internal.archive.NotificationArchiveDatasetProvider
```

Rules:

- provider implementations may use their own internal repositories/readers;
- `platform.archive` sees only the provider interface;
- `platform.archive` orchestrates through Spring injection of `List<ArchiveDatasetProvider>`;
- dataset providers must produce archive rows with explicit schema version;
- dataset providers are responsible for owner-specific lookup index records.

---

## 10. Archive registry

### 10.1 Tables

```sql
CREATE TABLE archive_run (
  id uuid PRIMARY KEY,
  status varchar(32) NOT NULL,
  strategy varchar(32) NOT NULL,
  trigger_type varchar(32) NOT NULL,
  idempotency_key varchar(160) NOT NULL,
  started_at timestamptz NOT NULL,
  completed_at timestamptz NULL,
  requested_by uuid NULL,        -- app_user.id, provider-neutral
  reason text NULL,
  error_message text NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_archive_run_idem UNIQUE (idempotency_key)
);

CREATE TABLE archive_object (
  id uuid PRIMARY KEY,
  archive_run_id uuid NOT NULL REFERENCES archive_run(id),
  table_name varchar(96) NOT NULL,
  tenant_id uuid NULL,
  period_start date NULL,
  period_end date NULL,
  lower_bound_at timestamptz NULL,
  upper_bound_at timestamptz NULL,
  segment_no int NOT NULL,
  object_uri text NOT NULL,
  format varchar(32) NOT NULL,
  compression varchar(32) NOT NULL,
  row_count bigint NOT NULL,
  byte_size bigint NOT NULL,
  checksum_sha256 varchar(128) NOT NULL,
  schema_version int NOT NULL,
  status varchar(32) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE archive_lookup_index (
  id uuid PRIMARY KEY,
  table_name varchar(96) NOT NULL,
  tenant_id uuid NULL,
  entity_type varchar(64) NULL,
  entity_id uuid NULL,
  public_code varchar(96) NULL,
  business_date date NULL,
  occurred_at timestamptz NULL,
  archive_object_id uuid NOT NULL REFERENCES archive_object(id),
  object_offset bigint NULL,
  object_length bigint NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
```

### 10.2 Status values

`archive_run.status`: `STARTED`, `COMPLETED`, `FAILED`

`archive_run.strategy`: `MONTHLY`, `FILL_MONITOR`, `MANUAL`

`archive_run.trigger_type`: `SCHEDULED`, `MANUAL`, `ROLLOVER_MONITOR`

`archive_object.status`: `PENDING`, `VERIFIED`, `INVALID`

### 10.3 Archive run idempotency

Business key: `{table}:{tenant_or_global}:{period_start}:{period_end}`

For segmented exports: `{table}:{tenant_or_global}:{period_start}:{period_end}:{segment_no}`

Rules:

- create `archive_run(status=STARTED)` with `idempotency_key` before export;
- second attempt with same key: `COMPLETED` → no-op; `STARTED` → resume or clean partial;
  `FAILED` → retry under same key;
- partition DDL/drop step checks `archive_object.status=VERIFIED`;
- scheduled runs do not require HTTP `Idempotency-Key`;
- manual platform endpoint may accept HTTP `Idempotency-Key`, but the canonical guard remains
  `archive_run.idempotency_key`.

---

## 11. Retrieval design

### 11.1 Online-first ticket lookup

1. Search hot `ticket` table by tenant + ticketId/publicCode.
2. If not found, search `archive_lookup_index` under current context/RLS.
3. Fetch archive object through backend archive storage adapter.
4. Extract matching row(s).
5. Assemble archived DTO.
6. Return response with `archived=true`.

```json
{
  "archived": true,
  "source": "ARCHIVE",
  "ticket": {},
  "archive": {
    "objectId": "...",
    "periodStart": "2026-01-01",
    "periodEnd": "2026-02-01"
  }
}
```

### 11.2 External routes

Tenant/admin scope:

```
GET /api/v1/admin/archive/tickets/{ticketId}
GET /api/v1/admin/archive/tickets/by-public-code/{publicCode}
GET /api/v1/admin/archive/payouts/{payoutId}
GET /api/v1/admin/archive/audit?entityType=ticket&entityId=...
```

Platform scope:

```
POST /api/v1/platform/archive/runs
GET  /api/v1/platform/archive/runs
GET  /api/v1/platform/archive/objects
POST /api/v1/platform/archive/restore-preview
POST /api/v1/platform/archive/restore
```

Controller mapping rule — do not include `/api/v1` in `@RequestMapping`:

```java
@RequestMapping("/admin/archive")
@RequestMapping("/platform/archive")
```

### 11.3 Archive read rules

- Archive reads must be explicit or fallback-controlled.
- Do not mix archived data into normal operational queries.
- Dashboards do not query archive by default.
- Reports may opt in to archive reads.
- Archived DTOs must clearly indicate `archived=true`.

---

## 12. Restore options

### 12.1 Mode A — Read-from-archive (default)

Fetch object → parse compressed file → locate row(s) → assemble DTO → return response.

Use for: ticket lookup by public code/id, audit lookup by entity, payout dispute lookup.

### 12.2 Mode B — Temporary restore tables

For larger investigations. Tables: `platform_archive_restore_ticket`,
`platform_archive_restore_ticket_line`, `platform_archive_restore_audit_log`.

Rules:

- platform/SUPER_ADMIN only; not reachable from tenant/admin endpoints;
- outside RLS by design; may contain cross-tenant investigation data;
- mandatory reason; functionally audited with `ARCHIVE_RESTORE`;
- TTL-bound cleanup; max rows per restore run; max active restore runs.

```yaml
tch:
  archive:
    restore:
      temp-ttl: P7D
      max-rows-per-run: 1000000
      max-active-restore-runs: 5
```

---

## 13. Retention policy

```yaml
tch:
  archive:
    enabled: true
    schedule:
      cron: "0 0 2 1 * *"
    hot-retention:
      ticket: P12M
      ticket-line: P12M
      payout: P24M
      audit-log: P12M
      notification-delivery-log: P6M
      provider-raw-payload: P6M
    rollover:
      max-rows-per-partition: 50000000
      max-bytes-per-partition: 100GB
      mode: MONITOR_ONLY
    storage:
      type: s3
      bucket: tchalanet-archive
      prefix: archive
      target-compressed-object-size: 512MB
    restore:
      temp-ttl: P7D
      max-rows-per-run: 1000000
      max-active-restore-runs: 5
```

Additional defaults:

- stats projections: HOT 24M or rebuildable
- `processed_event`: 6–12 M unless longer replay safety is needed
- `idempotency_record`: TTL-based cleanup

---

## 14. Dashboards, reports and cache

### 14.1 Dashboards

Dashboards must read projections, not raw high-volume transaction tables.

Sources: `sales_daily_stats`, `sales_session_stats`, `sales_draw_stats`, `sales_selection_stats`,
recent operational tables, short-TTL dashboard cache.

Dashboards must not query archive by default.

### 14.2 Historical reports

Read order: stats projections → hot transactional tables → archive only when explicit date range
requires it.

Archive is for lookup, dispute, compliance and rare historical extraction, not live dashboards.

### 14.3 Cache

```
features.dashboard.cashier.today
features.dashboard.admin.overview
features.report.sales.summary
```

Rules:

- declare all caches through `CacheSpecProvider`;
- use short TTL for dashboards;
- never use cache as source of truth;
- no cache for critical money operations (sell, payout confirm, settlement).

---

## 15. Event and projector policy

Projectors must be idempotent on: `tenant_id + handler_key + event_id`.

Events: `TicketSoldEvent`, `TicketVoidedEvent`, `TicketResultedEvent`, `PayoutRequestedEvent`,
`PayoutPaidEvent`, `DrawSettledEvent`.

Rules:

- listeners run after commit;
- projectors update stats in separate transactions;
- projector failure does not roll back the ticket sale;
- duplicate events are skipped silently;
- rebuild jobs can recompute projections for a period;
- handler keys are stable constants and never client-provided.

---

## 16. Archival workflow

```
1.  Determine eligible closed period.
2.  Acquire archive-run lock via scheduler/batch gate.
3.  Create archive_run(status=STARTED) with idempotency_key.
4.  Verify period is older than retention.
5.  Ask matching ArchiveDatasetProvider for export plan.
6.  Export rows by table and tenant/period in read transaction.
7.  Write archive object to storage.
8.  Compute checksum and row count.
9.  Insert archive_object rows with status=PENDING.
10. Insert archive_lookup_index rows for searchable entities.
11. Verify checksum + row count.
12. Mark archive_object.status=VERIFIED.
13. Separate idempotent DDL/cleanup step:
    detach/drop/truncate old partition ONLY if all matching archive_object rows are VERIFIED.
14. Mark archive_run.status=COMPLETED.
15. Write one functional audit action ARCHIVE_RUN with outcome in details.
```

Failure policy:

- export fails → keep hot data, `archive_run.status=FAILED`
- checksum fails → keep hot data, `archive_object.status=INVALID`
- DDL cleanup fails → archive object remains valid; cleanup retries later

**DDL is never part of the export transaction.**

---

## 17. Security

- Tenant-scoped archive lookups enforce tenant visibility through canonical context/RLS.
- Client-provided `tenant_id` is never trusted.
- Platform archive ops are `SUPER_ADMIN` only.
- Archive restore/read actions are functionally audited.
- Restore requires mandatory reason.
- Archive object URIs are never exposed directly to tenant/admin users.
- All archive DTOs use typed IDs outside persistence.
- `archive_restore_*` tables are platform-only and outside RLS.
- `archive_lookup_index` must not leak global or cross-tenant rows to tenant-scoped callers.

---

## 18. API contracts

### 18.1 Archived ticket response

```json
{
  "archived": true,
  "source": "ARCHIVE",
  "ticketId": "...",
  "publicCode": "...",
  "soldAt": "...",
  "businessDate": "...",
  "status": "...",
  "totalAmount": "...",
  "lines": [],
  "payout": null,
  "archiveMeta": {
    "objectId": "...",
    "periodStart": "2026-01-01",
    "periodEnd": "2026-02-01",
    "checksum": "..."
  }
}
```

### 18.2 Archive run request

```json
{
  "tables": ["ticket", "ticket_line", "audit_log"],
  "periodStart": "2026-01-01",
  "periodEnd": "2026-02-01",
  "dryRun": true,
  "reason": "Monthly retention archive"
}
```

Rules: 2xx → `ApiResponse<T>`; errors → `ProblemDetail`; list endpoints → standard pagination.

---

## 19. Non-goals V1

- Full data warehouse
- Real-time OLAP
- Automatic cross-storage SQL federation
- Kafka/outbox requirement
- Tenant-facing archive browser
- Global search over all archived JSON
- Parquet-only implementation
- Automatic mid-month partition rollover DDL

---

## 20. Resolved decisions

| # | Decision             | Resolution                                                                    |
|---|----------------------|-------------------------------------------------------------------------------|
| 1 | Partition key        | Physical by `occurred_at`/`sold_at`/`created_at`; `business_date` indexed    |
| 2 | Archive format       | `jsonl.gz` for V1; parquet later                                              |
| 3 | Archive lookup       | Maintain `archive_lookup_index`                                               |
| 4 | Archival trigger     | Monthly physical partitioning + fill-based monitor                            |
| 5 | Ticket Envers        | No Envers; immutable snapshots + functional audit                             |
| 6 | Restore table security | SUPER_ADMIN only, outside RLS, mandatory reason + audit                    |
| 7 | Archive files security | `tenant_id` retained in payload; access via service, not DB RLS             |
| 8 | Module boundary      | `platform.archive` orchestrates; owning modules implement `ArchiveDatasetProvider` |
| 9 | Actor identity       | `actor_id` is always internal `app_user.id`; provider-neutral (Firebase/KC)  |

---

## 21. Acceptance criteria

- Hot ticket sale path is not slowed by Envers on `ticket_line`.
- Dashboard endpoints read projection tables, not raw `ticket_line` aggregations.
- `audit_log` is append-only, partitioned and archivable.
- Archive run is idempotent through `archive_run.idempotency_key`.
- Archive run produces one terminal `ARCHIVE_RUN` audit row.
- Archived ticket is retrievable by ticket id or public code.
- Archived payout is retrievable by payout id.
- Archived audit entries are retrievable by entity type/id and date range.
- Old partitions are dropped only after checksum + row-count verification.
- DDL cleanup is separate from export transaction.
- No archive object URI is exposed directly to tenant/admin users.
- `archive_lookup_index` never leaks global/cross-tenant rows to tenant-scoped callers.
- `archive_restore_*` is reachable only through SUPER_ADMIN platform endpoints.
- Restore requires mandatory reason and functional audit.
- `platform.archive` does not import `core.*.internal` or core repositories.
- Dataset-specific archive logic lives in the owning module providers.
- All admin/platform archive operations require permission and reason.
- `audit_log.actor_id` is always internal `app_user.id` regardless of identity provider.

---

## 22. Definition of Done

- Build + tests OK.
- Flyway migrations OK.
- Partitioned tables created where applicable.
- Archive registry tables created.
- RLS policies created and tested for `archive_lookup_index`.
- No tenant leak in archive lookup tests.
- Scope/path correct: external `/api/v1/admin/**`, `/api/v1/platform/**`; controller mappings
  use logical paths.
- 2xx = `ApiResponse`.
- Errors = `ProblemDetail`.
- Standard pagination on `/runs` and `/objects`.
- After-commit for archive side-effects/events.
- No raw UUID outside persistence.
- `platform.archive` exposes only `api/`.
- No `platform.archive.internal` import from consumers.
- Functional audit on archive run/restore actions.
- Cache specs declared for dashboard/report caches.
- Docs updated: `data-lifecycle.md` and conventions if a new pattern emerges.
