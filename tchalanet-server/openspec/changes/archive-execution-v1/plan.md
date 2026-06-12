# Follow-up Plan — archive-execution-v1

> **Status**: PENDING — follow-up after `data-lifecycle-archive-v1` scaffold  
> **Scope**: `tchalanet-server` — `platform.archive`, archive providers, object storage, restore, lookup, cleanup, observability  
> **Goal**: turn the archive scaffold into a real, testable archive execution path.

---

## 1. Current state

The previous `data-lifecycle-archive-v1` work delivered the architectural scaffold:

| Commit | Phase | What shipped |
|---|---|---|
| `404403a` | Phase 1 | `docs/architecture/data-lifecycle.md` — CLASS A/B/C/D matrix, retention, Envers allowlist |
| `a8c5165b` | Phase 2 | V226 partitioned `audit_log`, canonicalized `AuditAction` enum |
| `7cec126c` | Phase 3 | V227 `analytics_session` + `analytics_selection`, projectors, cache specs |
| `2a8fe18f` | Phase 4 | V228 archive registry tables, `ArchiveDatasetProvider`, ArchUnit gates |
| `31a8e39a` | Phase 5 | Stub providers in owning modules: `platform.audit`, `platform.notification`, `core.sales`, `core.payout` |
| `eb1dc535` | Phase 6 | V229 restore tables, `ArchiveApi`, `AdminArchiveController`, `PlatformArchiveController`, `ArchiveService` stub |

Current status:

```text
Archive architecture scaffold: DONE
Production-ready archive execution: NOT DONE
```

This follow-up must deliver:

- real object storage adapter;
- real `plan()` and `export()` flow for at least `audit_log`;
- archive run execution;
- checksum and row-count verification;
- archive lookup index population;
- read-from-archive lookup;
- recovery/idempotency;
- restore cleanup;
- functional audit for archive restore;
- integration tests.

---

## 2. Architectural constraints

### 2.1 Module boundaries

`platform.archive` orchestrates the archive system, but it must not become the owner of core persistence.

Rules:

- `platform.archive.internal` must not import `core.*.internal`.
- `platform.archive.internal` must not import repositories from `core.sales`, `core.payout`, or any other owning domain.
- Dataset-specific archive logic lives in the owning module through `ArchiveDatasetProvider` implementations.
- `platform.archive` sees only `platform.archive.api.ArchiveDatasetProvider`.

Provider examples:

```text
platform.audit.internal.archive.AuditLogArchiveDatasetProvider
platform.notification.internal.archive.NotificationArchiveDatasetProvider
core.sales.infra.archive.SalesTicketArchiveDatasetProvider
core.payout.infra.archive.PayoutArchiveDatasetProvider
```

### 2.2 API route convention

External routes include `/api/v1`:

```text
/api/v1/admin/archive/**
/api/v1/platform/archive/**
```

Java controller mappings must use logical paths only:

```java
@RequestMapping("/admin/archive")
@RequestMapping("/platform/archive")
```

Do not put `/api/v1` in `@RequestMapping`.

### 2.3 Archive access model

Archive files are not PostgreSQL tables and do not use DB RLS directly.

Rules:

| Storage layer | Tenant marker | RLS | Access model |
|---|---:|---:|---|
| Hot PostgreSQL tables | `tenant_id` | yes | normal tenant/admin/platform context |
| `archive_lookup_index` | `tenant_id` / `NULL` | yes, scope-aware | tenant/admin sees own rows only; platform sees all |
| Archive files | `tenant_id` in payload | no DB RLS | accessed only through backend archive service |
| `platform_archive_restore_*` tables | may contain multiple tenants | no | SUPER_ADMIN only, reason + audit |

---

# Phase 7A — Archive execution foundation

## Objective

Implement the common execution engine inside `platform.archive`, without making `platform.archive` depend on `core.*.internal`.

## Tasks

- [ ] Create `ArchiveStoragePort` in `platform.archive.internal.storage`:
  - [ ] `putObject(...)`
  - [ ] `getObject(...)`
  - [ ] `deleteObject(...)`
  - [ ] `exists(...)`
  - [ ] `openStream(...)`

- [ ] Create storage implementations:
  - [ ] `LocalFileArchiveStorageAdapter` for dev/test
  - [ ] `S3ArchiveStorageAdapter` or MinIO-compatible adapter behind config
  - [ ] config switch: `tch.archive.storage.type=local|s3`

- [ ] Add archive storage config:
  - [ ] bucket/root path
  - [ ] prefix
  - [ ] target compressed object size
  - [ ] timeout
  - [ ] retry count
  - [ ] max object size guard

