# core.payout spec delta

## ADDED Requirements

### Requirement: PosPayoutOperationValidator

`core.payout` SHALL provide `PosPayoutOperationValidator` composing `ValidateTerminalForOperationQuery(PAYOUT)`, `ValidateOutletForOperationQuery(PAYOUT)`, `ValidateSalesSessionForOperationQuery(PAYOUT)`, plus the existing ticket-payable check.

#### Scenario: Blocked outlet rejects payout

- **GIVEN** a payout request on an outlet with `payoutBlocked == true`
- **WHEN** the validator runs
- **THEN** it SHALL reject with `OUTLET_PAYOUT_BLOCKED`

#### Scenario: Locked terminal rejects payout

- **GIVEN** a payout request on a `LOCKED` terminal
- **WHEN** the validator runs
- **THEN** it SHALL reject with `TERMINAL_LOCKED`

#### Scenario: Closed session rejects payout

- **GIVEN** a payout request on a session with status `CLOSED`
- **WHEN** the validator runs
- **THEN** it SHALL reject with `SESSION_CLOSED`

### Requirement: In-tx re-check in RequestPayoutCommandHandler

`RequestPayoutCommandHandler` SHALL re-read terminal-locked, outlet block flags, and session status inside its transaction before mutation.

#### Scenario: Race after validation

- **GIVEN** a payout request validated successfully
- **WHEN** the terminal is locked between validation and commit
- **THEN** the in-tx re-check SHALL fail with `TERMINAL_LOCKED`
- **AND** no payout SHALL be persisted
