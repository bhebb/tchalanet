# web-e2e-auth-phase1 Spec Delta

Phase 1 of `web-e2e-critical-flows-v1`: **authentication, role dispatch and
context override** — the login/session/redirection backbone, done first, before
the per-screen config/general flows (Phase 2). UI-observable behavior only (the
parent boundary applies: no backend business assertions).

Grounded on `@tch/core/auth` (`libs/core/auth`): `LoginPage` (`/login` in each
app), `AuthRedirectService.navigateAfterLogin`, `authGuard`,
`spaceDispatchGuard` (`/app` entry, dispatches on `session.entryRoute` / role),
`roleGuard(role)`; cross-app redirect via `location.assign()` and
`TchRuntimeConfig.portalBaseUrls`.

## ADDED Requirements

### Requirement: Phase 1 Boundary

Phase 1 SHALL cover only authentication, role-based dispatch and context
override, asserted through the browser.

#### Scenario: Phase 1 scope is auth and context only

- **WHEN** a Phase 1 spec is written
- **THEN** it SHALL assert login, session, redirection, guards, or the
  tenant/seller-terminal context override
- **AND** it SHALL NOT assert backend business rules (limits, payout,
  idempotency, ProblemDetail bodies, RLS)
- **AND** config/general screens (setup, limits editing, POS sale, reporting)
  SHALL be deferred to Phase 2.

### Requirement: Public Anonymous Baseline

The public portal SHALL be reachable with no session and offer the login entry.

#### Scenario: Anonymous visitor lands on public shell

- **WHEN** an unauthenticated browser opens `/` (public-portal, :4301)
- **THEN** `TchPublicShellComponent` SHALL render public content
- **AND** the app SHALL send no `Authorization` header (session unauthenticated)
- **AND** the header "Connexion" action SHALL navigate to `/login`.

### Requirement: Real UI Login And Role Dispatch

A real UI login SHALL authenticate the user and dispatch them to the space
matching their role.

#### Scenario: Tenant admin logs in via the UI

- **WHEN** an `admin` (TENANT_ADMIN) submits valid credentials on `/login`
- **THEN** `navigateAfterLogin` SHALL land the authenticated admin shell under
  `/app/admin` (admin-portal, :4302)
- **AND** the tenant-admin navigation SHALL render.

#### Scenario: Super admin logs in via the UI

- **WHEN** a `super_admin` submits valid credentials on `/login`
- **THEN** dispatch SHALL land the authenticated platform shell under
  `/app/platform` (platform-portal, :4303).

#### Scenario: Invalid credentials

- **WHEN** invalid credentials are submitted
- **THEN** an inline error SHALL show and no navigation SHALL occur
- **AND** the submit button SHALL not stay disabled.

### Requirement: Route Guards And Redirection

Guards SHALL redirect based on session and role.

#### Scenario: Unauthenticated access to a private route

- **WHEN** an unauthenticated browser opens a guarded `/app/**` route
- **THEN** `authGuard` SHALL redirect to `/login`.

#### Scenario: Wrong-role access

- **WHEN** a TENANT_ADMIN opens a `/app/platform/**` route (or a CASHIER opens a
  tenant-admin-only route)
- **THEN** `roleGuard` SHALL redirect / show forbidden, not render the screen.

#### Scenario: Space dispatch honors entryRoute

- **WHEN** an authenticated user hits `/app`
- **THEN** `spaceDispatchGuard` SHALL route to `session.entryRoute`
- **AND** an un-activated TENANT_ADMIN SHALL be routed to `/account/activation`.

### Requirement: Super Admin Acting Within A Tenant

A super admin SHALL be able to operate inside a tenant's context from the
platform portal, and the browser SHALL show the tenant scope.

#### Scenario: Super admin enters a tenant

- **WHEN** a `super_admin` selects/acts on a tenant from the platform portal
- **THEN** the tenant-scoped screen SHALL render for that tenant (the client
  operates through the `asTenantAdmin` / `X-Tenant-Id` override)
- **AND** the UI SHALL indicate the active tenant context.

#### Scenario: Super admin leaves the tenant context

- **WHEN** the super admin exits the tenant context
- **THEN** the platform scope SHALL be restored (no residual tenant scoping in
  the UI).

### Requirement: Admin Acting On A Seller Terminal

A tenant admin SHALL be able to open and act on a specific seller terminal, and
the browser SHALL show that terminal's context.

#### Scenario: Admin opens a seller terminal

- **WHEN** an `admin` opens a seller terminal from `admin-portal`
  (`features/seller-terminals`)
- **THEN** that terminal's context screen SHALL render (identity / per-terminal
  view)
- **AND** the terminal shown SHALL be the one selected, not another.

#### Scenario: Admin cannot reach a terminal outside its tenant

- **WHEN** an admin targets a seller terminal id not in its tenant
- **THEN** the UI SHALL show a not-found / forbidden state, not another tenant's
  terminal.
