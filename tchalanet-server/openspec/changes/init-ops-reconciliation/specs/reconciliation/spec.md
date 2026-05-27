# Spec: Ops reconciliation

## ADDED Requirements

### Requirement: Reconciliation stores runs and anomalies

The platform SHALL store reconciliation runs, check results, anomalies, and repair action records.

#### Scenario: daily run completes without anomalies

- **WHEN** a daily reconciliation run completes cleanly
- **THEN** a run record SHALL be stored with status `COMPLETED`
- **AND** no tenant-admin anomaly email SHALL be sent.

#### Scenario: daily run detects anomalies

- **WHEN** checks detect mismatches
- **THEN** anomalies SHALL be stored with severity and status `OPEN`
- **AND** the run status SHALL indicate anomalies.

### Requirement: Reconciliation compares Sales winning tickets and Payout claims

The reconciliation process SHALL detect winning settled tickets that do not have payout claims.

#### Scenario: winning settled ticket has no payout claim

- **GIVEN** Sales reports a settled winning ticket with payout amount greater than zero
- **AND** Payout has no claim for that ticket
- **WHEN** reconciliation runs
- **THEN** an anomaly `WINNING_TICKET_WITHOUT_PAYOUT_CLAIM` SHALL be created.

#### Scenario: payout claim amount differs from Sales settlement

- **GIVEN** Sales reports payout amount `A`
- **AND** Payout claim has amount `B`
- **WHEN** `A != B`
- **THEN** an anomaly `PAYOUT_AMOUNT_MISMATCH` SHALL be created.

### Requirement: Reconciliation compares Payout claims and payments

The reconciliation process SHALL detect inconsistencies between claim status and payment records.

#### Scenario: paid claim has no posted payment

- **GIVEN** a payout claim is `PAID`
- **AND** no posted payment evidence exists
- **WHEN** reconciliation runs
- **THEN** a critical anomaly SHALL be created.

### Requirement: Reconciliation compares Offline submissions and Sales tickets

The reconciliation process SHALL compare offline submissions against tickets created from them.

#### Scenario: accepted submission has no ticket

- **GIVEN** an offline submission is `ACCEPTED`
- **AND** no Sales ticket exists for it
- **WHEN** reconciliation runs
- **THEN** an anomaly `ACCEPTED_OFFLINE_SUBMISSION_WITHOUT_TICKET` SHALL be created.

#### Scenario: rejected submission has active ticket

- **GIVEN** an offline submission is `REJECTED`
- **AND** an active Sales ticket exists for it
- **WHEN** reconciliation runs
- **THEN** a critical anomaly SHALL be created.

### Requirement: Repairs dispatch owner-domain commands

Reconciliation SHALL NOT mutate Sales, Payout, Draw, or OfflineSync persistence directly.

#### Scenario: missing payout claim repair is executed

- **GIVEN** anomaly `WINNING_TICKET_WITHOUT_PAYOUT_CLAIM`
- **WHEN** Ops triggers repair
- **THEN** reconciliation SHALL dispatch the Payout owner command to open a claim
- **AND** record the repair action result.

### Requirement: Tenant admins are notified for important anomalies

Tenant admins SHALL receive email notifications when reconciliation detects critical or attention-required anomalies.

#### Scenario: critical anomaly detected

- **WHEN** a run completes with one or more critical anomalies
- **THEN** tenant admin recipients SHALL be resolved
- **AND** an email SHALL be sent through platform communication.

#### Scenario: no anomaly detected

- **WHEN** a run completes cleanly
- **THEN** no anomaly email SHALL be sent.
