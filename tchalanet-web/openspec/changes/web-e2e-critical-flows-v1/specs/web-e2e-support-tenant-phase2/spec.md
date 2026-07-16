# web-e2e-support-tenant-phase2 Spec Delta

Phase 2 of `web-e2e-critical-flows-v1`: the **platform support mode** — a
super-admin opens the support-tenant screen and starts admin access on a tenant.
Builds on the Phase 1 harness (LoginPage fixture). UI-observable only (parent
boundary applies: no backend business assertions).

Grounded on `apps/platform-portal/src/app/features/support-tenant`
(`tch-platform-support-tenant-page`, route `/app/platform/support-tenant`) and
the shared `tch-start-tenant-admin-access-dialog`.

## ADDED Requirements

### Requirement: Support-Tenant Screen Is Guarded

The support-tenant screen SHALL require a super-admin session.

#### Scenario: Unauthenticated access

- **WHEN** an unauthenticated browser opens `/app/platform/support-tenant`
- **THEN** `authGuard` SHALL redirect to `/login`.

### Requirement: Super Admin Opens The Support-Tenant Screen

A super admin SHALL reach the support-tenant screen and see the tenant list or
an empty state.

#### Scenario: Screen renders

- **WHEN** a `super_admin` navigates to `/app/platform/support-tenant`
- **THEN** `tch-platform-support-tenant-page` SHALL render
- **AND** it SHALL show the tenant table or the empty-state (both valid).

### Requirement: Start Tenant Admin Access From Support Mode

From the support-tenant list, a super admin SHALL open the start-access dialog
for a tenant.

#### Scenario: Open the start-access dialog

- **WHEN** the super admin activates the "Mode support admin" action on a tenant row
- **THEN** the `tch-start-tenant-admin-access-dialog` SHALL open
- **AND** its confirm/submit control SHALL be visible.

#### Scenario: No tenants to support

- **WHEN** the support list has no rows
- **THEN** the empty-state SHALL render and no start-access action SHALL be present.
