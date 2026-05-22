# Spec: timezone

## ADDED Requirements

### Requirement: Business event timestamps use Instant

All business event moments SHALL be represented as `Instant` in domain/application models and persisted as timezone-aware database timestamps.

Examples include `createdAt`, `updatedAt`, `soldAt`, `openedAt`, `closedAt`, `scheduledAt`, `cutoffAt`, `occurredAt`, `resultedAt`, `syncedAt`, and `paidAt`.

#### Scenario: Persist ticket sold time

- **GIVEN** a ticket sale is accepted
- **WHEN** the sale is saved
- **THEN** `soldAt` is an `Instant`
- **AND** the database column is timezone-aware
- **AND** the API exposes an ISO-8601 instant.

#### Scenario: Reject LocalDateTime business event

- **GIVEN** new business code persists an event timestamp
- **WHEN** the field type is `LocalDateTime`
- **THEN** architecture/static verification fails unless the field is explicitly allowlisted as a UI/query helper.

### Requirement: Clock is the only now source

Business code SHALL obtain the current moment from injected `Clock` or `TchTimeProvider`.

#### Scenario: Direct Instant.now in core application

- **GIVEN** code under `core/**/application/**`
- **WHEN** it calls `Instant.now()` directly
- **THEN** static verification fails.

#### Scenario: JVM timezone usage

- **GIVEN** business code under `core/**` or `features/**`
- **WHEN** it calls `ZoneId.systemDefault()`
- **THEN** static verification fails.

### Requirement: Semantic owner timezone is explicit

Every calendar-based computation SHALL identify its semantic owner timezone.

#### Scenario: Tenant dashboard today

- **GIVEN** a tenant dashboard requests “today”
- **WHEN** the date window is computed
- **THEN** the calculation uses `ctx.tenantZoneId()`.

#### Scenario: Result slot occurredAt

- **GIVEN** a result slot has timezone `America/New_York`
- **WHEN** occurredAt is computed from slot date/time
- **THEN** the calculation uses `America/New_York`
- **AND** does not mutate request context timezone.
