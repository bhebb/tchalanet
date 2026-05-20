# Spec: scheduler

## ADDED Requirements

### Requirement: Scheduler does not use JVM timezone for business decisions

Schedulers SHALL use explicit semantic timezones for calendar windows and `Instant` for moment comparisons.

#### Scenario: Generate draw range

- **GIVEN** a draw channel timezone
- **WHEN** the scheduler generates draw dates
- **THEN** the base date is computed in the draw channel timezone.

#### Scenario: Close draw

- **GIVEN** an open draw with `cutoffAt`
- **WHEN** the scheduler compares current time
- **THEN** it compares `timeProvider.now()` with `cutoffAt`.

### Requirement: Scheduler logs include semantic zone

Scheduler summaries SHALL include current instant and semantic zone/date/time used for the decision.

#### Scenario: Open-today summary

- **WHEN** open-today logs a tick summary
- **THEN** the log includes `now`, `channelZone`, `channelDate` or equivalent aggregated observability.
