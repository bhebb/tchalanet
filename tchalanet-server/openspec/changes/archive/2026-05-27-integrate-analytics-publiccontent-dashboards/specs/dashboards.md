# Spec: dashboard integration

## ADDED Requirements

### Requirement: Features consume analytics through QueryBus

Dashboard features SHALL consume analytics through stable `core.analytics` queries.

#### Scenario: Tenant admin dashboard loads business KPIs

- **WHEN** the dashboard provider loads tenant admin KPIs
- **THEN** it SHALL dispatch `GetTenantDashboardStatsQuery`
- **AND** it SHALL not depend on `features.stats`.

#### Scenario: Cashier POS dashboard loads session KPIs

- **WHEN** cashier POS dashboard loads
- **THEN** it SHALL return compact action-oriented KPIs
- **AND** it SHALL avoid heavy report-style queries.

### Requirement: Tenant admin supports optional weekly comparison

Tenant admin dashboard SHALL support optional week comparison.

#### Scenario: Admin enables week comparison

- **WHEN** comparison mode is requested
- **THEN** analytics SHALL provide last 7 days and previous 7 days values
- **AND** the feature SHALL expose deltas in the dashboard payload.

### Requirement: Dashboards can display public content

Dashboards SHALL be able to display platform public/internal content targeted to their surface.

#### Scenario: Tenant admin dashboard requests announcements

- **WHEN** the tenant admin dashboard loads public content widget
- **THEN** it SHALL call `platform.publiccontent.api`
- **AND** return content targeted to `TENANT_ADMIN_DASHBOARD`.

#### Scenario: User personalization is not available in V1

- **WHEN** a user wants to hide public content by preference
- **THEN** the system SHALL treat this as V2 backlog
- **AND** V1 SHALL rely on platform surface/audience targeting.

### Requirement: PageModel providers use grouped payload memoization

Dynamic PageModel providers SHALL memoize grouped payloads per request where useful.

#### Scenario: Multiple widgets use the same source

- **WHEN** multiple widgets use the same provider source
- **THEN** the provider SHOULD load grouped data once through `PageModelResolutionContext`
- **AND** dispatch slices by widget ID.
