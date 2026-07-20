# Tasks

- [x] Identify and test the seller-terminal live-stat status mismatch.
- [x] Restrict live seller statistics to official (`APPROVED`) tickets.
- [x] Turn limit-policy `REQUIRE_APPROVAL` into a V0 blocking sale decision.
- [x] Replace direct `sales_ticket` KPI reads in POS, tenant-admin dashboard, and seller-terminal
      summary with canonical `core.analytics` queries. Seller-terminal queries must be keyed by
      `SellerTerminalId`, never user ID.
- [ ] Define canonical metric semantics and API names: approved ticket count, stake total, total
      paid, seller commission, calculated winnings, and paid winnings.
- [x] Add an `AnalyticsTrustState` read model/API with `READY`, `RECONCILIATION_REQUIRED`, and
      `UNAVAILABLE` states; scope it by platform, tenant, seller terminal, draw, and business day.
- [x] Make POS and reporting BFFs return an explicit unavailable KPI section plus a stable notice
      when their requested scope is not trustworthy; no UI consumer may render unavailable values
      as zero. POS, tenant dashboard and admin reports suppress KPI/report rows and disable exports
      until the requested scope is trustworthy.
- [ ] Add a platform-ops audited action to disable or re-enable metric visibility by scope and
      reason. Client code must not control the flag.
- [ ] Add a read-only reconciler that compares transactional `APPROVED` tickets to daily, draw,
      and seller-terminal projections and persists a discrepancy report with a watermark. The
      reconciler must set the tenant RLS context explicitly for every tenant-scoped read.
- [ ] Add an explicit, auditable recompute operation that rebuilds a selected scope from the
      transactional source; do not use the current destructive projection cleanup as a repair.
- [ ] Make projection processing atomic with its idempotency marker, and make cancellation reverse
      all affected financial projections using immutable ticket snapshots.
- [ ] Add alerts for projection failure or reconciliation mismatch and automatically mark the
      affected scope unavailable.
- [ ] Remove remaining approval-only lifecycle APIs and persistence states after the V0 data reset.
- [ ] Add end-to-end reconciliation coverage for directly approved sales, limit rejection,
      cancellation, failed projection, recompute, and unavailable-KPI rendering.
- [ ] Run focused core sales and analytics tests after the V0 approval removal.
