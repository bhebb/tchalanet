# Tasks

## 1. Move persistence

- [ ] Move `StatsDailyEntity` to `AnalyticsDailyEntity`.
- [ ] Move `StatsDrawEntity` to `AnalyticsDrawEntity`.
- [ ] Move repositories to `core.analytics.internal.infra.persistence`.
- [ ] Rename table mappings from `stats_daily`/`stats_draw` to `analytics_daily`/`analytics_draw` if migration creates new tables.
- [ ] Add `tenant_id` explicitly to daily rows where needed for RLS-safe reads.

## 2. Replace event log

- [ ] Remove `StatsEventLogEntity` usage.
- [ ] Use `ProcessedEventPort` / `processed_event` pattern.
- [ ] Define stable handler keys.
- [ ] Ensure event consumers bind tenant context when needed.

## 3. Move listeners and projectors

- [ ] Move `StatsAggregatesEventListener` to `AnalyticsEventListener`.
- [ ] Move `StatsDailyUpdaterService` to `AnalyticsDailyProjector`.
- [ ] Move `StatsDrawUpdaterService` to `AnalyticsDrawProjector`.
- [ ] Add `AnalyticsSessionProjector` if POS session summary is needed.
- [ ] Use `@TransactionalEventListener(AFTER_COMMIT)`.

## 4. Fix core session events

- [ ] Expose `SalesSessionOpenedEvent` and `SalesSessionClosedEvent` under `core.session.api.event`.
- [ ] Replace internal imports.
- [ ] Add ArchUnit/compile validation for no imports from `core.session.internal`.

## 5. Fix time and ID handling

- [ ] Inject `Clock` where time is used.
- [ ] Replace `Instant.now()` with `Instant.now(clock)`.
- [ ] Use tenant timezone for tenant daily metrics when semantically tenant-local.
- [ ] Remove `UUID.randomUUID()` from application services.

## 6. Queries replacing feature stats services

- [ ] Create `GetTenantDashboardStatsQuery`.
- [ ] Create `GetPlatformDashboardStatsQuery`.
- [ ] Create `GetCashierDashboardStatsQuery` if POS integration is included now.
- [ ] Update `features.tenantadmin.dashboard` to use QueryBus.
- [ ] Update `features.platformadmin.dashboard` to use QueryBus.
- [ ] Remove imports from `features.stats`.

## 7. SQL and migrations

- [ ] Add analytics table migrations.
- [ ] Add triggers `trg_analytics_daily__set_updated_at`, `trg_analytics_draw__set_updated_at`.
- [ ] Add required indexes.
- [ ] Add optional atomic functions if chosen.
- [ ] Preserve or migrate old data if required.

## 8. Tests

- [ ] Projector idempotence test.
- [ ] Duplicate event does not double count.
- [ ] Ticket cancellation adjusts counts correctly.
- [ ] Settlement/winnings update uses correct source.
- [ ] Payout paid metrics use payout events, not claim existence.
- [ ] Dashboard query returns zero/default shape when no data.