- [ ] Create `ArchiveRunExecutor`:
  - [ ] receives archive request
  - [ ] resolves matching `ArchiveDatasetProvider`
  - [ ] creates `archive_run`
  - [ ] calls provider `plan()`
  - [ ] calls provider `export()`
  - [ ] stores objects
  - [ ] writes `archive_object`
  - [ ] writes `archive_lookup_index`
  - [ ] verifies checksum and row count
  - [ ] marks run `COMPLETED` or `FAILED`

- [ ] Implement `ArchiveRunRepository` / JDBC adapter:
  - [ ] create run with idempotency key
  - [ ] find by idempotency key
  - [ ] update status
  - [ ] append error message
  - [ ] list runs paginated

- [ ] Implement `ArchiveObjectRepository`:
  - [ ] save object metadata
  - [ ] mark `PENDING`
  - [ ] mark `VERIFIED`
  - [ ] mark `INVALID`
  - [ ] list objects paginated

- [ ] Implement `ArchiveLookupIndexRepository`:
  - [ ] save lookup rows
  - [ ] query by tenant + table/entity/id/public code
  - [ ] rely on RLS for tenant visibility
  - [ ] never trust tenant id from client payload

- [ ] Implement checksum support:
  - [ ] SHA-256 on compressed archive object
  - [ ] row-count verification
  - [ ] byte-size verification

- [ ] Implement `jsonl.gz` writer:
  - [ ] streaming, not loading all rows in memory
  - [ ] one JSON object per row
  - [ ] schema version included
  - [ ] stable enough field ordering for debugging

- [ ] Implement `jsonl.gz` reader:
  - [ ] streaming read
  - [ ] filter by lookup criteria
  - [ ] max scan guard
  - [ ] timeout guard

## Acceptance criteria

- [ ] `ArchiveRunExecutor` can execute a dry run and a real run.
- [ ] Local storage works in integration tests.
- [ ] Storage abstraction does not leak into providers.
- [ ] `platform.archive` still does not import `core.*.internal` or core repositories.
- [ ] Failed run does not delete hot data.
- [ ] Checksum and row count are stored and verified.

---

# Phase 7B — Archive run idempotency and recovery

## Objective

Make archive execution safe to retry after crashes, duplicate calls, or partial failure.

## Tasks

- [ ] Implement idempotency key builder:

```text
{dataset}:{tenant_or_global}:{period_start}:{period_end}
```

For segmented exports:

```text
{dataset}:{tenant_or_global}:{period_start}:{period_end}:{segment_no}
```

- [ ] Add `ArchiveRunGuard`:
  - [ ] `beginOrResume(idempotencyKey)`
  - [ ] `complete(runId)`
  - [ ] `fail(runId, error)`
  - [ ] `canRetry(runId)`

- [ ] Implement recovery rules:
  - [ ] `COMPLETED` -> no-op
  - [ ] `STARTED` + valid verified objects -> resume completion
  - [ ] `STARTED` + partial invalid objects -> cleanup or mark failed
  - [ ] `FAILED` -> retry under same idempotency key allowed
  - [ ] duplicate manual call -> returns existing run status

- [ ] Add partial object cleanup strategy:
  - [ ] delete unverified object if safe
  - [ ] mark invalid object if delete fails
  - [ ] never delete verified object automatically

- [ ] Add tests:
  - [ ] duplicate run
  - [ ] failed run retry
  - [ ] crash after object write before DB metadata
  - [ ] crash after metadata before verification
  - [ ] crash after verification before completion

## Acceptance criteria

- [ ] Running the same archive request twice does not duplicate objects or lookup rows.
- [ ] Failed archive can be retried.
- [ ] Completed archive is an idempotent no-op.
- [ ] Partial object states are recoverable.

---

# Phase 7C — Real `audit_log` archive provider E2E

## Objective

Use `audit_log` as the first real archive target.

This is safer than starting with tickets because it tests the archive pipeline without requiring multi-table ticket reconstruction.

## Tasks

- [ ] Implement `AuditLogArchiveDatasetProvider` in `platform.audit`:
  - [ ] real `plan()`
  - [ ] real `export()`
  - [ ] real lookup row generation

- [ ] `plan()` must return:
  - [ ] eligible period
  - [ ] estimated row count
  - [ ] tenant/global scope
  - [ ] expected partitions
  - [ ] dry-run summary
  - [ ] warnings if period is not closed or not older than retention

