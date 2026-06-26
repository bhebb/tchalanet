# Tasks — analytics-dashboard-reporting-projections-v1

> Checkpoint: this change owns backend analytics projections for dashboards/reports.

## Slice 1 — Contracts

- [x] Extend `PlatformDashboardStatsView` with `dailyBreakdown` and `gameBreakdown`.
- [x] Extend tenant daily dashboard points with `netRevenueEstimated`.
- [x] Document that `gameBreakdown` is a contract now, populated from `analytics_selection`.

## Slice 2 — Payout Lifecycle Events

- [x] Add `TicketPayoutPaidEvent`.
- [x] Add `TicketPayoutReversedEvent`.
- [x] Add handlers for existing `MarkTicketPayoutPaidCommand` and `MarkTicketPayoutReversedCommand`.
- [x] Publish payout events after commit from those handlers.
- [x] Auto-settle winning tickets as paid when official draw results are applied.
- [x] Publish payout paid/reversed events from corrected result reconciliation.
- [x] Publish winning-settlement reversal events so corrected results adjust `winningsCalculated`.

## Slice 3 — Analytics Projection

- [x] Consume `TicketWinningSettlementCreatedEvent` for `winningsCalculated`.
- [x] Consume `TicketPayoutPaidEvent` for `payoutsPaid`.
- [x] Consume `TicketPayoutReversedEvent` as negative `payoutsPaid` V1 delta.
- [x] Stop relying on `TicketResultedEvent.totalPayout` for paid payout metrics.

## Slice 4 — Query Handlers

- [x] Populate platform `dailyBreakdown` from PLATFORM rows.
- [x] Populate tenant daily `netRevenueEstimated`.
- [x] Populate platform/tenant `gameBreakdown` from `analytics_selection`.

## Slice 5 — Validation

- [x] Focused backend tests compile/pass for analytics and sales payout handlers.
- [x] OpenSpec validate strict.

## Slice 6 — PageModel Wiring

- [x] Expose platform sales, trend, game breakdown and top-tenants as dedicated dynamic payload slices.
- [x] Expose tenant sales trend and game breakdown as dedicated dynamic payload slices.
- [x] Wire tenant/superadmin dashboard templates to `TrendChartWidget` and `BreakdownListWidget`.
- [x] Add FR/EN/HT labels for the new dashboard sections/widgets.
- [x] Use a 7-day analytics window for dashboard trend/breakdown widgets while keeping today KPIs scoped to today.
- [x] Add widget support/tests for short date labels and currency-formatted breakdown values.

## Slice 7 — Financial Snapshots

- [x] Route normal ticket-line odds through `core.pricing` seller-terminal odds resolution.
- [x] Route promotion-created free game line odds through the same seller-terminal odds resolution.
- [x] Snapshot the effective odds in `TicketLine.oddsSnapshot` and calculate potential payout from it.
- [x] Add focused unit coverage for seller-terminal effective odds during sale preparation.
- [x] Document commission, fee and promotion analytics as sale-time snapshots.
- [x] Add schema/projection support for seller commission totals by day, draw and seller terminal.
- [x] Add schema/projection support for charge totals by payer.
- [x] Add schema/projection support for promotion-funded/free-play metrics.

## Slice 8 — Tenant Financial Drilldowns

- [x] Add core analytics query/view for tenant financial breakdowns by day, draw and seller-terminal/day.
- [x] Expose tenant-admin financial breakdown endpoint through `features.tenantadmin`.
- [x] Add seller-terminal-by-draw projection/table for exact terminal-vendeur par tirage drilldowns.
- [x] Document that exact seller-terminal-by-draw rows come from the dedicated projection, not UI inference.
- [x] Add focused unit coverage for seller-terminal-by-draw projection and empty tenant financial breakdowns.
