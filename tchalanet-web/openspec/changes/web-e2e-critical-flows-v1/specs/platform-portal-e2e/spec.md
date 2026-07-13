# Spec: Platform Portal E2E

Base URL :4303. Role: `superAdmin`. UI-observable only — provisioning
correctness stays in Python E2E.

## ADDED Requirements

### Requirement: Super-admin login dispatches to the platform space

A SUPER_ADMIN signing in SHALL be routed to `/app/platform/dashboard`.

#### Scenario: Super-admin lands on platform dashboard

- **WHEN** a SUPER_ADMIN completes sign-in
- **THEN** the browser lands on `/app/platform/dashboard`.

### Requirement: Platform space is role-guarded

A non-super-admin identity SHALL be blocked from `/app/platform/*` by the role
guard, landing on `ForbiddenPage` or redirected.

#### Scenario: Non-super-admin blocked

- **WHEN** a non-super-admin navigates to an `/app/platform/*` route
- **THEN** the browser shows forbidden or is redirected away.

### Requirement: Tenants list renders

`/app/platform/tenants` SHALL render the tenants table with pagination controls,
or an empty-state when there are none.

#### Scenario: Tenants table present

- **WHEN** the super-admin opens `/app/platform/tenants`
- **THEN** the tenants table with pagination is visible (or a clear empty-state).

### Requirement: Tenant onboarding form validates before submit

`/app/platform/tenants/onboarding` SHALL render, and submitting with a missing
required field SHALL show inline validation and SHALL NOT issue the provisioning
request. Provisioning correctness is owned by Python E2E.

#### Scenario: Missing required field blocks submit

- **WHEN** the super-admin submits the onboarding form with a required field
  empty
- **THEN** inline validation is shown and no provisioning request is sent.
