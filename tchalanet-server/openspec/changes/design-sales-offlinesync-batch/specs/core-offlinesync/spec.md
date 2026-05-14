# Spec: core.offlinesync

## ADDED Requirements

### Requirement: API contains only externally consumed Java contracts

`core.offlinesync.api` SHALL expose only contracts consumed by another Java module.

#### Scenario: grant command used only by offlinesync controller

- **GIVEN** `IssueOfflineSalesGrantCommand` or `RequestOfflineGrantCommand` is only used by `OfflineGrantController`
- **THEN** it SHALL live under `core.offlinesync.internal.application.command.model`
- **AND** it SHALL NOT live under `core.offlinesync.api.command`

#### Scenario: sales needs a submission snapshot

- **GIVEN** `core.sales` processes an offline submission
- **WHEN** it asks `GetOfflineSubmissionForSalesQuery`
- **THEN** the query SHALL live under `core.offlinesync.api.query`
- **AND** the returned view SHALL live under `core.offlinesync.api.model`

### Requirement: offline batch reception persists submissions but does not call sales immediately

`ReceiveOfflineBatchCommandHandler` SHALL persist offline submissions and assign processing statuses without synchronously invoking sales.

#### Scenario: technically valid submission received

- **WHEN** a submission passes grant/device/signature/payload checks
- **THEN** offlinesync SHALL persist it with `READY_FOR_SALES`
- **AND** the HTTP response SHALL acknowledge receipt
- **AND** sales SHALL NOT be called in that HTTP request path

#### Scenario: technically invalid submission received

- **WHEN** a submission fails technical validation
- **THEN** offlinesync SHALL persist it with `TECH_REJECTED` or `REVIEW_REQUIRED`
- **AND** it SHALL include a technical rejection code/reason

### Requirement: scheduler dispatches ready submissions in bounded chunks

`core.offlinesync` SHALL include a configurable scheduler that dispatches ready submissions to sales.

#### Scenario: scheduler tick

- **GIVEN** `tch.offlinesync.sales-processing.active=true`
- **WHEN** the scheduler fires according to configured cron
- **THEN** it SHALL execute `DispatchReadyOfflineSubmissionsCommand`
- **AND** the command SHALL claim at most `max-items-per-tick` submissions
- **AND** each claimed submission SHALL be marked `SALES_PROCESSING`

#### Scenario: sales accepts a submission

- **WHEN** sales returns accepted with a ticket id or publishes accepted event
- **THEN** offlinesync SHALL record `SALES_ACCEPTED`
- **AND** store the created `ticketId`

#### Scenario: sales rejects a submission

- **WHEN** sales returns rejected or publishes rejected event
- **THEN** offlinesync SHALL record `SALES_REJECTED`
- **AND** store the sales rejection code/reason

#### Scenario: temporary processing failure

- **WHEN** sales processing fails due to transient error
- **THEN** offlinesync SHALL increment attempt count
- **AND** set `RETRY_PENDING` with `nextProcessingAt`
- **OR** mark `REVIEW_REQUIRED` after max attempts

### Requirement: offlinesync owns grants, device proof, and submission status

`core.offlinesync` SHALL own offline grant lifecycle, device proof validation, token verification, and submission technical statuses.

#### Scenario: offline grant request

- **WHEN** a grant request is received
- **THEN** offlinesync SHALL require a validated POS context and strong device proof
- **AND** it SHALL store a revocable grant

#### Scenario: submission duplicate

- **GIVEN** the same `(tenant, grant, clientSaleId)` is submitted twice
- **THEN** offlinesync SHALL prevent duplicate submissions
- **AND** return or expose the existing processing result where appropriate

## Suggested Public API

```text
core.offlinesync.api.query
  GetOfflineSubmissionForSalesQuery

core.offlinesync.api.model
  OfflineSubmissionForSalesView

core.offlinesync.api.event
  OfflineGrantIssuedEvent
  OfflineGrantRevokedEvent
  OfflineBatchReceivedEvent
  OfflineSubmissionReceivedEvent
  OfflineSubmissionTechnicallyRejectedEvent
  OfflineSubmissionReadyForSalesEvent
  OfflineSubmissionSalesDecisionRecordedEvent
```

## Suggested Internal Commands

```text
core.offlinesync.internal.application.command.model
  RequestOfflineGrantCommand
  RevokeOfflineGrantCommand
  ReceiveOfflineBatchCommand
  DispatchReadyOfflineSubmissionsCommand
  RecordOfflineSubmissionSalesDecisionCommand
  RetryOfflineSubmissionCommand
  AdminRejectOfflineSubmissionCommand
```

## Suggested Statuses

```text
RECEIVED
TECH_REJECTED
READY_FOR_SALES
SALES_PROCESSING
SALES_ACCEPTED
SALES_REJECTED
REVIEW_REQUIRED
RETRY_PENDING
```

## Suggested Configuration

```yaml
tch:
  offlinesync:
    sales-processing:
      active: true
      cron: "0 */5 * * * *"
      max-items-per-tick: 500
      lock-seconds: 300
      max-attempts: 5
      retry-delay-seconds: 600
```
