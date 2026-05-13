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

