# system-execution-scope spec delta

## ADDED Requirements

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
