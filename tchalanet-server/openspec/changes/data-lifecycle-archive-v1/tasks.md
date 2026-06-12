# Tasks — data-lifecycle-archive-v1

Status: PENDING (spec under review)

---

## Phase 1 — Inventory and policy

- [ ] Create `docs/architecture/data-lifecycle.md`
- [ ] Document data classes A/B/C/D
- [ ] Document retention defaults:
  - ticket: P12M
  - ticket-line: P12M
  - payout: P24M
  - audit-log: P12M
  - notification-delivery-log: P6M
  - provider-raw-payload: P6M
- [ ] Document Envers allowlist / blocklist
- [ ] Document archive tenant/RLS model:
  - hot tables: `tenant_id` + RLS
  - archive lookup index: `tenant_id` + scope-aware RLS
  - archive files: `tenant_id` in payload, no DB RLS
  - restore tables: SUPER_ADMIN only, outside RLS

---

## Phase 2 — Audit log hardening

- [ ] Flyway migration: partition `audit_log` by `occurred_at`
- [ ] Flyway migration: add archive-ready columns where missing:
  - `business_date`
  - `outlet_id`
  - `terminal_id`
  - `sales_session_id`
  - `correlation_id`
  - `request_id`
- [ ] Create minimal indexes:
  - tenant/time
  - tenant/entity
  - tenant/actor
  - tenant/action
  - business_date
- [ ] Canonicalize `AuditAction` enum
- [ ] Remove Envers from `audit_log` if present
- [ ] Ensure one business action = one audit row
- [ ] Ensure no audit is emitted for dashboard reads, cache misses or projector internals

---

## Phase 3 — Stats projections

- [ ] Flyway migration:
  - `sales_daily_stats`
  - `sales_session_stats`
  - `sales_draw_stats`
  - `sales_selection_stats`
- [ ] Event projectors:
  - `TicketSoldEvent`
  - `TicketVoidedEvent`
  - `TicketResultedEvent`
  - `PayoutRequestedEvent`
  - `PayoutPaidEvent`
  - `DrawSettledEvent`
- [ ] `processed_event` idempotency table + handler_key convention
- [ ] Dashboard service readers use projections only
- [ ] `CacheSpecProvider` entries for dashboard/report caches
- [ ] Verify dashboard endpoints do not aggregate directly from `ticket_line`

---

## Phase 4 — Archive registry

- [ ] Create package/capability `platform.archive` inside existing `tchalanet-platform`
- [ ] Create `ArchiveDatasetProvider` interface in `platform.archive.api`
- [ ] Ensure `platform.archive.internal` does not import `core.*.internal` or core repositories
- [ ] Flyway migration:
  - `archive_run`
  - `archive_object`
  - `archive_lookup_index`
- [ ] Add optional future-seek columns:
  - `archive_lookup_index.object_offset`
  - `archive_lookup_index.object_length`
- [ ] RLS policies on `archive_lookup_index`
- [ ] Archive run idempotency guard via `archive_run.idempotency_key`
- [ ] Dry-run command
- [ ] Platform ops endpoints:
  - controller mapping: `/platform/archive/**`
  - external route: `/api/v1/platform/archive/**`
- [ ] Functional audit: `ARCHIVE_RUN`
- [ ] ArchUnit test:
  - `platform.archive.internal` must not be imported by consumers
  - `platform.archive` must not depend on `core.*.internal`

---

## Phase 5 — First archive targets providers

- [ ] `AuditLogArchiveDatasetProvider` in `platform.audit`
- [ ] `NotificationArchiveDatasetProvider` in `platform.notification`
- [ ] `ProviderRawPayloadArchiveDatasetProvider` in owning provider/result module
- [ ] `SalesTicketArchiveDatasetProvider` in `core.sales`
- [ ] `PayoutArchiveDatasetProvider` in `core.payout`
- [ ] Settlement detail provider in owning settlement/sales module
- [ ] Verify each provider writes rows with:
  - `tenant_id`
  - schema version
  - period bounds
  - lookup index records where needed

---

## Phase 6 — Archive retrieval

- [ ] Admin archived ticket lookup:
  - controller mapping: `/admin/archive/tickets/**`
  - external route: `/api/v1/admin/archive/tickets/**`
- [ ] Admin archived payout lookup:
  - controller mapping: `/admin/archive/payouts/**`
  - external route: `/api/v1/admin/archive/payouts/**`
- [ ] Admin archived audit lookup:
  - controller mapping: `/admin/archive/audit`
  - external route: `/api/v1/admin/archive/audit`
- [ ] Read-from-archive service:
  - fetch object
  - decompress
  - locate rows
  - assemble archived DTO
- [ ] Temporary restore tables:
  - `platform_archive_restore_ticket`
  - `platform_archive_restore_ticket_line`
  - `platform_archive_restore_audit_log`
- [ ] Restore endpoint:
  - controller mapping: `/platform/archive/restore`
  - external route: `/api/v1/platform/archive/restore`
- [ ] Restore cleanup job:
  - TTL cleanup
  - max rows per restore run
  - max active restore runs
- [ ] Functional audit: `ARCHIVE_RESTORE`

---

## OpenSpec recommendation

**OpenSpec change required.**

This introduces:

- new `platform.archive` capability;
- new RLS policies;
- Flyway migrations;
- archive registry tables;
- archive lookup and restore APIs;
- data lifecycle policy;
- audit/Envers policy updates;
- dashboard projection expectations.

Do not implement directly without an OpenSpec change or equivalent architecture change document.
