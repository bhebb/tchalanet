# Tasks — Peaufiner Sales / Print / Core Reconciliation

## 1. Sales public API hardening

- [x] Replace internal `Ticket` aggregate in `SellTicketResult` with public result DTO fields.
- [x] Update controllers/mappers/tests consuming `SellTicketResult`.
- [x] Add ArchUnit or package test ensuring `core.sales.api` does not import `core.sales.internal`.

## 2. Sales promotion materialization

- [x] Validate `BOOST_ODDS` effect amount is non-null and positive.
- [x] Normalize boosted odds to scale 4.
- [x] Document BOOST_ODDS V1 target as gameCode-only.
- [x] Replace hardcoded free-line fallback selection `"1"`.
- [x] Add explicit free-line selection source enum handling.
- [x] Extend waived charges with promotion decision id, rule id, effect type, and label.
- [x] Ensure ticket money excludes waived buyer-facing charges but retains original charge for audit/print.
- [ ] Ensure preview and final promotion evaluation phases are distinct if preview is supported.
- [x] Add unit tests for FREE_GAME_LINE, BOOST_ODDS, WAIVE_CHARGE.

## 3. Sales reconciliation query support

- [x] Add `ListExpectedTicketOutcomesForDrawResultQuery` in `core.sales.api.query`.
- [x] Add `ExpectedTicketOutcomeRow` including `ticketCode`, `publicCode`, `displayCode`.
- [x] Implement query using sales snapshots and official draw result.
- [x] Add `ListActualTicketStatesForDrawQuery`.
- [x] Add `GetSalesOutcomeSummaryForDrawQuery`.
- [ ] Add tests proving expected outcome does not read current pricing odds.

## 4. Print/receipt

- [x] Remove implicit `TchContext` dependency from `TicketReceiptAssembler`.
- [x] Fail fast on required missing print metadata with clear message.
- [x] Add `ReceiptBrandingDisplayMode` V1 enum.
- [x] Add tenant/outlet display mode settings if storage is ready, otherwise default in formatter.
- [x] Implement AUTO, NAME_ONLY, HEADER_ONLY, NAME_AND_HEADER in `TicketReceiptBrandingFormatter`.
- [x] Add receipt tests for tenant header/name and outlet header/name combinations.

## 5. core.drawresult support

- [x] Add `ListReconciliationDrawResultsQuery`.
- [x] Implement tenant business date lookup.
- [ ] Add tests for resulted/eligible draw filtering.

## 6. core.payout support

- [x] Add `ListPayoutClaimsForDrawQuery`.
- [x] Add `ListPayoutPaymentsForDrawQuery`.
- [x] Add `GetPayoutSummaryForDrawQuery`.
- [x] Include ticket public/display codes in payout rows when available.

## 6b. core.ledger support

- [x] Add `ListLedgerEntriesForDrawQuery`.
- [x] Add `GetLedgerSummaryForDrawQuery`.
- [x] Include ticket public/display codes for ledger-linked ticket and payout entries.

## 7. core.reconciliation domain

- [x] Create `core.reconciliation` package/module boundary.
- [x] Add `ReconciliationRun` domain model.
- [x] Add `ReconciliationAnomaly` domain model.
- [x] Add enums: `ReconciliationRunStatus`, `ReconciliationAnomalyType`, `ReconciliationSeverity`, `ReconciliationAnomalyStatus`.
- [x] Add anomaly fingerprint policy.
- [x] Add persistence entities/repositories/migrations.
- [x] Add idempotent upsert behavior for anomalies.

## 8. core.reconciliation batch

- [x] Add `DailyReconciliationJobConfig`.
- [x] Add reader for draw results to verify.
- [x] Add processor loading sales/payout read models via QueryBus.
- [x] Add writer storing anomalies and updating run counters.
- [x] Add scheduler for tenant-local midnight.
- [x] Add config flags/gates.

## 9. Ops controller

- [x] Add `ReconciliationOpsController` under `core.reconciliation.internal.infra.web`.
- [x] Expose `POST /platform/ops/reconciliation/daily-runs`.
- [x] Require SUPER_ADMIN.
- [x] Require reason for forced run.
- [x] Add audit log.
- [x] Add list/detail endpoints for runs/anomalies if useful for admin UI.

## 10. Notifications and CSV

- [x] Generate CSV from persisted anomalies for run.
- [x] Include `ticket_code`, `public_code`, `display_code` columns.
- [x] Send email after commit only when anomalies meet threshold.
- [ ] Add tests for CSV escaping/format.

## 11. Integration tests

- [ ] Scenario: counts match but wrong ticket is winner -> detail anomaly created.
- [ ] Scenario: ticket result missing after applied result -> anomaly created.
- [ ] Scenario: winner without claim -> anomaly created.
- [ ] Scenario: claim for non-winner -> critical anomaly created.
- [ ] Scenario: paid non-winning ticket -> critical anomaly created.
- [ ] Scenario: rerun same day -> anomalies not duplicated.
