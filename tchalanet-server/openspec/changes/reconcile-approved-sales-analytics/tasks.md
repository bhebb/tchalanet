# Tasks

Status: IMPLEMENTED for the V1 validation/repair path; observability and broader scenario coverage
remain follow-ups.

- [x] Identify and test the seller-terminal live-stat status mismatch.
- [x] Restrict live seller statistics to official (`APPROVED`) tickets.
- [x] Turn limit-policy `REQUIRE_APPROVAL` into a V0 blocking sale decision.
- [x] Replace direct `sales_ticket` KPI reads in POS, tenant-admin dashboard, and seller-terminal
      summary with canonical `core.analytics` queries. Seller-terminal queries must be keyed by
      `SellerTerminalId`, never user ID.
- [x] Define canonical metric semantics and API names: approved ticket count, stake total, total
      paid, seller commission, calculated winnings, and paid winnings. Declare separately the
      sales business date, draw business date, and settlement business date for every report.
- [x] Add an `AnalyticsTrustState` read model/API with `READY`, `RECONCILIATION_REQUIRED`, and
      `UNAVAILABLE` states; scope it by platform, tenant, seller terminal, draw, and business day.
- [x] Compare tenant and seller-terminal projection coverage with source ticket lifecycle activity;
      treat dates with no source activity as trustworthy zero-activity dates while keeping
      draw-specific coverage strict.
- [x] Run analytics event markers and projection deltas in one new transaction, and give
      draw-result/cache/sales-result after-commit listeners an explicit transaction boundary.
- [x] Make POS and reporting BFFs return an explicit unavailable KPI section plus a stable notice
      when their requested scope is not trustworthy; no UI consumer may render unavailable values
      as zero. POS, tenant dashboard and admin reports suppress KPI/report rows and disable exports
      until the requested scope is trustworthy.
- [ ] Add a platform-ops audited action to disable or re-enable metric visibility by scope and
      reason. Client code must not control the flag.
- [x] Add the V1 ticket paid-amount snapshot schema (`paid_amount` plus correction audit
      metadata) and initialize it when draw results are applied. Existing environment data must be
      backfilled as part of the pre-production database reset.
- [x] Replace the transient paid-amount adjustment event contract with an authorized ticket update:
      read the previous amount server-side, persist the effective paid amount and audit metadata,
      then project the delta after commit. Preserve `winning_amount` and all ticket-line outcomes;
      do not expose manual payout reversal in V1.
- [x] Extend the sales reconciliation snapshot with ticket result outcome, settlement timestamp,
      effective paid amount and paid-amount correction metadata. Keep `ticket_line` unchanged.
- [x] Add invariants and tests: `ticket.winning_amount` equals the sum of calculated winning line
      outcomes; `paid_amount` initially equals it and may differ only with persisted correction
      metadata; reconciliation validates and reports these two accounting bases separately.
- [x] Add the tenant-scoped sales snapshot read boundary and batch-load ticket charges, so a
      reconciliation window does not execute one charge query per ticket.
- [x] Add a read-only `VALIDATE` reconciler that compares immutable transactional snapshots to
      daily, draw, seller-terminal and seller-terminal/draw projections. It returns exact deltas
      and missing identifiers for the bounded tenant/date scope. Persistent run history is a
      follow-up; the run id is returned and the operation is audited.
- [x] Add explicit `REBUILD_AND_VALIDATE` repair for a selected tenant scope. It rebuilds from
      source snapshots under the tenant repair lock, re-runs validation, and returns `SUCCESS` only
      on an exact post-rebuild match. Mutating requests require the tenant-scoped idempotency key.
- [x] Implement the source-based replay contract: source snapshot boundary, metric-date semantics,
      tenant/platform scope, repair lock, atomic replacement and audit behavior. See `design.md`.
- [x] Define and implement the operational tenant-dashboard trust contract: profitability KPIs,
      paginated terminal performance, channel drilldown, and explicit unavailable states. See
      `design.md`.
- [x] Define the dashboard period comparison and PageModel migration: `TODAY`, `YESTERDAY`,
      `THIS_WEEK`, `LAST_WEEK`, provider slices, source queries, JSON schema versioning, and
      fallback rules. See `design.md`; implementation remains pending.
- [x] Define the independent sales-trend window: seven daily points ending at the selected
      period's upper bound, with a bar-chart contract using date and sales axes. Month/year
      aggregation remains a future chart-window option.
- [ ] Make projection processing atomic with its idempotency marker, and make cancellation reverse
      all affected daily, draw and seller-terminal financial projections using immutable ticket
      snapshots.
- [ ] Add alerts for projection failure or reconciliation mismatch and automatically mark the
      affected scope unavailable.
- [ ] Run the resulted-draw operational check after every result application: all eligible tickets
      must be resolved before settlement; after the attention threshold, send a deduplicated
      platform Web and Slack notification containing pending and failed ticket counts.
- [ ] Remove remaining approval-only lifecycle APIs and persistence states after the V0 data reset.
- [~] Add end-to-end reconciliation coverage for directly approved sales, limit rejection,
      cancellation, failed projection, recompute, and unavailable-KPI rendering. The focused
      ticket → result → paid-amount adjustment → validate/rebuild/idempotent retry flow is covered now;
      rejection, cancellation, forced projection failure and unavailable-KPI scenarios remain.
- [ ] Run focused core sales and analytics tests after the V0 approval removal.
