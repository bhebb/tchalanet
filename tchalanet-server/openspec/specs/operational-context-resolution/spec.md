# operational-context-resolution Specification

## Purpose
TBD - created by archiving change refactor-operational-context-identity. Update Purpose after archive.
## Requirements
### Requirement: Seller operational context resolution

Seller operational context resolution SHALL validate runtime operation frames before POS-sensitive actions such as sell, offline sale sync, or payout execution.

#### Scenario: Seller context resolves successfully

- **GIVEN** a request context with tenant and actor facts
- **AND** the actor is allowed to operate on the terminal/outlet/session frame
- **WHEN** seller operational context is resolved
- **THEN** the resolver SHALL return a value object or result containing tenant id, actor user id, terminal id, outlet id, and session id when required
- **AND** it SHALL NOT return JPA entities

### Requirement: Resolver composition uses public APIs

Operational context resolution SHALL compose facts through public APIs only.

#### Scenario: Resolver reads cross-module facts

- **GIVEN** the resolver needs identity, permissions, terminal, outlet, or session state
- **WHEN** it queries those facts
- **THEN** it SHALL use `platform.identity.api`, `platform.accesscontrol.api`, `core.terminal.api`, `core.outlet.api`, and `core.session.api`
- **AND** it SHALL NOT import internal packages from those modules

### Requirement: Resolver does not mutate business state

Operational context resolution SHALL validate the operation frame without mutating business state or bypassing RLS.

#### Scenario: Invalid session frame

- **GIVEN** a seller action requires an open session
- **AND** the resolved session is missing or closed
- **WHEN** seller operational context is resolved
- **THEN** the resolver SHALL fail with a problem-compatible application exception
- **AND** no sales mutation SHALL run

### Requirement: Business invariants remain in core

Business use cases SHALL remain responsible for business invariants after operational context has been resolved.

#### Scenario: Sell command executes after context resolution

- **GIVEN** seller operational context has been resolved
- **WHEN** the sell command executes
- **THEN** core sales SHALL still enforce sales-specific invariants
- **AND** the resolver SHALL NOT move those invariants into platform identity

