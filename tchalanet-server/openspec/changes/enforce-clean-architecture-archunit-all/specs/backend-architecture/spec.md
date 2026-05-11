# backend-architecture Specification Delta

## ADDED Requirements

### Requirement: Core modules MUST follow clean architecture package boundaries

Every core domain SHALL be organized around domain, application, ports, and infra.

Canonical package structure:

```text
core.<domain>
  domain
    model
    service
    event
    exception
  application
    command.model
    command.handler
    query.model
    query.handler
    port.out
    service
    exception
  infra
    web
    persistence
    event
    batch
    scheduler
    cache
    config
```

#### Scenario: A core domain adds a new command

- **WHEN** a new command is created
- **THEN** the command model lives in `core.<domain>.application.command.model`
- **AND** the handler lives in `core.<domain>.application.command.handler`
- **AND** the handler depends on domain and `application.port.out`, not infra.

### Requirement: Domain layer MUST remain pure

The domain layer SHALL NOT depend on Spring, JPA, web, persistence, cache, batch, scheduler, application, or infra packages.

#### Scenario: A domain service needs data from the database

- **WHEN** a rule needs database data
- **THEN** the application handler loads the data first
- **AND** passes a snapshot/value object into the domain service
- **AND** the domain service remains pure.

### Requirement: Application layer MUST not depend on infra

The application layer SHALL NOT import or depend on `infra.web`, `infra.persistence`, `infra.cache`, `infra.batch`, `infra.scheduler`, JPA entities, Spring Data repositories, or MVC request/response types.

#### Scenario: A handler needs to save an aggregate

- **WHEN** the handler needs persistence
- **THEN** it calls an `application.port.out` interface
- **AND** an infra adapter implements that interface.

### Requirement: Infrastructure adapters MAY depend inward only

Infrastructure adapters MAY depend on application and domain packages, but domain/application SHALL NOT depend on infra.

#### Scenario: A JPA adapter saves a domain aggregate

- **WHEN** the adapter receives a domain aggregate
- **THEN** it maps it to a JPA entity inside `infra.persistence`
- **AND** returns a domain model or application view.

### Requirement: `port.in` is forbidden by default

Core modules SHALL NOT define `port.in` packages by default because CommandBus/QueryBus are the canonical application entry points.

#### Scenario: A developer creates `application.port.in`

- **WHEN** no ADR explicitly approves it
- **THEN** ArchUnit fails the build.

### Requirement: Controllers MUST be thin

Controllers SHALL only validate/map request input, resolve context, dispatch commands/queries, map responses, and declare audit/security metadata.

#### Scenario: A controller needs ticket data

- **WHEN** an endpoint needs data
- **THEN** it calls `QueryBus.ask(...)`
- **AND** it does not call repositories or persistence adapters directly.

### Requirement: JPA entities and repositories MUST stay in persistence infra

Classes annotated with `@Entity` and Spring Data repositories SHALL reside in `..infra.persistence..`.

#### Scenario: A JPA entity is added in domain

- **WHEN** a class annotated `@Entity` resides in `..domain..`
- **THEN** ArchUnit fails the build.

### Requirement: Services MUST be named and placed by responsibility

Domain services SHALL be pure policies/calculators/rules. Application services SHALL be orchestration helpers only.

#### Scenario: `PayoutService` appears in domain

- **WHEN** `PayoutService` injects a repository, port, bus, or Spring bean
- **THEN** it violates the architecture
- **AND** must be split into domain policy and application handler/orchestrator.
