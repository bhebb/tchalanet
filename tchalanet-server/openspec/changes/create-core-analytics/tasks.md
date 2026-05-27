# Tasks

## 1. Domain skeleton

- [ ] Create `core.analytics.api` packages for query/command/model.
- [ ] Create `core.analytics.internal.application` packages.
- [ ] Create `core.analytics.internal.infra.persistence`, `infra.event`, `infra.scheduler`, `infra.web.ops`.
- [ ] Add ArchUnit/module boundary expectations if needed.

## 2. Tables and migrations

- [ ] Create `analytics_daily` table.
- [ ] Create `analytics_draw` table.
- [ ] Create `analytics_session` table if POS session dashboard should be pre-aggregated V1.
- [ ] Create `analytics_recompute_run` table if recompute audit/status is needed.
- [ ] Add audit columns: created_at, updated_at, deleted_at if soft-delete, version.
- [ ] Add `trg_analytics_daily__set_updated_at`.
- [ ] Add `trg_analytics_draw__set_updated_at`.
- [ ] Add trigger(s) for any additional analytics tables.

## 3. Indexes

- [ ] Add unique index for daily dimensions.
- [ ] Add tenant/date indexes.
- [ ] Add dimension/date indexes.
- [ ] Add draw/date/channel indexes.
- [ ] Add session/seller/outlet indexes.
- [ ] Add processed_event lookup/purge indexes if missing.

## 4. Atomic SQL primitives

- [ ] Evaluate whether `increment_analytics_daily(...)` is required.
- [ ] If added, keep it as an atomic upsert/increment only.
- [ ] Ensure Java projectors decide which deltas to apply.
- [ ] Add tests or migration checks for conflict behavior.

## 5. Projectors and events

- [ ] Create `AnalyticsEventListener` with `@TransactionalEventListener(AFTER_COMMIT)`.
- [ ] Use processed-event idempotence with stable handler keys, for example:
  - [ ] `analytics.daily`
  - [ ] `analytics.draw`
  - [ ] `analytics.session`
- [ ] Bind tenant context for listeners if processed-event/RLS requires it.
- [ ] Consume only public API events from source domains.
- [ ] Do not import `core.<domain>.internal.*`.

## 6. Queries

- [ ] Implement `GetCashierDashboardStatsQuery`.
- [ ] Implement `GetTenantDashboardStatsQuery`.
- [ ] Implement `GetPlatformDashboardStatsQuery`.
- [ ] Implement `GetTenantKpisQuery`.
- [ ] Implement report queries needed by `features.reporting`.

## 7. Recompute

- [ ] Implement `RecomputeAnalyticsDailyCommand`.
- [ ] Recompute from sales/settlement/payout/session source tables, not from processed events.
- [ ] Make recompute window explicit with from/to dates and optional tenant.
- [ ] Add locking/guard to avoid concurrent recompute for the same scope/window.
- [ ] Record recompute run summary if table is created.

## 8. Purge

- [ ] Implement `PurgeAnalyticsCommand`.
- [ ] Purge processed-event markers older than retention.
- [ ] Purge analytics only according to configured retention.
- [ ] Add scheduler with active flag and cron.
- [ ] Keep scheduler thin and delegate to command handler.

## 9. Tests

- [ ] Projector idempotence tests.
- [ ] Atomic upsert concurrency test if possible.
- [ ] Tenant timezone day-boundary tests.
- [ ] Query performance smoke tests with indexes.
- [ ] Recompute consistency tests comparing projection vs source data.
