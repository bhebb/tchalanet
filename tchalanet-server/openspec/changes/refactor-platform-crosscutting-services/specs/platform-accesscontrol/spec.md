# Spec — platform.accesscontrol

## ADDED Requirements

### Requirement: Public AccessControl API

`platform.accesscontrol` SHALL expose authorization decisions through `platform.accesscontrol.api`
and SHALL hide implementation under `platform.accesscontrol.internal`.

#### Scenario: Core checks a reusable command permission

- **GIVEN** a command handler can be invoked outside HTTP method security
- **WHEN** the handler needs an authorization decision
- **THEN** it SHALL call `AccessControlApi`
- **AND** it SHALL NOT import `platform.accesscontrol.internal.*`

#### Scenario: Non-owner imports internal accesscontrol

- **GIVEN** a class outside `platform.accesscontrol`
- **WHEN** it imports `platform.accesscontrol.internal.*`
- **THEN** architecture tests SHALL fail.

### Requirement: Deny-safe permission evaluation

Permission evaluation SHALL deny access when required security facts are missing or ambiguous.

#### Scenario: Missing actor

- **GIVEN** a protected endpoint requires a permission
- **WHEN** no authenticated actor can be resolved
- **THEN** accesscontrol SHALL deny the request
- **AND** it SHALL NOT fallback to a permissive role.

#### Scenario: Missing tenant for tenant-scoped permission

- **GIVEN** a tenant-scoped permission is evaluated
- **WHEN** no effective tenant can be resolved
- **THEN** accesscontrol SHALL deny the request
- **AND** it SHALL NOT trust tenant ids from request bodies.

### Requirement: Method security delegation

HTTP controllers SHALL declare authorization requirements through method security annotations.

#### Scenario: Protected write endpoint

- **GIVEN** an admin or tenant write endpoint
- **WHEN** the endpoint is invoked
- **THEN** `@PreAuthorize` or an equivalent meta-annotation SHALL express the permission requirement
- **AND** the controller SHALL NOT perform manual role/permission `if` checks.

#### Scenario: Permission evaluator checks permission

- **GIVEN** `hasPermission(...)` is evaluated
- **WHEN** Spring invokes `TchPermissionEvaluator`
- **THEN** the evaluator SHALL resolve actor and tenant from canonical context/authentication
- **AND** delegate to accesscontrol service/API
- **AND** return `false` for deny.

### Requirement: AccessControl does not own business invariants

`platform.accesscontrol` SHALL decide whether an actor may attempt an action, not whether the target
resource is in a valid business state.

#### Scenario: Payout approval

- **GIVEN** an actor has `PAYOUT_APPROVE`
- **WHEN** the payout is not approvable according to payout domain state
- **THEN** `core.payout` SHALL reject the command
- **AND** `platform.accesscontrol` SHALL NOT contain payout state transition logic.

#### Scenario: POS sale

- **GIVEN** an actor has permission to sell
- **WHEN** the terminal is blocked or the sales session is closed
- **THEN** core terminal/session/sales validators SHALL reject the action
- **AND** accesscontrol SHALL NOT validate terminal or session state.

### Requirement: Tenant-safe permission persistence

Tenant-scoped access-control rows SHALL be tenant-safe and RLS-compatible.

#### Scenario: Tenant admin loads user permissions

- **GIVEN** tenant admin requests permissions for a tenant user
- **WHEN** accesscontrol reads role assignments and grants
- **THEN** RLS SHALL restrict rows to the effective tenant
- **AND** client-provided tenant ids SHALL NOT be the source of truth.

### Requirement: AccessControl write endpoints are audited

All role/permission/assignment write endpoints SHALL be functionally audited.

#### Scenario: Admin grants permission

- **GIVEN** an admin grants a permission to a role or user
- **WHEN** the operation succeeds
- **THEN** a functional audit entry SHALL record actor, tenant, target, permission, and outcome.

#### Scenario: Permission write denied

- **GIVEN** an actor attempts an unauthorized permission write
- **WHEN** access is denied
- **THEN** denied audit SHALL be recorded when audit policy requires it
- **AND** the denial SHALL remain the client-visible result.
