# common.context Specification

## Purpose
TBD - created by archiving change refactor-operational-context-identity. Update Purpose after archive.
## Requirements
### Requirement: Request context ownership

`common.context` SHALL own request/runtime context primitives, including `TchRequestContext`, `TchContext`, `TchContextResolver`, request/thread binding, and batch/startup/system binding helpers.

#### Scenario: Common context compiles independently

- **GIVEN** `common.context` owns runtime context primitives
- **WHEN** `tchalanet-common` is compiled
- **THEN** `common.context` SHALL NOT depend on `platform`, `core`, `catalog`, or `features`

### Requirement: Current context injection remains available

Controllers SHALL still be able to inject the current request context through the project-standard `@CurrentContext TchRequestContext` mechanism.

#### Scenario: Controller receives current context

- **GIVEN** an HTTP request has passed through the canonical context filter
- **WHEN** a controller declares `@CurrentContext TchRequestContext`
- **THEN** the MVC argument resolver SHALL provide the bound request context

### Requirement: Common context contains no business ownership

`common.context` SHALL NOT contain user profile persistence, tenant membership persistence, permission decisions, terminal/outlet/session validation, or business rules.

#### Scenario: User membership data is needed

- **GIVEN** a flow needs tenant membership facts for the current actor
- **WHEN** the flow resolves identity
- **THEN** it SHALL use `platform.identity`
- **AND** it SHALL NOT add membership persistence to `common.context`

### Requirement: Runtime Context Package Ownership

`common.context` SHALL own only neutral runtime request context types and helpers.

It SHALL be organized around `common.context`, `common.context.web`, `common.context.tenant`, `common.context.system` and `common.context.operational`.

#### Scenario: Request context is produced for HTTP

- **GIVEN** an HTTP request enters the backend
- **WHEN** request context is created
- **THEN** `TchContextFilter` is the canonical producer
- **AND** the produced context contains request metadata, API scope, actor facts, tenant facts when applicable and optional operational context.

#### Scenario: Runtime context remains neutral

- **GIVEN** a class in `common.context`
- **WHEN** dependencies are inspected
- **THEN** it does not depend on platform, core, catalog or feature packages.

### Requirement: Single HTTP Context Filter

The backend SHALL NOT introduce a separate `OperationalContextFilter`.

Operational context parsing and attachment SHALL happen inside the existing HTTP context production pipeline.

#### Scenario: Operational headers are present

- **GIVEN** an HTTP request includes operational context headers
- **WHEN** `TchContextFilter` builds the request context
- **THEN** it may attach a parsed operational context
- **AND** no second operational context filter participates in the request.

### Requirement: Operational Package Is Neutral

`common.context.operational` SHALL contain only neutral operational context types, headers, parser inputs, parser results, exceptions and helper contracts.

It SHALL NOT contain DB lookup, permission lookup, terminal validation, outlet validation, session validation or business rules.

#### Scenario: Operational parser reads headers

- **GIVEN** operational headers on a request
- **WHEN** the parser runs
- **THEN** it parses, normalizes and marks trust/source
- **AND** it does not call repositories, `CommandBus`, `QueryBus`, platform APIs, core APIs, catalog APIs or feature APIs.

#### Scenario: Transactional resources require validation

- **GIVEN** a parsed operational context contains terminal, outlet and sales session ids
- **WHEN** the context is attached to `TchRequestContext`
- **THEN** those ids are treated as inputs
- **AND** they are not treated as proof that the resources exist or are valid.

### Requirement: Tenant Lookup Interface Boundary

`TenantContextLookup` SHALL remain an interface in `common.context.tenant`.

The platform tenant configuration component SHALL provide the implementation.

#### Scenario: Common resolves tenant through interface

- **GIVEN** context creation needs tenant details for an already extracted tenant id or code
- **WHEN** the resolver needs tenant metadata
- **THEN** it uses `TenantContextLookup`
- **AND** common does not import the platform tenant configuration implementation.

