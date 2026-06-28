# Tasks — archive-execution-v1

Status: PENDING

## Phase 0 — Spec alignment

- [x] Add concise `platform.archive` spec for execution, partitioning, retention and legal hold.
- [x] Validate OpenSpec change with `openspec validate archive-execution-v1 --strict`.

## Phase 1 — Execution foundation

- [x] Implement archive storage port and local dev/test adapter.
- [ ] Implement S3/MinIO-compatible storage adapter.
- [x] Implement `ArchiveRunExecutor` with run-level idempotency and checksum capture.
- [x] Harden `ArchiveRunExecutor` with explicit row-count/byte-size verification before marking objects `VERIFIED`.
- [x] Implement repositories for `archive_run`, `archive_object` and `archive_lookup_index`.
- [x] Write archive objects as streaming `jsonl.gz`.

## Phase 2 — Partition and cleanup safety

- [x] Partition `audit_log` by month using `occurred_at`.
- [x] Add Flyway partition maintenance helpers for monthly partition creation and future `audit_log` partitions.
- [ ] Partition ticket/draw/result high-volume hot tables by month using the operational timestamp.
- [ ] Keep business date indexed for reporting and lookup when ticket/draw/result partition migrations are added.
- [x] Implement cleanup planner as dry-run first.
- [x] Add stricter cleanup execution safety: re-check eligibility inside execute, validate partition names, and block unplanned DDL.
- [x] Detach/drop/truncate partitions only after verified archive objects exist and legal hold checks pass.

## Phase 3 — Legal access and disputes

- [x] Add legal-hold records keyed by tenant, dataset, entity id and/or period.
- [x] Block archive purge and partition cleanup when matching legal hold exists.
- [x] Add focused cleanup tests proving legal hold blocks partition cleanup.
- [ ] Require reason and functional audit for archive read, restore and legal-hold changes.
- [ ] Keep restore tables SUPER_ADMIN-only with TTL cleanup.

## Phase 4 — First datasets

- [x] Add `AuditLogArchiveDatasetProvider`.
- [x] Add focused `ArchiveRunExecutor` tests for verified object and invalid object paths.
- [ ] Prove end-to-end flow with `audit_log` in integration tests.
- [x] Add `SalesTicketArchiveDatasetProvider` for ticket headers.
- [x] Add `SalesTicketLineArchiveDatasetProvider` for ticket lines by parent ticket sold period.
- [x] Archive ticket charges/snapshots, not only ticket headers and lines.
- [x] Prove `sales_ticket_charge` archive flow with dev seed data and a local verified archive run.
- [ ] Implement archived ticket DTO and lookup by ticket id/public code before deleting any hot ticket
  partitions.
- [x] Implement `draw`/`draw_result` archive provider after ticket lookup is proven.
- [x] Implement Spring Batch archive provider using job execution as the bounded export aggregate.
- [x] Implement Envers revision archive provider using `revinfo` as the bounded export aggregate.
- [ ] Add purge policies for Spring Batch and Envers only after verified archive objects and legal-hold
  checks are proven.
- [x] Implement guarded ticket hot-table purge endpoint with dry-run, archive verification, legal-hold
  checks, bounded deletes, and child-before-parent order.
- [x] Implement guarded draw, draw_result and entity_revision purge endpoint with dry-run,
  archive verification, legal-hold checks and dependency blockers.
- [x] Document Spring Batch emergency purge order and ticket-line emergency cleanup guardrails.
- [ ] Add integration tests for batch/audit provider rules with bounded lookup indexes.

## Phase 5 — Web and ops readiness

- [x] Add platform archive page and API service for recent runs, failed runs, invalid objects and ops summary.
- [x] Add an explicit empty-state hint for dev: no run exists until a manual archive run is triggered.
- [x] Make platform archive sidenav routes explicit: overview/runs/issues/legal-holds/partitions load their intended data.
- [x] Expose archive purges in the archive UI with dry-run-first controls.
- [x] Extract archive page UI into route-owned standalone components under `pages/archive/components`.
- [x] Add focused frontend tests for the archive page data/loading/error states.
- [ ] Add backend integration tests for trigger run, list runs, lookup index isolation and cleanup planning.

## Phase 6 — Operator documentation

- [x] Add `docs/ARCHIVE_RUNBOOK.md` for archive execution, partitioning, restore, purge, legal holds and incident response.
