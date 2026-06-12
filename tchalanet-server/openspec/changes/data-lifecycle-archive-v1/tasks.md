# Tasks — data-lifecycle-archive-v1

Status: PENDING (spec under review)

---

## Phase 1 — Inventory and policy

- [ ] Create `docs/architecture/data-lifecycle.md` with table classification matrix
- [ ] Document retention defaults (ticket P12M, payout P24M, audit P12M, etc.)
- [ ] Document Envers allowlist / blocklist in ARCHITECTURE.md or conventions

## Phase 2 — Audit log hardening

- [ ] Flyway migration: partition `audit_log` by `occurred_at` (monthly)
- [ ] Flyway migration: add `business_date`, `outlet_id`, `terminal_id`, `sales_session_id`,
      `correlation_id`, `request_id` columns where missing
- [ ] Create minimal indexes (tenant_time, tenant_entity, actor_time, action_time)
- [ ] Canonicalize `AuditAction` enum with the full taxonomy from §4.1
- [ ] Remove Envers from `audit_log` if present

## Phase 3 — Stats projections

- [ ] Flyway migration: `sales_daily_stats`, `sales_session_stats`, `sales_draw_stats`,
      `sales_selection_stats`
- [ ] Event projectors: `TicketSoldEvent`, `TicketVoidedEvent`, `TicketResultedEvent`,
      `PayoutRequestedEvent`, `PayoutPaidEvent`, `DrawSettledEvent`
- [ ] `processed_event` idempotency table + handler key guard
- [ ] Dashboard service readers (projections only, not raw ticket/payout)
- [ ] `CacheSpecProvider` entries for dashboard/report caches

## Phase 4 — Archive registry

- [ ] New Maven module or package `platform.archive`
- [ ] `ArchiveDatasetProvider` interface in `platform.archive.api`
- [ ] Flyway migration: `archive_run`, `archive_object`, `archive_lookup_index`
- [ ] RLS policies on `archive_lookup_index`
- [ ] Archive run idempotency guard
- [ ] Dry-run command
- [ ] Platform ops endpoints (`/platform/archive/runs`, `/platform/archive/objects`)
- [ ] Functional audit: `ARCHIVE_RUN`
- [ ] ArchUnit test: `platform.archive.internal` does not import `core.*.internal`

## Phase 5 — First archive targets (providers)

- [ ] `AuditLogArchiveDatasetProvider` in `platform.audit`
- [ ] `NotificationArchiveDatasetProvider` in `platform.notification`
- [ ] `ProviderRawPayloadArchiveDatasetProvider` (owner TBD)
- [ ] `SalesTicketArchiveDatasetProvider` in `core.sales`
- [ ] `PayoutArchiveDatasetProvider` in `core.payout`
- [ ] Settlement detail provider

## Phase 6 — Archive retrieval

- [ ] Admin archived ticket lookup (`/admin/archive/tickets/{ticketId}`)
- [ ] Admin archived payout lookup (`/admin/archive/payouts/{payoutId}`)
- [ ] Admin archived audit lookup (`/admin/archive/audit`)
- [ ] Read-from-archive service (fetch → decompress → locate → assemble DTO)
- [ ] Temporary restore tables (`platform_archive_restore_*`) + TTL cleanup job
- [ ] Restore endpoint (`/platform/archive/restore`) with mandatory reason + audit

---

## OpenSpec recommendation

**OpenSpec change required.** This introduces a new `platform.archive` module, new DB tables,
new RLS policies, Flyway migrations, and new API endpoints. An OpenSpec change is mandatory per
project conventions.

Change id: `data-lifecycle-archive-v1`
