# Change: create-core-analytics

## Status

Proposed.

## Summary

Create a new core domain `core.analytics` to own analytical read projections, KPIs, aggregates, recompute, purge, indexes and event-driven projectors.

`core.analytics` is not the financial source of truth. It is the owner of reliable, fast, recomputable analytical projections derived from source-of-truth domains.

## Why

Dashboard and reporting reads need to be correct and fast. The current legacy implementation lives under `features.stats` and `features.reporting` with persistence, listeners and direct SQL readers. This violates feature boundaries and creates unclear ownership.

A core analytics domain is justified because analytics has:

- persistent tables;
- event consumers;
- idempotence;
- recompute;
- purge;
- DB indexes and atomic increments;
- consumers across multiple features.

## Core principle

```text
core.analytics owns derived read truth.
It does not invent financial truth.
It derives metrics from sales, settlement, payout, session and eventually ledger.
```

## Scope

### In scope

- Create `core.analytics` package and public query API.
- Add analytics tables and migrations:
  - `analytics_daily`
  - `analytics_draw`
  - `analytics_session` if required for POS V1
  - optional `analytics_recompute_run`
- Add event projectors/listeners consuming core events after commit.
- Add processed-event idempotence for projectors.
- Add recompute commands/handlers.
- Add purge commands/handlers and scheduler.
- Add indexes for dashboard/reporting access patterns.
- Add SQL triggers and optional atomic increment functions.

### Out of scope V1

- Advanced BI/OLAP.
- User-customized dashboards.
- Complex historical rollups beyond simple daily/session/draw projections.
- Ledger-based accounting truth until ledger is mature.

## Target package shape

```text
core/analytics/
  api/
    query/
      GetCashierDashboardStatsQuery.java
      GetTenantDashboardStatsQuery.java
      GetPlatformDashboardStatsQuery.java
      GetTenantKpisQuery.java
      GetSalesReportQuery.java
      GetOutletReportQuery.java
    command/
      RecomputeAnalyticsDailyCommand.java
      PurgeAnalyticsCommand.java
    model/
      CashierDashboardStatsView.java
      TenantDashboardStatsView.java
      PlatformDashboardStatsView.java
      TenantKpisView.java
      SalesReportLine.java
      OutletReportLine.java
      AnalyticsDimensionType.java
  internal/
    application/
      query/handler/
      command/handler/
      service/
        AnalyticsDailyProjector.java
        AnalyticsDrawProjector.java
        AnalyticsSessionProjector.java
        AnalyticsRecomputeService.java
        AnalyticsPurgeService.java
    infra/
      persistence/
        AnalyticsDailyEntity.java
        AnalyticsDrawEntity.java
        AnalyticsSessionEntity.java
        AnalyticsDailyRepository.java
        AnalyticsDrawRepository.java
        AnalyticsReportingReader.java
      event/
        AnalyticsEventListener.java
      scheduler/
        AnalyticsMaintenanceScheduler.java
      web/ops/
        AnalyticsOpsController.java
```

## Metric definitions V1

- `ticketsSold`: confirmed sold tickets excluding final cancelled/void tickets.
- `ticketsCancelled`: cancelled/void tickets.
- `grossSales`: money collected/sale amount for valid tickets.
- `stakeTotal`: sum of ticket line stakes before optional charges if distinct from gross sales.
- `winningsCalculated`: gains calculated from settlement/resulted ticket facts.
- `payoutsPaid`: money actually paid via posted payout payments.
- `netRevenueEstimated`: gross sales minus calculated winnings.
- `netRevenuePaidBasis`: gross sales minus posted paid payouts.
- `openPayableClaims`: payable claims not fully paid.
- `pendingSettlementDraws`: resulted draws not fully settled.

## Source-of-truth mapping

| Metric | Source |
|---|---|
| ticketsSold | sales ticket placed/final state |
| ticketsCancelled | sales ticket cancelled/void |
| grossSales/stake | sales money snapshot |
| winningsCalculated | settlement / ticket resulted facts |
| payoutsPaid | payout payment posted/reversed facts |
| session counts | session opened/closed facts |
| platform totals | analytics daily global rollup or catalog/tenant read projections |

## SQL baseline rules

Existing baseline uses `public.set_updated_at()` triggers and an atomic function `public.increment_draw_exposure(...)`. `core.analytics` SHALL follow this pattern:

- Every analytics table with `updated_at` SHALL have a `trg_<table>__set_updated_at` trigger.
- SQL functions may be added only as technical atomic primitives.
- Business logic remains in Java projectors/handlers.
- Upsert functions must use correct unique indexes and `WHERE deleted_at IS NULL` if soft-delete is used.

## Retention policy V1

- `processed_event` analytics handler markers: 180 days.
- `analytics_daily`: at least 24 months.
- `analytics_draw`: at least 24 months.
- `analytics_session`: at least 12 to 24 months depending on volume.
- Do not purge analytics more aggressively than source-data retention supports.

## Risks

- Incorrect metric definitions can mislead admins.
- Wrong timezone choice can shift daily KPIs.
- Event replay without idempotence can double-count.
- Direct query from sales tables on every dashboard can become too slow.

## Rollout strategy

1. Create tables and queries while keeping old stats intact.
2. Run dual-read/compare in dev/staging.
3. Switch dashboard providers to `core.analytics` queries.
4. Remove feature-to-feature stats dependency.
5. Decommission old `stats_*` tables when safe or migrate data.
