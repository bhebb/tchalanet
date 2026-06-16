# common.context Delta Specification

## ADDED Requirements

### Requirement: Canonical context is assembled after access resolution

Authenticated HTTP requests SHALL bind the canonical `TchRequestContext` only after application
identity, effective tenant, roles, and permissions have been resolved.

#### Scenario: Tenant-scoped request reaches database access

- **GIVEN** a technically authenticated tenant-scoped request
- **WHEN** the request reaches tenant-scoped database access
- **THEN** canonical context SHALL already contain the resolved actor and effective tenant
- **AND** RLS session variables SHALL derive from that canonical context

### Requirement: Common consumes only neutral resolved-access facts

`common.context` SHALL consume neutral context input owned by common and SHALL NOT import identity or
access-control implementation/API types.

#### Scenario: Access snapshot is converted for context assembly

- **GIVEN** `platform.accesscontrol` resolved effective access
- **WHEN** the canonical context is assembled
- **THEN** the app/filter pipeline SHALL provide neutral resolved-access facts to common
- **AND** `common.context` SHALL NOT import `platform.accesscontrol` or `platform.identity`

### Requirement: Resolved access is temporary pipeline input

`ResolvedAccessContext` SHALL be a neutral temporary HTTP pipeline input consumed by
`TchContextFilter`; it SHALL NOT become a second canonical runtime context.

#### Scenario: Canonical context has been bound

- **GIVEN** `TchContextFilter` consumed `ResolvedAccessContext`
- **WHEN** downstream application code needs actor, role, permission, or tenant facts
- **THEN** it SHALL read `TchRequestContext`
- **AND** it SHALL NOT read resolved-access request attributes directly

### Requirement: Runtime bindings are cleared

Canonical request, ThreadLocal, MDC, and RLS-related runtime bindings SHALL be cleared after request
or explicit system-context execution.

#### Scenario: Request completes

- **GIVEN** a request bound a canonical context
- **WHEN** request processing completes or fails
- **THEN** runtime bindings SHALL be cleared in `finally`
- **AND** pooled connections SHALL not leak tenant context

### Requirement: Non-HTTP execution creates explicit system context

Batch and scheduler entry points SHALL create explicit platform or tenant system context before
database access and SHALL NOT depend on HTTP identity/access filters.

#### Scenario: Tenant batch starts

- **GIVEN** a tenant-scoped batch job with an approved tenant parameter
- **WHEN** the job begins database work
- **THEN** it SHALL bind an explicit system actor and effective tenant context
- **AND** clear the context in `finally`

#### Scenario: Batch authorities are assigned

- **GIVEN** a batch or scheduler job family
- **WHEN** its explicit system context is created
- **THEN** its authorities SHALL be named and allowlisted for that job family
- **AND** it SHALL NOT receive an implicit unrestricted system authority set

### Requirement: Context binding consumes resolved access without re-deciding it

For provider-neutral protected requests, the canonical context binder SHALL build `TchRequestContext`
from the resolved-access facts and SHALL hydrate tenant **metadata only**. It SHALL NOT re-resolve the
effective tenant from request headers, nor decide membership or tenant override.

#### Scenario: Protected request binds context from resolved access

- **GIVEN** a request carrying resolved-access facts with an effective tenant
- **WHEN** the canonical context binder runs
- **THEN** it SHALL take the effective tenant from the resolved-access facts
- **AND** it SHALL hydrate only tenant metadata (code, identifier, timezone, currency)
- **AND** it SHALL NOT re-read a tenant header to choose the tenant

#### Scenario: Hydration target tenant is unknown

- **GIVEN** a resolved effective tenant identifier
- **WHEN** tenant metadata hydration cannot find that tenant
- **THEN** the request SHALL be denied before controller execution
- **AND** no canonical context SHALL be bound
