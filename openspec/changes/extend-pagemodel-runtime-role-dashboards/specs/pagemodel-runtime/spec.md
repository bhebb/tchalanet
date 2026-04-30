# Capability: PageModel Runtime Dashboards

## Status

Draft

## ADDED Requirements

### Requirement: Runtime PageModel BFF ownership

The system SHALL use `features.pagemodel` as the runtime BFF layer for PageModel responses.

The runtime BFF SHALL:

- resolve the effective PageModel via `core.pagemodel` queries;
- enrich dynamic widgets through `PageModelDynamicProvider` implementations;
- return UI-friendly PageModel responses;
- avoid repositories and JPA entities;
- avoid business invariants and money-critical calculations.

#### Scenario: Public PageModel resolution

- **Given** a published or fallback system template for `public.home`
- **When** a client requests the public home PageModel
- **Then** the BFF resolves the base PageModel
- **And** enriches dynamic public widgets
- **And** returns a PageModel response with `dynamic.widgets`.

#### Scenario: Provider failure isolation

- **Given** a PageModel contains multiple dynamic widgets
- **And** one provider fails
- **When** the BFF resolves the PageModel
- **Then** the failing widget is reported in `dynamic.errors`
- **And** other widgets still return their dynamic payloads
- **And** the full PageModel response is not discarded.

---

### Requirement: Dashboard resolution uses request context

The system SHALL resolve dashboard PageModel type from `TchRequestContext`.

Dashboard resolution SHALL NOT use role alone.

Dashboard resolution SHALL consider:

- API scope;
- current role;
- tenant override state;
- effective tenant.

#### Scenario: Platform super admin dashboard

- **Given** the current request scope is `PLATFORM`
- **And** the current role is `SUPER_ADMIN`
- **When** the dashboard PageModel is requested
- **Then** the system resolves `platform.dashboard.super_admin`.

#### Scenario: Super admin tenant override dashboard

- **Given** the current request scope is `TENANT`
- **And** the current role is `SUPER_ADMIN`
- **And** tenant override is active
- **When** the dashboard PageModel is requested
- **Then** the system resolves `private.dashboard.tenant_admin`.

#### Scenario: Tenant admin dashboard

- **Given** the current request scope is `TENANT`
- **And** the current role is `TENANT_ADMIN`
- **When** the dashboard PageModel is requested
- **Then** the system resolves `private.dashboard.tenant_admin`.

#### Scenario: Cashier dashboard

- **Given** the current request scope is `TENANT`
- **And** the current role is `CASHIER`
- **When** the dashboard PageModel is requested
- **Then** the system resolves `private.dashboard.cashier`.

#### Scenario: Unsupported tenant dashboard role

- **Given** the current request scope is `TENANT`
- **And** the current role cannot access a dashboard
- **When** the dashboard PageModel is requested
- **Then** the system rejects the request with a forbidden-style error.

---

### Requirement: Tenant runtime uses effective context tenant

The tenant dashboard runtime SHALL use the effective tenant from `TchRequestContext`.

Tenant runtime SHALL NOT accept tenant id from request body or ordinary query parameters.

#### Scenario: Tenant dashboard uses context tenant

- **Given** an authenticated tenant request
- **And** the context contains an effective tenant
- **When** the dashboard is resolved
- **Then** the PageModel resolution uses the context tenant.

#### Scenario: Missing tenant context

- **Given** a tenant dashboard request
- **And** no request context is available
- **When** the dashboard is resolved
- **Then** the system fails fast instead of resolving a tenant dashboard without context.

---

### Requirement: Platform runtime resolves platform dashboard

The platform dashboard runtime SHALL resolve platform PageModels without tenant runtime override.

#### Scenario: Platform dashboard request

- **Given** a SUPER_ADMIN authenticated request in platform scope
- **When** `/platform/pagemodel/dashboard` is requested
- **Then** the BFF returns the `platform.dashboard.super_admin` PageModel.

---

### Requirement: Public home dynamic sources

The `public.home` template SHALL use canonical dynamic sources.

Required public widget sources:

- `public.hero`
- `public.draws.today`
- `public.features`
- `public.news.latest`
- `public.tchala.featured`
- `public.testimonials`
- `public.plans`

The `home.check_ticket` widget SHALL remain static and route to `/verifier`.

