# Spec: result-pipeline

## ADDED Requirements

### Requirement: Result occurredAt uses provider or slot timezone

A result occurrence moment SHALL be resolved by using provider-supplied `Instant` when available, otherwise by computing from result slot date/time/timezone.

#### Scenario: Provider supplies occurredAt instant

- **GIVEN** the provider payload contains a valid occurrence instant
- **WHEN** the result is stored
- **THEN** the provider instant is used as `occurredAt`.

#### Scenario: Provider omits occurredAt

- **GIVEN** the provider payload does not contain occurrence instant
- **AND** the result slot has `drawDate`, `drawTime`, and `zoneId`
- **WHEN** the result is stored
- **THEN** `occurredAt = ZonedDateTime.of(drawDate, drawTime, zoneId).toInstant()`.

#### Scenario: Fallback to clock is observable

- **GIVEN** neither provider occurredAt nor complete slot schedule data is available
- **WHEN** the resolver falls back to `clock.instant()`
- **THEN** the fallback is logged or exposed as an operational warning.

### Requirement: Fetch windows use result-slot timezone

Fetch retry and stop windows SHALL be based on result-slot/provider occurredAt, not tenant timezone.

#### Scenario: Fetch starts after result slot occurrence

- **GIVEN** result slot occurredAt is `T`
- **AND** fetch starts after 5 minutes
- **WHEN** `now >= T + 5 minutes`
- **THEN** the slot is eligible for fetch.
