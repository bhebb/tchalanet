# Change: migrate-feature-stats-to-core-analytics

## Status

Proposed.

## Summary

Migrate legacy `features.stats` code into `core.analytics` and remove feature-to-feature Java dependencies.

Current legacy code includes:

- `features.stats.aggregates.persistence.StatsDailyEntity`
- `features.stats.aggregates.persistence.StatsDrawEntity`
- `features.stats.aggregates.persistence.StatsEventLogEntity`
- `features.stats.aggregates.StatsAggregatesEventListener`
- `StatsDailyUpdaterService`, `StatsDrawUpdaterService`, `RecomputeDailyStatsService`
- tenant/platform dashboard stats services consumed by `features.tenantadmin` and `features.platformadmin`

These are not UI features. They own persistence, event consumption and projections.

## Why

`features.stats` violates feature responsibilities because it:

- owns JPA entities and stats tables;
- listens to core domain events;
- maintains event idempotence;
- performs recompute;
- is consumed by other features.

`core.analytics` is the correct target owner.

## Scope

### In scope

- Move legacy stats persistence and projectors into `core.analytics.internal`.
- Rename `stats_*` entities/services to `analytics_*` equivalents.
- Replace `StatsEventLogEntity` with the shared `processed_event` pattern where possible.
- Expose tenant/platform dashboard stats as `core.analytics.api.query` queries.
- Remove imports from `features.tenantadmin` and `features.platformadmin` to `features.stats`.
- Correct boundary violations such as imports from `core.session.internal`.
- Correct time/ID handling issues.

### Out of scope

- Full historical data migration in production if not needed for V1.
- Advanced charting/BI.
- User-configurable dashboard metrics.

## Required fixes observed in legacy code

### Boundary violation

Current code imports:

```java
com.tchalanet.server.core.session.internal.domain.event.SalesSessionClosedEvent
com.tchalanet.server.core.session.internal.domain.event.SalesSessionOpenedEvent
```

This must be replaced by public API events:

```java
com.tchalanet.server.core.session.api.event.SalesSessionClosedEvent
com.tchalanet.server.core.session.api.event.SalesSessionOpenedEvent
```

or the listener must temporarily stop consuming these events until the session domain exposes them.

### Idempotence

Replace `stats_event_log` with `processed_event` and stable handler keys:

```text
analytics.daily
analytics.draw
analytics.session
```

### Time handling

- Replace `Instant.now()` with `Instant.now(clock)`.
- Replace `LocalDate.now(UTC)` for tenant dashboards with tenant-zone date where semantic owner is tenant.
- Inject `Clock` into services using current time.

### ID generation

- Avoid `UUID.randomUUID()` in application/services.
- Let JPA/DB generate entity IDs or use project `IdGenerator` in application layer.

### Transaction boundaries

- Avoid `REQUIRES_NEW` on low-level repositories unless explicitly justified.
- Place transaction boundary at listener/projector/application command handler level.

## Migration mapping

```text
features.stats.aggregates.persistence.StatsDailyEntity
→ core.analytics.internal.infra.persistence.AnalyticsDailyEntity

features.stats.aggregates.persistence.StatsDrawEntity
→ core.analytics.internal.infra.persistence.AnalyticsDrawEntity

features.stats.aggregates.StatsAggregatesEventListener
→ core.analytics.internal.infra.event.AnalyticsEventListener

features.stats.aggregates.app.StatsDailyUpdaterService
→ core.analytics.internal.application.service.AnalyticsDailyProjector

features.stats.aggregates.app.StatsDrawUpdaterService
→ core.analytics.internal.application.service.AnalyticsDrawProjector

features.stats.aggregates.app.RecomputeDailyStatsService
→ core.analytics.internal.application.command.handler.RecomputeAnalyticsDailyCommandHandler
```

## Table migration options

Preferred V1:

- Rename `stats_daily` to `analytics_daily` or create new table and backfill.
- Rename `stats_draw` to `analytics_draw` or create new table and backfill.
- Drop/deprecate `stats_event_log` after switching to `processed_event`.

If production risk is high:

- Keep old tables temporarily.
- Create new analytics tables.
- Dual-write in staging only.
- Switch reads to new queries once verified.

## Rollout

1. Introduce `core.analytics` queries and tables.
2. Move projectors and listeners.
3. Replace dashboard feature service imports with QueryBus calls.
4. Verify metrics against old stats in staging.
5. Remove old `features.stats` service layer.
6. Remove or archive old tables when safe.