- [ ] `export()` must stream `audit_log` rows for the period and include:
  - [ ] `tenant_id`
  - [ ] `occurred_at`
  - [ ] `business_date`
  - [ ] `action`
  - [ ] `entity_type`
  - [ ] `entity_id`
  - [ ] `actor_id`
  - [ ] `request_id`
  - [ ] `correlation_id`
  - [ ] compact `details`
  - [ ] schema version

- [ ] Populate `archive_lookup_index` for searchable audit entries:
  - [ ] `table_name = audit_log`
  - [ ] `tenant_id`
  - [ ] `entity_type`
  - [ ] `entity_id`
  - [ ] `business_date`
  - [ ] `occurred_at`
  - [ ] `archive_object_id`

- [ ] Implement admin archived audit lookup:

Controller mapping:

```text
/admin/archive/audit
```

External route:

```text
/api/v1/admin/archive/audit
```

Filters:

```text
entityType
entityId
from
to
action
page
size
```

- [ ] Implement read-from-archive for audit:
  - [ ] lookup index first
  - [ ] fetch object
  - [ ] stream/decompress
  - [ ] filter matching rows
  - [ ] return archived audit DTOs

- [ ] Add tests:
  - [ ] archive old audit rows
  - [ ] query archived audit by entity
  - [ ] tenant cannot read another tenant's archived audit
  - [ ] tenant cannot see global audit rows
  - [ ] platform can read global rows
  - [ ] object URI is never exposed

## Acceptance criteria

- [ ] `audit_log` can be archived end-to-end.
- [ ] Archived audit entries are retrievable by entity type/id.
- [ ] RLS on `archive_lookup_index` is verified.
- [ ] No object URI leaks to tenant/admin responses.

---

# Phase 7D — Archive API hardening

## Objective

Make the archive API safe, consistent and aligned with Tchalanet Web API conventions.

## Tasks

- [ ] Ensure controller mappings are logical paths only:
  - [ ] `/admin/archive`
  - [ ] `/platform/archive`
  - [ ] not `/api/v1/...`

- [ ] External documentation may show:
  - [ ] `/api/v1/admin/archive/**`
  - [ ] `/api/v1/platform/archive/**`

- [ ] Add `@PreAuthorize` on controllers:
  - [ ] admin archive lookup: tenant admin / superadmin + permission
  - [ ] platform archive operations: SUPER_ADMIN only

- [ ] Add permission keys:
  - [ ] `ARCHIVE_READ`
  - [ ] `ARCHIVE_RUN`
  - [ ] `ARCHIVE_RESTORE`
  - [ ] `ARCHIVE_OBJECT_LIST`

- [ ] Add mandatory reason validation for:
  - [ ] manual archive run
  - [ ] restore preview
  - [ ] restore execution

- [ ] List endpoints must use standard pagination:
  - [ ] `GET /platform/archive/runs`
  - [ ] `GET /platform/archive/objects`

- [ ] 2xx responses use `ApiResponse<T>`.
- [ ] Errors use `ProblemDetail`.
- [ ] No raw UUID outside persistence.
- [ ] Use typed IDs:
  - [ ] `ArchiveRunId`
  - [ ] `ArchiveObjectId`
  - [ ] `TicketId`
  - [ ] `PayoutId`
  - [ ] `TenantId`

## Acceptance criteria

- [ ] SecurityArchTest passes.
- [ ] All protected endpoints have method/class security.
- [ ] Pagination convention is respected.
- [ ] No `/api/v1` appears in controller `@RequestMapping`.

---

# Phase 7E — Restore cleanup and restore audit

## Objective

Make temporary restore safe and bounded.

## Tasks

- [ ] Implement `ArchiveRestoreService`:
  - [ ] restore preview
  - [ ] restore execution
  - [ ] restore metadata
  - [ ] max rows per run
  - [ ] max active restore runs

- [ ] Implement restore TTL cleanup scheduler:
  - [ ] deletes expired rows from `platform_archive_restore_ticket`
  - [ ] deletes expired rows from `platform_archive_restore_ticket_line`
  - [ ] deletes expired rows from `platform_archive_restore_audit_log`
  - [ ] records cleanup summary
  - [ ] does not run without platform context

- [ ] Add config:

```yaml
tch:
  archive:
    restore:
      temp-ttl: P7D
      max-rows-per-run: 1000000
      max-active-restore-runs: 5
```

- [ ] Emit functional audit `ARCHIVE_RESTORE`, including:
  - [ ] reason
  - [ ] actor
  - [ ] restored table(s)
  - [ ] row count
  - [ ] period
  - [ ] archive object IDs
  - [ ] outcome

