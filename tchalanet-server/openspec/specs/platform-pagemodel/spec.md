# platform-pagemodel Specification

## Purpose

`platform.pagemodel` defines the runtime contract for the PageModel mini-CMS: provider sources,
widget dispatch, memoized payload resolution, and the rules for consuming analytics and public
content in dashboard surfaces.
## Requirements
### Requirement: Existing cashier dashboard becomes web-only

The existing `private.dashboard.cashier` PageModel SHALL be repositioned as a web dashboard model.

#### Scenario: Reposition cashier PageModel

- **GIVEN** the existing PageModel contains many rows such as identity, overview, quick_sale, top_selections, recent_tickets, pending_approvals, next_draws, session, and limits
- **WHEN** migrating to the new surface model
- **THEN** the web model is renamed or copied to `private.dashboard.cashier.web`
- **AND** it is not used as the mobile POS home.

### Requirement: PageModel is layout/config, not operational truth

PageModel SHALL NOT decide real-time POS readiness.

#### Scenario: POS runtime state

- **GIVEN** the app needs to know if the seller can sell
- **WHEN** rendering mobile POS home
- **THEN** it uses the cashier home BFF response for operational context, session, draw, and action availability
- **AND** it does not infer those values from PageModel.

### Requirement: Pending approvals are not shown in mobile POS V1

Mobile POS V1 SHALL NOT expose pending approval workflow.

#### Scenario: Old dashboard had pending approvals

- **GIVEN** the old PageModel includes `dashboard.cashier.pending_approvals`
- **WHEN** creating mobile POS home
- **THEN** pending approvals are excluded
- **AND** any approval-required sale is surfaced as a required change in the sale flow, not as POS home state.

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

