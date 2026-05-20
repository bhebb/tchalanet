# core.offlinesync spec delta

## ADDED Requirements

### Requirement: Offline submission is not a ticket

`OfflineSaleSubmission`, `OfflineCodeReservation`, and offline receipts SHALL NOT be treated as official tickets. Only `core.sales` produces `sales.ticket` rows.

#### Scenario: Offlinesync never writes sales.ticket

- **GIVEN** a technically-accepted offline submission
- **WHEN** `core.offlinesync` records it
- **THEN** it SHALL NOT insert into `sales.ticket`
- **AND** any official ticket creation SHALL be delegated to `core.sales` through a command

#### Scenario: Offline counts do not pollute official stats

- **GIVEN** a submission in status `RECEIVED` or `READY_FOR_SALES`
- **WHEN** sales statistics are computed
- **THEN** the submission SHALL NOT be counted as an official sale until a `TicketPlacedEvent` is emitted by `core.sales`

### Requirement: Offline sync domain events

`core.offlinesync` SHALL publish, after commit, the following events:

- `OfflineGrantIssuedEvent`
- `OfflineBatchReceivedEvent`
- `OfflineSubmissionTechnicallyRejectedEvent`
- `OfflineBatchReadyForSalesEvent`
- `OfflineSubmissionSalesDecisionRecordedEvent`

#### Scenario: Batch reception publishes event

- **GIVEN** a valid offline batch is received and technically validated
- **WHEN** the receive command commits
- **THEN** `OfflineBatchReceivedEvent` SHALL be published after commit
- **AND** `OfflineBatchReadyForSalesEvent` SHALL be published when the batch reaches `READY_FOR_SALES`

#### Scenario: Technical rejection publishes event

- **GIVEN** a submission rejected by `INVALID_SIGNATURE`, `PAYLOAD_HASH_MISMATCH`, or any other technical reason
- **WHEN** the handler commits
- **THEN** `OfflineSubmissionTechnicallyRejectedEvent` SHALL be published

### Requirement: Technical reject taxonomy

`core.offlinesync` SHALL recognise at least the following technical reject reasons: `INVALID_SIGNATURE`, `PAYLOAD_HASH_MISMATCH`, `UNKNOWN_GRANT`, `GRANT_REVOKED`, `GRANT_EXPIRED`, `DUPLICATE_CODE`, `SEQUENCE_GAP`, `TERMINAL_MISMATCH`, `SELLER_MISMATCH`, `SESSION_MISMATCH`.

#### Scenario: Sequence gap detected

- **GIVEN** an offline submission whose `localSequence` skips an expected value for the terminal
- **WHEN** the receive command runs
- **THEN** the submission SHALL be marked `TECHNICALLY_REJECTED` with reason `SEQUENCE_GAP`

### Requirement: Risk flags on submissions

`OfflineSaleSubmission` SHALL carry a `riskFlags` collection with at least values `FINALIZED_SESSION` and `RESULT_KNOWN_AT_SYNC`.

#### Scenario: FINALIZED session flagged

- **GIVEN** an offline submission targeting a `FINALIZED` session
- **WHEN** the sales-decision projection is updated
- **THEN** `riskFlags` SHALL include `FINALIZED_SESSION`

#### Scenario: Result already known

- **GIVEN** an offline submission whose draw result is already known
- **WHEN** the sales-decision projection is updated
- **THEN** `riskFlags` SHALL include `RESULT_KNOWN_AT_SYNC`

### Requirement: Approve offline submission bridges to Sales

`ApproveOfflineSubmissionCommandHandler` SHALL, on admin approval:

1. require a trusted operational context
2. invoke `OfflineSaleAcceptanceValidator` (in `core.sales`)
3. issue a sales command that produces an official ticket
4. record `SalesOfflineDecision.ACCEPTED` and publish `OfflineSubmissionSalesDecisionRecordedEvent`

#### Scenario: Approval produces a ticket via Sales

- **GIVEN** an admin approves a submission in status `READY_FOR_SALES`
- **WHEN** the command commits
- **THEN** a sales command SHALL be issued to create the official ticket
- **AND** `TicketPlacedEvent` SHALL be published by `core.sales`
- **AND** `OfflineSubmissionSalesDecisionRecordedEvent` SHALL be published by `core.offlinesync`

#### Scenario: Approval against FINALIZED session is rejected

- **GIVEN** an admin tries to approve a submission targeting a `FINALIZED` session
- **WHEN** `OfflineSaleAcceptanceValidator` runs
- **THEN** the decision SHALL be `REVIEW_REQUIRED`
- **AND** no `TicketPlacedEvent` SHALL be published
