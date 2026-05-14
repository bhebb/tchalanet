# Tasks

## 1. Sales API pruning

- [ ] Move controller-only commands from `core.sales.api.command` to `core.sales.internal.application.command.model`.
- [ ] Keep `ProcessOfflineSubmissionForSalesCommand` in `core.sales.api.command`.
- [ ] Keep `GetTicketPrintViewQuery` in `core.sales.api.query` because `features.cashier` consumes it.
- [ ] Keep cashier ticket list/details queries in `core.sales.api.query` if consumed by `features.cashier`.
- [ ] Keep payout/draw settlement queries in `api.query` only if consumed by those modules.
- [ ] Keep cross-domain events in `core.sales.api.event`.

## 2. Sales controllers

- [ ] Remove duplicate `POST /tenant/tickets` between `TicketLifecycleController` and `TicketSalesController`.
- [ ] Keep target sales controllers: `TicketLifecycleController`, `TicketQueryController`, `TicketPrintController`.
- [ ] Ensure controllers use `@CurrentContext TchRequestContext` where tenant/user/POS hint is needed.
- [ ] Ensure POS context IDs are not accepted from body.
- [ ] Ensure controller writes are audited where required.
- [ ] Ensure idempotency remains on sell endpoint.

## 3. Features cashier

- [ ] Add `features.cashier.tickets` slice.
- [ ] Add cashier ticket page endpoint.
- [ ] Add cashier ticket details endpoint.
- [ ] Add print endpoint/orchestration that calls `GetTicketPrintViewQuery` then `platform.document` when rendering server-side.
- [ ] Ensure feature contains no business invariants.

## 4. Offlinesync API pruning

- [ ] Move controller-only commands from `core.offlinesync.api.command` to `core.offlinesync.internal.application.command.model`.
- [ ] Keep `GetOfflineSubmissionForSalesQuery` in `core.offlinesync.api.query`.
- [ ] Keep `OfflineSubmissionForSalesView` in `core.offlinesync.api.model`.
- [ ] Keep only externally consumed events in `core.offlinesync.api.event`.

## 5. Offlinesync intake and delayed processing

- [ ] Ensure `ReceiveOfflineBatchCommandHandler` only validates technical proofs and persists submissions.
- [ ] Add statuses: `RECEIVED`, `TECH_REJECTED`, `READY_FOR_SALES`, `SALES_PROCESSING`, `SALES_ACCEPTED`, `SALES_REJECTED`, `REVIEW_REQUIRED`, `RETRY_PENDING`.
- [ ] Add scheduler `OfflineSubmissionSalesProcessingScheduler`.
- [ ] Add command `DispatchReadyOfflineSubmissionsCommand` internal to offlinesync.
- [ ] Add lock/claim logic for bounded batch processing.
- [ ] Add config `tch.offlinesync.sales-processing.*`.

## 6. Sales offline processing

- [ ] Implement `ProcessOfflineSubmissionForSalesCommandHandler` in sales.
- [ ] Handler asks `GetOfflineSubmissionForSalesQuery`.
- [ ] Handler validates draw/cutoff/pricing/limit/session rules.
- [ ] Handler creates official ticket or returns rejected/review decision.
- [ ] Handler publishes `OfflineSubmissionAcceptedAsTicketEvent` or `OfflineSubmissionRejectedBySalesEvent` after commit.

## 7. Decision recording

- [ ] Add offlinesync listener for sales accepted/rejected events.
- [ ] Listener calls internal `RecordOfflineSubmissionSalesDecisionCommand`.
- [ ] Ensure idempotency for decision recording.
- [ ] Add indexes/constraints to prevent duplicate ticket creation from one submission.

## 8. Tests

- [ ] Controller tests for route conflicts and security annotations.
- [ ] Handler tests for receive batch status decisions.
- [ ] Scheduler/dispatcher tests for max-items and lock behavior.
- [ ] Sales offline processing tests for accepted/rejected/review outcomes.
- [ ] ArchUnit/API boundary tests ensuring internal commands are not imported cross-module.
