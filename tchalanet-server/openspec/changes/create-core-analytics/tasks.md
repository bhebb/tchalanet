# Tasks

## 1. Domain skeleton

- [x] Create `core.analytics.api` packages for query/command/model.
- [x] Create `core.analytics.internal.application` packages.
- [x] Create `core.analytics.internal.infra.persistence`, `infra.event`, `infra.scheduler`, `infra.web.ops`.
- [ ] Add ArchUnit/module boundary expectations if needed.

## 2. Tables and migrations

- [x] Create `analytics_daily` table.
- [x] Create `analytics_draw` table.
- [ ] Create `analytics_session` table if POS session dashboard should be pre-aggregated V1. *(skipped V1 — session projector not needed)*
- [ ] Create `analytics_recompute_run` table if recompute audit/status is needed. *(skipped V1 — no run summary table)*
- [x] Add audit columns: created_at, updated_at, deleted_at if soft-delete, version.
- [x] Add `trg_analytics_daily__set_updated_at`.
- [x] Add `trg_analytics_draw__set_updated_at`.
- [ ] Add trigger(s) for any additional analytics tables. *(n/a — analytics_session not created)*

## 3. Indexes

- [x] Add unique index for daily dimensions.
- [x] Add tenant/date indexes.
- [x] Add dimension/date indexes.
- [x] Add draw/date/channel indexes.
- [ ] Add session/seller/outlet indexes. *(n/a — analytics_session not created)*
- [x] Add processed_event lookup/purge indexes if missing.

## 4. Atomic SQL primitives

- [x] Evaluate whether `increment_analytics_daily(...)` is required. *(decision: not needed — Java projectors apply deltas via upsertAndIncrement)*
- [ ] If added, keep it as an atomic upsert/increment only.
- [x] Ensure Java projectors decide which deltas to apply.
- [ ] Add tests or migration checks for conflict behavior.

## 5. Projectors and events

- [x] Create `AnalyticsEventListener` with `@TransactionalEventListener(AFTER_COMMIT)`.
- [x] Use processed-event idempotence with stable handler keys, for example:
  - [x] `analytics.daily`
  - [x] `analytics.draw`
  - [ ] `analytics.session` *(n/a — no AnalyticsSessionProjector)*
- [x] Bind tenant context for listeners if processed-event/RLS requires it.
- [x] Consume only public API events from source domains.
- [x] Do not import `core.<domain>.internal.*`.

## 6. Queries

- [x] Implement `GetCashierDashboardStatsQuery`.
- [x] Implement `GetTenantDashboardStatsQuery`.
- [x] Implement `GetPlatformDashboardStatsQuery`.
- [x] Implement `GetTenantKpisQuery`.
- [x] Implement report queries needed by `features.reporting`. *(GetSalesReportQuery + GetOutletReportQuery)*

## 7. Recompute

- [x] Implement `RecomputeAnalyticsDailyCommand`.
- [x] Recompute from sales/settlement/payout/session source tables, not from processed events.
- [x] Make recompute window explicit with from/to dates and optional tenant.
- [x] Add locking/guard to avoid concurrent recompute for the same scope/window.
- [ ] Record recompute run summary if table is created. *(skipped — no analytics_recompute_run table V1)*

## 8. Purge

- [x] Implement `PurgeAnalyticsCommand`.
- [x] Purge processed-event markers older than retention.
- [x] Purge analytics only according to configured retention.
- [x] Add scheduler with active flag and cron.
- [x] Keep scheduler thin and delegate to command handler.

## 9. Tests

- [ ] Projector idempotence tests.
- [ ] Atomic upsert concurrency test if possible.
- [ ] Tenant timezone day-boundary tests.
- [ ] Query performance smoke tests with indexes.
- [ ] Recompute consistency tests comparing projection vs source data.
