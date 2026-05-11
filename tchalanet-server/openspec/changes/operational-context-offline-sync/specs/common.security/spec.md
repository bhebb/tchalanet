# common.security spec delta

## ADDED Requirements

### Requirement: Single canonical context producer

`TchContextFilter` SHALL be the single canonical producer of HTTP request context, chaining `contextFactory → tenantContextResolver → actorContextResolver → operationalContextResolver → contextBinder.bind`.

#### Scenario: Pipeline order

- **GIVEN** an authenticated HTTP request
- **WHEN** `TchContextFilter` runs
- **THEN** it SHALL invoke the resolvers in order: factory, tenant, actor, operational, binder
- **AND** no other servlet filter SHALL rebind the request context

#### Scenario: OperationalContextFilter removed

- **GIVEN** the production Spring context
- **WHEN** registered filters are inspected
- **THEN** no bean named `OperationalContextFilter` SHALL exist
- **AND** no `FilterRegistrationBean` SHALL register such a filter

### Requirement: OperationalContextResolver contract

The system SHALL define `OperationalContextResolver.resolve(TenantId, UserId, Set<TchRole>, HttpServletRequest)` returning `Optional<OperationalRequestContext>`. Its default implementation SHALL delegate to `GetCurrentOperationalContextQuery` via the `QueryBus`.

#### Scenario: Cashier auto-resolution

- **GIVEN** an authenticated user with role `CASHIER` or `OPERATOR`
- **WHEN** `OperationalContextResolver.resolve` runs
- **THEN** it SHALL attempt automatic resolution from device binding or signed headers

#### Scenario: Admin without selection

- **GIVEN** an authenticated user with role `TENANT_ADMIN` or `SUPER_ADMIN` and no active admin selection
- **WHEN** `OperationalContextResolver.resolve` runs
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: System role

- **GIVEN** an authenticated `SYSTEM` caller
- **WHEN** `OperationalContextResolver.resolve` runs
- **THEN** it SHALL return `Optional.empty()`

## REMOVED Requirements

### Requirement: OperationalContextFilter

**Reason**: Two canonical producers of request context create ordering and ownership ambiguity. Operational context resolution moves into the `TchContextFilter` chain as the `OperationalContextResolver` step.

**Migration**: Delete the `OperationalContextFilter` class and any Spring registration. Existing callers that read the operational context via `TchContextHolder` continue to work because the binder now provides the same `OperationalRequestContext`.
