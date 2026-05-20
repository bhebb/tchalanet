# system-execution-scope Specification

## Purpose
TBD - created by archiving change refactor-operational-context-identity. Update Purpose after archive.
## Requirements
### Requirement: SYSTEM is internal execution scope

`SYSTEM` MAY exist as an internal execution scope for scheduler, batch, startup, retry, and outbox flows, but it SHALL NOT be a public HTTP scope by default.

#### Scenario: Public system route is proposed

- **GIVEN** a controller maps a public route under `/api/v1/system/**`
- **WHEN** the route is reviewed
- **THEN** it SHALL require an ADR-approved exception

### Requirement: Tenant-scoped system work binds context explicitly

Tenant-scoped system work SHALL bind tenant context explicitly before database access.

#### Scenario: Batch job runs tenant-scoped work

- **GIVEN** a batch job processes tenant-scoped data
- **WHEN** it accesses tenant tables
- **THEN** it SHALL bind an explicit tenant request context before DB access
- **AND** audit metadata SHALL record a system actor with job or execution id

### Requirement: Global system work stays global

Global system work SHALL access only global or non-RLS tables unless an explicit job policy permits tenant-scoped access.

#### Scenario: Retry listener runs outside HTTP

- **GIVEN** an event listener or retry flow runs without ambient HTTP ThreadLocal context
- **WHEN** it needs tenant-scoped state
- **THEN** it SHALL restore or bind context from event metadata or job parameters

### Requirement: Super-Admin Tenant Override Is Per Request

Super-admin tenant override SHALL be explicit and scoped to a single request.

The override SHALL use `X-Tch-Tenant-Override` as the target tenant input and `X-Tch-Override-Reason` as the reason input.

#### Scenario: Super-admin provides override

- **GIVEN** a super-admin calls a tenant-scoped operation from platform scope
- **WHEN** the request includes `X-Tch-Tenant-Override` and a valid `X-Tch-Override-Reason`
- **THEN** the request may resolve an effective tenant for that request
- **AND** the override does not persist beyond the request.

#### Scenario: Override reason is missing

- **GIVEN** a super-admin calls a sensitive tenant-scoped operation with tenant override
- **WHEN** `X-Tch-Override-Reason` is missing or blank
- **THEN** the operation is rejected before domain mutation.

### Requirement: Super-Admin Override Requires Permission

Super-admin tenant override SHALL require the dedicated permission `platform.tenant.override`.

#### Scenario: Super-admin lacks override permission

- **GIVEN** an authenticated super-admin attempts tenant override
- **WHEN** the actor lacks `platform.tenant.override`
- **THEN** the override is rejected
- **AND** no tenant-scoped mutation is executed.

### Requirement: Super-Admin Override Is Auditable

Every request with active super-admin tenant override SHALL produce audit data containing actor id, target tenant id, reason, request id and correlation id.

#### Scenario: Override is accepted

- **GIVEN** a super-admin override is accepted for a request
- **WHEN** the request is processed
- **THEN** audit data records the override usage
- **AND** the target use case still enforces its domain invariants.

### Requirement: System Actor Does Not Imply Tenant Override

System execution scope SHALL NOT imply super-admin tenant override.

#### Scenario: System replay executes offline sync

- **GIVEN** a system process replays offline sales
- **WHEN** the process executes under system scope
- **THEN** it does not gain super-admin override semantics
- **AND** any tenant context must be resolved from the sync payload or owning workflow policy.

