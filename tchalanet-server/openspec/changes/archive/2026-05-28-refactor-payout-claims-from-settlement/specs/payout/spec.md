# Spec: Payout claims from settlement

## ADDED Requirements

### Requirement: Payout claims are opened from winning ticket settlement

`core.payout` SHALL open payout claims from Sales winning settlement events, not from normal POS/manual requests.

#### Scenario: Sales emits winning settlement event

- **GIVEN** Sales has settled a ticket as won with payout amount greater than zero
- **WHEN** `TicketWinningSettlementCreatedEvent` is consumed
- **THEN** Payout SHALL create a `PayoutClaim` with status `OPEN`
- **AND** source `SALES_SETTLEMENT`
- **AND** source event id recorded.

#### Scenario: duplicate winning settlement event is replayed

- **GIVEN** a payout claim already exists for the same winning settlement
- **WHEN** the event is replayed
- **THEN** Payout SHALL not create a duplicate claim
- **AND** the handler SHALL complete idempotently.

### Requirement: No normal payout request flow in V1

Cashier/POS SHALL NOT create payout claims through a normal `RegisterPayoutCommand` flow.

#### Scenario: cashier scans a winning ticket

- **WHEN** the cashier verifies a ticket
- **THEN** Cashier SHALL look up an existing payout claim
- **AND** SHALL NOT create the claim.

### Requirement: Execute payout is the only normal payment transition

`ExecutePayoutCommand` SHALL be the only normal path to mark a payout claim as paid.

#### Scenario: claim is open and context is valid

- **GIVEN** claim status is `OPEN`
- **AND** terminal/outlet/session are valid and trusted
- **AND** Sales settled payout snapshot matches the claim
- **WHEN** payout is executed
- **THEN** the claim SHALL transition to `PAID`
- **AND** `PayoutPaidEvent` SHALL be published after commit.

#### Scenario: claim is blocked

- **GIVEN** claim status is `BLOCKED`
- **WHEN** payout execution is attempted
- **THEN** the command SHALL not mark the claim as paid.

#### Scenario: concurrent payout execution

- **GIVEN** two terminals attempt to pay the same claim concurrently
- **WHEN** both requests are processed
- **THEN** only one payment SHALL be posted
- **AND** the other response SHALL report already-paid or blocked according to implementation.

### Requirement: Manual actions are corrections only

Admin/Ops manual payout actions SHALL be limited to block, unblock, cancel, and reverse.

#### Scenario: admin blocks a claim

- **WHEN** admin blocks an open claim with a reason
- **THEN** claim status SHALL become `BLOCKED`
- **AND** the action SHALL be audited.

#### Scenario: admin reverses a paid payout

- **WHEN** admin reverses a paid claim with a reason
- **THEN** claim status SHALL become `REVERSED`
- **AND** `PayoutReversedEvent` SHALL be published after commit.

### Requirement: Sales may project payout status but not own payment truth

Sales MAY consume payout events to update ticket-facing payout projections.

#### Scenario: payout paid event is consumed by Sales

- **WHEN** Sales consumes `PayoutPaidEvent`
- **THEN** Sales MAY mark ticket payout projection as paid
- **BUT** Payout remains the source of payment truth.
