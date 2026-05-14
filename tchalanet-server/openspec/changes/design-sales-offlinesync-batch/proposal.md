# Change: design-sales-offlinesync-batch

## Summary

Design the next boundary split between `core.sales`, `core.offlinesync`, and `features.cashier` after the operational POS claim/grant work.

The change establishes:

- `core.sales` owns official tickets, lifecycle, verification, ticket details, and print snapshots.
- `core.offlinesync` owns offline grants, technical submission intake, submission status, and delayed dispatch to sales.
- `features.cashier` owns cashier-facing pages/orchestration and may call public Java APIs from `core.sales` and `core.offlinesync`.
- Commands/queries are exposed in `api/` only when consumed by another Java module.
- HTTP exposure through a controller does not imply Java API exposure.

## Why

The previous first map exposed too many commands/queries in `api/`. In Tchalanet, `api/` is the public Java contract between modules, while commands and queries called only by controllers inside the same bounded context should remain internal.

Offline sync also needs to avoid synchronously calling sales when a batch arrives. Submissions are persisted first, then a configurable scheduler/command dispatches bounded chunks to sales later.

## Key decisions

1. Keep controller-only commands/queries internal.
2. Expose only cross-module Java contracts in `api/`.
3. `features.cashier` needs `GetTicketPrintViewQuery`, `ListCashierTicketsQuery` or equivalent, and `GetTicketDetailsQuery` from `core.sales.api.query`.
4. `core.offlinesync` needs `ProcessOfflineSubmissionForSalesCommand` from `core.sales.api.command`.
5. `core.sales` needs `GetOfflineSubmissionForSalesQuery` from `core.offlinesync.api.query`.
6. `ReceiveOfflineBatchCommandHandler` persists submissions only; it does not call sales directly.
7. A scheduler/batch in `core.offlinesync` dispatches ready submissions every configurable interval.
8. Sales creates the official ticket or returns a rejection/review decision.
9. Offlinesync records the final sales decision on the original submission.

## Existing controller consolidation

Current pasted code shows overlapping sales controllers:

- `TicketLifecycleController` includes sell, approve, reject, cancel.
- `TicketQueryController` includes list and details.
- `TicketSalesController` also includes sell.

Target:

- Keep three sales controller surfaces, but remove duplicate sell endpoint.
- Use one canonical sell endpoint under `TicketLifecycleController` or `TicketSalesController`, not both.
- Prefer these three sales controllers:
  - `TicketLifecycleController`
  - `TicketQueryController`
  - `TicketPrintController`

For offlinesync:

- `OfflineGrantController`
- `OfflineSyncController`
- optionally `OfflineSubmissionAdminController` for admin/ops follow-up.