#### Scenario: Public dynamic providers resolve

- **Given** `public.home` contains dynamic widgets with canonical public sources
- **When** the BFF resolves the page
- **Then** each dynamic widget is matched to a public provider
- **And** `home.check_ticket` is not passed to a provider.

---

### Requirement: Cashier dashboard dynamic sources

The `private.dashboard.cashier` template SHALL use canonical cashier sources.

Required cashier widget sources:

- `cashier.overview`
- `cashier.quick_sale`
- `cashier.open_draws`
- `cashier.recent_tickets`
- `cashier.session`

The cashier dashboard SHALL include an `open_draws` row and `dashboard.cashier.open_draws` widget.

#### Scenario: Cashier dynamic providers resolve

- **Given** `private.dashboard.cashier` contains the cashier dynamic widgets
- **When** a CASHIER requests the tenant dashboard
- **Then** each dynamic widget is matched to a cashier provider
- **And** the response contains dynamic payloads for overview, quick sale, open draws, recent tickets, and session.

---

### Requirement: Tenant admin dashboard template

The system SHALL provide a system template `private.dashboard.tenant_admin`.

The template SHALL include:

- overview;
- sales;
- draws;
- outlets;
- users;
- limits;
- alerts.

Required tenant admin widget sources:

- `tenant_admin.overview`
- `tenant_admin.sales_summary`
- `tenant_admin.draws_today`
- `tenant_admin.outlets`
- `tenant_admin.users`
- `tenant_admin.limits`
- `tenant_admin.alerts`

#### Scenario: Tenant admin dynamic providers resolve

- **Given** `private.dashboard.tenant_admin` contains tenant admin dynamic widgets
- **When** a TENANT_ADMIN requests the tenant dashboard
- **Then** each dynamic widget is matched to a tenant admin provider.

#### Scenario: Super admin tenant override uses tenant admin providers

- **Given** a SUPER_ADMIN is operating in TENANT scope with tenant override
- **When** the dashboard is resolved
- **Then** the system uses `private.dashboard.tenant_admin`
- **And** the tenant admin dynamic providers populate the widgets.

---

### Requirement: Platform super admin dashboard template

The system SHALL provide a system template `platform.dashboard.super_admin`.

The template SHALL include:

- overview;
- tenants;
- services;
- feature flags;
- jobs;
- audit;
- release notes.

Required platform widget sources:

- `platform.overview`
- `platform.tenants`
- `platform.services.health`
- `platform.flags`
- `platform.jobs`
- `platform.audit`
- `platform.release_notes`

#### Scenario: Platform dynamic providers resolve

- **Given** `platform.dashboard.super_admin` contains platform dynamic widgets
- **When** a SUPER_ADMIN requests the platform dashboard
- **Then** each dynamic widget is matched to a platform provider.

---

### Requirement: Dynamic provider matching

The dynamic resolver SHALL match providers by:

- logical page id;
- widget type;
- binding source.

If multiple providers match, the resolver SHALL choose deterministically by provider order.

#### Scenario: No provider found

- **Given** a dynamic widget has no matching provider
- **When** the dynamic resolver processes the widget
- **Then** the resolver adds a `NO_PROVIDER` error for that widget
- **And** continues processing remaining widgets.

#### Scenario: Provider throws exception

- **Given** a matching provider throws an exception
- **When** the dynamic resolver processes the widget
- **Then** the resolver logs the internal error
- **And** returns a sanitized `PROVIDER_ERROR` for the widget
- **And** continues processing remaining widgets.

---

### Requirement: Runtime response preserves static template

The runtime response SHALL preserve the base PageModel document and attach dynamic payloads separately.

#### Scenario: Widget config and dynamic payload are separate

- **Given** a widget has static props and a dynamic binding
- **When** the BFF returns the PageModel response
- **Then** the original widget config remains in the PageModel document
- **And** dynamic data is returned under `dynamic.widgets[widgetId]`.

---

### Requirement: PageModel admin is out of MVP scope

This change SHALL NOT require PageModel or PageTemplate admin UI.

#### Scenario: Runtime can use system templates

- **Given** system templates are available
- **When** the runtime BFF resolves a supported page or dashboard
- **Then** the page can be served without implementing PageModel/template admin screens.
