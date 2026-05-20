# offlinesync Specification

## ADDED Requirements

### Requirement: Offline grant issuance

The system SHALL issue an offline grant only when the request has a trusted operational context and the POS context is validated server-side.

#### Scenario: grant accepted

- **GIVEN** a cashier with a trusted operational context
- **AND** terminal, outlet and sales session are valid
- **AND** tenant plan allows offline sales
- **WHEN** the POS requests an offline grant
- **THEN** the system creates an `OfflineGrant`
- **AND** creates an `OfflineCodeBatch`
- **AND** returns signed grant metadata and offline codes

#### Scenario: untrusted context rejected

- **GIVEN** the request contains only client-claimed operational context
- **WHEN** the POS requests an offline grant
- **THEN** the system rejects the request with `403` or a domain problem code

### Requirement: separate grant creation and sync windows

The system SHALL distinguish `validUntil` from `syncAcceptedUntil`.

#### Scenario: sale created during grant validity and synced later

- **GIVEN** a grant valid until `T1`
- **AND** sync accepted until `T2`
- **AND** `T1 < T2`
- **WHEN** a sale created before `T1` is synced after `T1` but before `T2`
- **THEN** the submission is eligible for technical validation

#### Scenario: sync after syncAcceptedUntil rejected

- **WHEN** a submission is received after `syncAcceptedUntil`
- **THEN** the submission is rejected technically

### Requirement: offline code lifecycle

The system SHALL never return a submitted code to `AVAILABLE`.

#### Scenario: technical rejection after reservation

- **GIVEN** an offline code transitioned from `AVAILABLE` to `RESERVED`
- **WHEN** the submission is technically rejected
- **THEN** the code transitions to `CONSUMED_REJECTED`
- **AND** it does not transition to `AVAILABLE`

### Requirement: strict submission idempotence

The system SHALL detect duplicate and conflicting submissions by `clientSubmissionId` and `payloadHash`.

#### Scenario: same submission replay

- **GIVEN** a submission already exists for `clientSubmissionId = C1` and `payloadHash = H1`
- **WHEN** the same submission is received again
- **THEN** the API returns result `DUPLICATE`
- **AND** no new `OfflineSubmission` row is created

#### Scenario: payload mismatch

- **GIVEN** a submission already exists for `clientSubmissionId = C1` and `payloadHash = H1`
- **WHEN** a submission arrives with `clientSubmissionId = C1` and `payloadHash = H2`
- **THEN** the API returns conflict `offlinesync.submission.payload_mismatch`

### Requirement: self-contained promotion event

The system SHALL publish self-contained promotion events after technical validation.

#### Scenario: event published

- **GIVEN** a submission is technically validated
- **WHEN** the transaction commits
- **THEN** `OfflineSubmissionTechValidatedEvent` is published
- **AND** it contains all payload required by `sales` to create or reject the ticket

### Requirement: stale promotion result ignored

The system SHALL ignore `OfflineSubmissionProcessedEvent` when its `promotionAttemptId` is no longer current.

#### Scenario: stale result

- **GIVEN** a submission has current promotion attempt `P2`
- **WHEN** an event returns for attempt `P1`
- **THEN** the event is marked processed
- **AND** the submission state is not changed
