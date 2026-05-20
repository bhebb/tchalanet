# core.sales spec delta

## ADDED Requirements

### Requirement: PosCancelOperationValidator

`core.sales` SHALL provide `PosCancelOperationValidator` composing `ValidateTerminalForOperationQuery(CANCEL)` and `ValidateSalesSessionForOperationQuery(CANCEL)`.

#### Scenario: Cancel against CANCELLED session

- **GIVEN** a request to cancel a ticket on a session with status `CANCELLED`
- **WHEN** `PosCancelOperationValidator` runs
- **THEN** it SHALL reject with the session status error

#### Scenario: Cancel against FINALIZED session

- **GIVEN** a request to cancel a ticket on a session with status `FINALIZED`
- **WHEN** the validator runs
- **THEN** it SHALL reject with the session status error

#### Scenario: Locked terminal blocks cancel

- **GIVEN** a request to cancel on a `LOCKED` terminal
- **WHEN** the validator runs
- **THEN** it SHALL reject with `TERMINAL_LOCKED`

### Requirement: OfflineSaleAcceptanceValidator

`core.sales` SHALL provide `OfflineSaleAcceptanceValidator` that gates whether a technically-accepted offline submission becomes an official ticket.

#### Scenario: FINALIZED session routes to review

- **GIVEN** an offline submission for a `FINALIZED` session
- **WHEN** the validator runs
- **THEN** the decision SHALL be `REVIEW_REQUIRED`
- **AND** the reject reason SHALL be `SESSION_FINALIZED`
- **AND** the risk flags SHALL include `FINALIZED_SESSION`

#### Scenario: Draw result already known

- **GIVEN** an offline submission whose draw result is already known
- **WHEN** the validator runs
- **THEN** the decision SHALL be `REVIEW_REQUIRED` or `REJECTED` per tenant policy
- **AND** the risk flags SHALL include `RESULT_KNOWN_AT_SYNC`

#### Scenario: Device time alone is insufficient

- **GIVEN** an offline submission whose only time evidence is the device clock
- **WHEN** the validator evaluates trust
- **THEN** it SHALL combine grant issue time, validity window, server `receivedAt`, draw cutoff, and clock drift
- **AND** SHALL NOT auto-accept on device time alone

### Requirement: Fail-fast ordering in sales validators

Every sales use-case validator SHALL apply checks in this order: trusted context → terminal → outlet → session → action-specific gates.

#### Scenario: Missing trusted context short-circuits

- **GIVEN** a request without a trusted operational context
- **WHEN** any sales validator runs
- **THEN** it SHALL fail before reading terminal/outlet/session

### Requirement: Sales owns the ticket lifecycle

`core.offlinesync` SHALL NOT create rows in `sales.ticket`. Tickets are produced only through commands handled by `core.sales`, which then publish `TicketPlacedEvent`.

#### Scenario: Approved offline submission produces a ticket via Sales

- **GIVEN** an admin approves an offline submission
- **WHEN** the approval flow proceeds
- **THEN** `core.offlinesync` SHALL issue a command to `core.sales` rather than writing the ticket itself
- **AND** the resulting `TicketPlacedEvent` SHALL be published by `core.sales`
