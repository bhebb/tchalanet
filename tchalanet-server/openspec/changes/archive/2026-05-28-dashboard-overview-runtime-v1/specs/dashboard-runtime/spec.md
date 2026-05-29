# Spec: Dashboard PageModel Runtime V1

## ADDED Requirements

### Requirement: Dashboards are PageModel runtime pages

Dashboards SHALL be PageModel-based pages.

| Role / surface | PageModel logical id | Provider source | Purpose |
|---|---|---|---|
| Tenant admin | `private.dashboard.admin` | `tenant_admin_dashboard` | tenant admin landing |
| Cashier web | `private.dashboard.cashier.web` | `cashier_dashboard` | cashier web landing |
| Platform admin | `private.dashboard.platform` | `platform_admin_dashboard` | super admin landing |

#### Scenario: Dashboard resolves by surface

- **GIVEN** an authenticated user
- **WHEN** the frontend requests dashboard PageModel for the current surface
- **THEN** the backend resolves the effective PageModel from context/surface
- **AND** dynamic widgets are loaded only for that PageModel.

### Requirement: Provider sources are grouped

Each dashboard SHALL use one grouped source for role-specific widgets.

#### Scenario: Tenant admin dashboard has many widgets

- **GIVEN** `private.dashboard.admin` has multiple widgets
- **WHEN** the resolver loads dynamic widgets
- **THEN** tenant admin dashboard widgets use source `tenant_admin_dashboard`
- **AND** the provider loads `TenantAdminDashboardPayload` once per request.

### Requirement: Providers use failure-aware memoization

The PageModel dynamic resolver SHALL provide a per-request `PageModelResolutionContext` that memoizes success and failure.

#### Scenario: Grouped provider fails

- **GIVEN** multiple widgets use the same failing source
- **WHEN** the first widget load fails
- **THEN** the failure is memoized
- **AND** the same grouped read is not retried for each widget
- **AND** each affected widget receives an explicit `dynamic.error`.

### Requirement: Dashboard provider failures are surfaced

A provider SHALL NOT silently swallow errors.

#### Scenario: Provider fails

- **GIVEN** a provider throws a controlled exception
- **WHEN** the resolver handles the widget
- **THEN** the response contains a `dynamic.errors` entry
- **AND** other widgets may still render.

### Requirement: Dashboard does not preload management pages

Dashboards SHALL not load detailed management lists.

Forbidden in dashboard payloads:

- complete users list;
- complete outlets list;
- complete terminals list;
- all settings;
- all i18n overrides;
- all limits;
- all promotions;
- reports;
- audit logs.

### Requirement: Dynamic provider dependency direction is safe

`core.pagemodel.api` MAY define `DynamicWidgetProvider`, but `core.pagemodel` SHALL NOT import `features.*`.

#### Scenario: Providers are wired

- **GIVEN** provider implementations live in `features.*`
- **WHEN** the application starts
- **THEN** Spring assembly injects implementations into registry
- **AND** `core.pagemodel.internal` has no compile-time dependency on feature packages.