- [ ] Add tests:
  - [ ] restore requires SUPER_ADMIN
  - [ ] restore requires reason
  - [ ] tenant/admin cannot access restore endpoint
  - [ ] cleanup removes expired restore rows
  - [ ] cleanup keeps non-expired rows
  - [ ] audit emitted on restore

## Acceptance criteria

- [ ] Restore tables cannot grow forever.
- [ ] Restore is platform-only.
- [ ] Restore is always audited.

---

# Phase 7F — Partition cleanup / DDL management

## Objective

Prepare safe partition cleanup without risking hot data loss.

## Important V1 decision

DDL cleanup must be separate from export.

Export success does not automatically mean partition drop inside the same transaction.

## Tasks

- [ ] Implement `ArchivePartitionPlanner`:
  - [ ] identify archive-eligible partitions
  - [ ] map partition -> archive objects
  - [ ] verify all required archive objects are `VERIFIED`

- [ ] Implement `ArchivePartitionCleanupService`:
  - [ ] dry-run
  - [ ] detach partition
  - [ ] drop/truncate partition
  - [ ] mark cleanup outcome
  - [ ] never cleanup if archive object is missing or not verified

- [ ] Add cleanup modes:
  - [ ] `DRY_RUN`
  - [ ] `DETACH_ONLY`
  - [ ] `DROP`
  - [ ] `TRUNCATE`

- [ ] Add config:

```yaml
tch:
  archive:
    cleanup:
      enabled: false
      mode: DETACH_ONLY
```

- [ ] Add safety checks:
  - [ ] period is older than retention
  - [ ] partition name matches expected pattern
  - [ ] object row count equals exported row count
  - [ ] archive run completed
  - [ ] no invalid archive object exists for that period

- [ ] Add tests:
  - [ ] cleanup refused if object not verified
  - [ ] cleanup refused if period too recent
  - [ ] cleanup dry-run produces plan
  - [ ] cleanup idempotent if partition already detached/dropped

## Acceptance criteria

- [ ] No partition DDL runs unless archive verification passed.
- [ ] Cleanup can be tested in dry-run.
- [ ] Cleanup is disabled by default until explicitly enabled.

---

# Phase 7G — Ticket archive design before implementation

## Objective

Design ticket archive retrieval before implementing ticket export, because ticket archive is multi-table.

## Ticket archive complexity

A ticket archive lookup may need:

```text
ticket
ticket_line
ticket_charge
promotion snapshot
pricing snapshot
payout summary
settlement/result status
```

## Tasks

- [ ] Define archived ticket DTO.
- [ ] Define archive object grouping strategy:
  - [ ] Option A: one object per table
  - [ ] Option B: one denormalized ticket document object
  - [ ] Option C: hybrid — table exports + lookup assembler

Recommended V1:

```text
Export normalized table rows, but read-from-archive assembles an ArchivedTicketView.
Keep lookup index on ticketId and publicCode.
```

- [ ] Define lookup index entries:
  - [ ] by `ticket_id`
  - [ ] by `public_code`
  - [ ] by `business_date`
  - [ ] by `sold_at`

- [ ] Define ticket archive object boundaries:
  - [ ] target compressed object size 256MB-512MB
  - [ ] avoid one huge monthly tenant object if too large

- [ ] Define how ticket lines are located:
  - [ ] same archive object group id
  - [ ] or lookup by ticket id
  - [ ] or segment manifest

- [ ] Add manifest metadata if needed:
  - [ ] dataset group
  - [ ] related objects
  - [ ] table role: header/line/charge/snapshot

## Acceptance criteria

- [ ] Ticket archive implementation does not start until DTO and object grouping strategy are documented.
- [ ] Retrieval path can assemble a full ticket without scanning all monthly ticket_line files.

---

# Phase 8 — Ticket archive E2E

## Objective

Implement ticket archive only after audit_log archive E2E is proven.

## Tasks

- [ ] Implement real `SalesTicketArchiveDatasetProvider`.
- [ ] Implement `plan()` counts:
  - [ ] tickets
  - [ ] lines
  - [ ] charges
  - [ ] snapshots

- [ ] Implement `export()` streaming:
  - [ ] ticket rows
  - [ ] ticket_line rows
  - [ ] charge rows
  - [ ] snapshots

- [ ] Populate lookup index:
  - [ ] ticket id
  - [ ] public code
  - [ ] business date

- [ ] Implement archived ticket lookup:
  - [ ] hot table first
  - [ ] archive lookup second
  - [ ] read archive objects
  - [ ] assemble `ArchivedTicketView`

