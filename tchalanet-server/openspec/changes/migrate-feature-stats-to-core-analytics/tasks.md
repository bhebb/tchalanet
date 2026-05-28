# Tasks

## 1. Move persistence

- [x] Move `StatsDailyEntity` to `AnalyticsDailyEntity`.
- [x] Move `StatsDrawEntity` to `AnalyticsDrawEntity`.
- [x] Move repositories to `core.analytics.internal.infra.persistence`.
- [x] Rename table mappings from `stats_daily`/`stats_draw` to `analytics_daily`/`analytics_draw` if migration creates new tables.
- [x] Add `tenant_id` explicitly to daily rows where needed for RLS-safe reads.

## 2. Replace event log

- [x] Remove `StatsEventLogEntity` usage.
- [x] Use `ProcessedEventPort` / `processed_event` pattern.
- [x] Define stable handler keys.
- [x] Ensure event consumers bind tenant context when needed.

## 3. Move listeners and projectors

- [x] Move `StatsAggregatesEventListener` to `AnalyticsEventListener`.
- [x] Move `StatsDailyUpdaterService` to `AnalyticsDailyProjector`.
- [x] Move `StatsDrawUpdaterService` to `AnalyticsDrawProjector`.
- [ ] Add `AnalyticsSessionProjector` if POS session summary is needed. *(deferred V2)*
- [x] Use `@TransactionalEventListener(AFTER_COMMIT)`.

## 4. Fix core session events

- [x] Expose `SalesSessionOpenedEvent` and `SalesSessionClosedEvent` under `core.session.api.event`.
- [x] Replace internal imports (4 publishers updated; internal copies `@Deprecated`).
- [ ] Add ArchUnit/compile validation for no imports from `core.session.internal`.

## 5. Fix time and ID handling

- [ ] Inject `Clock` where time is used. *(`AnalyticsDrawProjector` still has `Instant.now()` without injected Clock)*
- [ ] Replace `Instant.now()` with `Instant.now(clock)`. *(same — AnalyticsDrawProjector)*
- [x] Use tenant timezone for tenant daily metrics when semantically tenant-local.
- [x] Remove `UUID.randomUUID()` from application services.

## 6. Queries replacing feature stats services

- [x] Create `GetTenantDashboardStatsQuery`.
- [x] Create `GetPlatformDashboardStatsQuery`.
- [x] Create `GetCashierDashboardStatsQuery` if POS integration is included now.
- [x] Update `features.tenantadmin.dashboard` to use QueryBus.
- [x] Update `features.platformadmin.dashboard` to use QueryBus.
- [x] Remove imports from `features.stats`.

## 7. SQL and migrations

- [x] Add analytics table migrations (V109).
- [x] Add triggers `trg_analytics_daily__set_updated_at`, `trg_analytics_draw__set_updated_at`.
- [x] Add required indexes.
- [ ] Add optional atomic functions if chosen. *(evaluated and not needed — Java projectors handle deltas)*
- [ ] Preserve or migrate old data if required. *(old `stats_daily`/`stats_draw` tables left in place; data migration deferred)*

## 8. Tests

- [ ] Projector idempotence test.
- [ ] Duplicate event does not double count.
- [ ] Ticket cancellation adjusts counts correctly.
- [ ] Settlement/winnings update uses correct source.
- [ ] Payout paid metrics use payout events, not claim existence.
- [ ] Dashboard query returns zero/default shape when no data.
