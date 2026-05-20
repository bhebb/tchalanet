# core.session spec delta

## ADDED Requirements

### Requirement: SalesSession exposes finalization fields

The `SalesSession` domain record SHALL carry `finalizedAt` and `finalizedBy`, mirroring the existing view model.

#### Scenario: Aggregate exposes finalization

- **GIVEN** a session that has been finalized
- **WHEN** the aggregate is loaded
- **THEN** `finalizedAt` SHALL be set
- **AND** `finalizedBy` SHALL be set

#### Scenario: Pre-go-live Flyway is amended

- **WHEN** Flyway scripts are inspected
- **THEN** the existing pre-go-live `V*__sales_session*.sql` script SHALL declare `finalized_at TIMESTAMPTZ NULL` and `finalized_by UUID NULL`
- **AND** no new `V*` migration SHALL be created solely for these columns (pre-go-live policy)

### Requirement: FINALIZED session rejects late offline submissions

When an offline submission references a `FINALIZED` session, the system SHALL NOT auto-mutate session totals and SHALL route the submission to admin review.

#### Scenario: FINALIZED session triggers review

- **GIVEN** an offline submission targeting a session with status `FINALIZED`
- **WHEN** Sales evaluates the submission
- **THEN** the submission status SHALL be set to `SALES_REVIEW_REQUIRED`
- **AND** `salesRejectReason` SHALL be `SESSION_FINALIZED`
- **AND** `riskFlags` SHALL include `FINALIZED_SESSION`
- **AND** no `TicketPlacedEvent` SHALL be emitted