- [ ] Add integration test:
  - [ ] sell old ticket
  - [ ] archive period
  - [ ] simulate hot absence
  - [ ] retrieve ticket by public code
  - [ ] verify lines and totals

- [ ] Add tenant isolation test:
  - [ ] Tenant A cannot retrieve Tenant B archived ticket

- [ ] Add object URI leak test.
- [ ] Add performance guard:
  - [ ] lookup should not scan unrelated archive objects

## Acceptance criteria

- [ ] Archived ticket retrievable by ticket id and public code.
- [ ] Ticket lines and totals match original snapshot.
- [ ] Tenant isolation verified.

---

# Phase 9 — Payout archive E2E

## Objective

Implement payout archive after ticket archive because payout often references tickets, settlement and workflow.

## Tasks

- [ ] Implement real `PayoutArchiveDatasetProvider`.
- [ ] Implement `plan()` counts.
- [ ] Implement `export()` streaming.
- [ ] Populate lookup index:
  - [ ] payout id
  - [ ] ticket id if applicable
  - [ ] business date
  - [ ] requested/paid date

- [ ] Implement archived payout lookup.
- [ ] Include workflow history if required.
- [ ] Add integration tests:
  - [ ] archive old payout
  - [ ] retrieve by payout id
  - [ ] tenant isolation
  - [ ] platform lookup

## Acceptance criteria

- [ ] Archived payout retrievable by payout id.
- [ ] Workflow status/history available if required by DTO.

---

# Phase 10 — Observability and operations

## Objective

Make archive execution observable and operable.

## Tasks

- [ ] Add structured logs:
  - [ ] archive run started
  - [ ] provider selected
  - [ ] rows exported
  - [ ] object written
  - [ ] checksum verified
  - [ ] lookup rows written
  - [ ] cleanup completed/failed

- [ ] Add metrics:
  - [ ] archive runs total
  - [ ] archive failures total
  - [ ] rows archived by dataset
  - [ ] bytes archived by dataset
  - [ ] archive duration
  - [ ] restore duration
  - [ ] lookup fallback count
  - [ ] archive object read errors

- [ ] Add platform ops view data:
  - [ ] latest archive runs
  - [ ] failed runs
  - [ ] invalid objects
  - [ ] restore table usage
  - [ ] cleanup backlog

- [ ] Add alert conditions:
  - [ ] archive run failed
  - [ ] invalid archive object
  - [ ] restore rows exceed threshold
  - [ ] partition exceeds rollover threshold
  - [ ] cleanup overdue

## Acceptance criteria

- [ ] Ops can know if archive is healthy.
- [ ] Failed archive runs are visible.
- [ ] Rollover monitor produces actionable signal.

---

# Recommended implementation order

```text
1. Phase 7A — execution foundation
2. Phase 7B — idempotency/recovery
3. Phase 7C — audit_log archive E2E
4. Phase 7D — API hardening
5. Phase 7E — restore cleanup/audit
6. Phase 7F — partition cleanup dry-run only
7. Phase 7G — ticket archive design
8. Phase 8 — ticket archive E2E
9. Phase 9 — payout archive E2E
10. Phase 10 — observability/ops
```

---

# Do not do immediately

Do not start with:

```text
ticket archive
payout archive
automatic partition DROP
massive cross-tenant restore
parquet
data warehouse
read replica
```

Start with `audit_log` archive E2E.

---

# Status targets

After Phase 7C:

```text
Archive execution MVP is operational for audit_log.
Ticket/payout archive remain follow-up datasets.
```

After Phase 8:

```text
Archive execution V1 is usable for operational lookup and dispute handling.
```

After Phase 10:

```text
Archive system is production-ready.
```

---

# Final Definition of Done

- [ ] Build + tests OK.
- [ ] Flyway migrations OK.
- [ ] No tenant leak in archive lookup tests.
- [ ] `platform.archive.internal` does not import `core.*.internal`.
- [ ] `platform.archive.internal` does not import core repositories.
- [ ] Archive run idempotency and recovery tested.
- [ ] `audit_log` archive E2E working.
- [ ] Read-from-archive working for archived audit rows.
- [ ] Restore tables have TTL cleanup.
- [ ] Restore requires SUPER_ADMIN + reason.
- [ ] `ARCHIVE_RESTORE` audit emitted.
- [ ] Partition cleanup is dry-run first and disabled by default.
- [ ] Ticket archive design documented before implementation.
- [ ] Metrics/logging added for archive runs.
