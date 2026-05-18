# sales Specification delta for offlinesync

## ADDED Requirements

### Requirement: create ticket from offline submission event

The sales module SHALL consume `OfflineSubmissionTechValidatedEvent` and `OfflineSubmissionAdminApprovedEvent` without querying `core.offlinesync`.

#### Scenario: offline event promoted

- **GIVEN** a self-contained offline submission event
- **WHEN** sales business validation passes
- **THEN** sales creates a ticket
- **AND** sets `ticket.offline_submission_id`
- **AND** publishes `OfflineSubmissionProcessedEvent` with outcome `PROMOTED`

#### Scenario: sales business rejection

- **GIVEN** a self-contained offline submission event
- **WHEN** sales business validation fails
- **THEN** sales does not create a ticket
- **AND** publishes `OfflineSubmissionProcessedEvent` with outcome `BUSINESS_REJECTED`

### Requirement: physical double-ticket protection

The sales table SHALL enforce uniqueness of `(tenant_id, offline_submission_id)`.

#### Scenario: duplicate event replay

- **GIVEN** sales already created a ticket for an offline submission
- **WHEN** the same event is processed again
- **THEN** no second ticket can be created
- **AND** the listener returns an idempotent outcome
