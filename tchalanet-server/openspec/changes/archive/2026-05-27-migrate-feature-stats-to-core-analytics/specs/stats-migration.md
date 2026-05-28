# Spec: migrate features.stats to core.analytics

## CHANGED Requirements

### Requirement: Dashboard stats services are no longer features

Dashboard stats SHALL be provided by `core.analytics` queries, not by `features.stats` services.

#### Scenario: Tenant admin dashboard loads stats

- **WHEN** tenant admin dashboard provider builds KPI payload
- **THEN** it SHALL call `QueryBus.ask(new GetTenantDashboardStatsQuery(...))`
- **AND** it SHALL NOT inject `features.stats.tenantdashboard.app.TenantDashboardStatsService`.

#### Scenario: Platform admin dashboard loads stats

- **WHEN** platform admin dashboard provider builds KPI payload
- **THEN** it SHALL call `QueryBus.ask(new GetPlatformDashboardStatsQuery(...))`
- **AND** it SHALL NOT inject `features.stats.platformdashboard.PlatformDashboardStatsService`.

### Requirement: Stats persistence is owned by core.analytics

Stats/analytics JPA entities SHALL live in `core.analytics.internal.infra.persistence`.

#### Scenario: A developer adds an analytics table entity

- **WHEN** the entity maps an analytics projection table
- **THEN** it SHALL be placed under `core.analytics.internal.infra.persistence`
- **AND** it SHALL NOT be placed under `features.*`.

### Requirement: Event consumers use public API events only

Analytics event consumers SHALL only import public API events from source domains.

#### Scenario: Analytics consumes a session event

- **WHEN** analytics listens to session opened/closed
- **THEN** the event class SHALL be exposed under `core.session.api.event`
- **AND** analytics SHALL NOT import `core.session.internal.*`.

### Requirement: Legacy stats event log is replaced

Analytics SHALL use the shared processed-event idempotence pattern.

#### Scenario: An analytics projector receives an event

- **WHEN** the event is processed
- **THEN** analytics SHALL mark `(tenant, handler_key, event_id)` as processed
- **AND** duplicate events SHALL be skipped.
