# security Specification

## Purpose

TBD - created by archiving change security-auth-accesscontrol-refactor. Update Purpose after archive.

## Requirements

### Requirement: Canonical security flow documentation

Tchalanet MUST maintain canonical security flow documentation under:

```text
tchalanet-docs/docs/01-architecture/flows/
```

#### Scenario: Authentication flow documentation exists

- **GIVEN** a contributor or agent needs to change authentication/context/RLS behavior
- **WHEN** they inspect security flow documentation
- **THEN** `authentication-flow.md` describes the canonical JWT, bootstrap, context and RLS pipeline

#### Scenario: Permission flow documentation exists

- **GIVEN** a contributor or agent needs to change authorization behavior
- **WHEN** they inspect security flow documentation
- **THEN** `permission-flow.md` describes the canonical permission evaluation pipeline

---

### Requirement: Authenticated users are bootstrapped locally

For authenticated protected requests, the server MUST guarantee a local `app_user` exists before building `TchRequestContext`.

#### Scenario: Authenticated user has no local app_user

- **GIVEN** a valid JWT for a user not yet present in `app_user`
- **WHEN** the user calls a protected endpoint
- **THEN** the server creates or synchronizes `app_user`
- **AND** `TchRequestContext.appUserId` is available downstream

#### Scenario: Suspended user calls protected endpoint

- **GIVEN** a valid JWT for a suspended local user
- **WHEN** the user calls a protected endpoint
- **THEN** the request is rejected with 403 before controller execution

---

### Requirement: Permissions are evaluated by core.accesscontrol

Fine-grained authorization decisions MUST be delegated to `core.accesscontrol`.

#### Scenario: Controller declares required permission

- **GIVEN** a protected controller method
- **WHEN** it requires a permission
- **THEN** it declares the requirement via Spring Method Security
- **AND** it does not implement manual role or permission checks

#### Scenario: Permission evaluator checks permission

- **GIVEN** Spring Method Security invokes `hasPermission`
- **WHEN** `TchPermissionEvaluator` evaluates it
- **THEN** it obtains tenant/user from `TchRequestContext`
- **AND** dispatches `CheckUserPermissionsQuery` through `QueryBus`
- **AND** returns a boolean decision

---

### Requirement: Super-admin overrides are controlled

Sensitive tenant/deleted-visibility override headers MUST be restricted to `SUPER_ADMIN`.

#### Scenario: Non-super-admin sends override header

- **GIVEN** a non-super-admin authenticated request
- **WHEN** it sends `X-Tenant-Id` or `X-Deleted-Visibility`
- **THEN** the server rejects the request with 403

#### Scenario: Super-admin sends override header

- **GIVEN** a super-admin authenticated request
- **WHEN** it sends `X-Tenant-Id`
- **THEN** the effective tenant is overridden in `TchRequestContext`
- **AND** RLS receives the effective tenant

---

### Requirement: RLS receives full request context

RLS-aware database access MUST receive the effective request context through session variables.

#### Scenario: Tenant-scoped request opens a DB connection

- **GIVEN** a tenant-scoped request with a resolved tenant
- **WHEN** the server opens a JDBC connection
- **THEN** it sets `app.current_tenant`
- **AND** it sets `app.deleted_visibility`
- **AND** it sets `app.api_scope`
- **AND** it sets `app.is_super_admin`
