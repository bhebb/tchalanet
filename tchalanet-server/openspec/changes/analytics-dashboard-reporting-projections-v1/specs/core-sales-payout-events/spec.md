# core-sales-payout-events Spec

## ADDED Requirements

### Requirement: Sales snapshot seller-terminal effective odds

`core.sales` SHALL request the effective odds from `core.pricing` for each sold ticket line and
snapshot that value before the ticket is persisted.

#### Scenario: Seller terminal has an odds override

- **WHEN** a seller terminal sells a ticket line for a game, bet type and bet option with an active
  seller-terminal odds override
- **THEN** `core.sales` stores the override value as the ticket line `oddsSnapshot`
- **AND** the line potential payout is calculated from the stake and the override value.

#### Scenario: Seller terminal has no odds override

- **WHEN** a seller terminal sells a ticket line for a game, bet type and bet option without an
  active seller-terminal odds override
- **THEN** `core.sales` stores the tenant default odds as the ticket line `oddsSnapshot`.

#### Scenario: Promotion creates a free game line

- **WHEN** a sale promotion creates a free game line for a seller terminal
- **THEN** `core.sales` resolves odds through `core.pricing` with the same seller-terminal context
- **AND** `core.sales` stores the effective odds as the promotional ticket line `oddsSnapshot`
- **AND** the promotional line potential payout is calculated from the promotion payout base and
  the effective odds.

#### Scenario: Result settlement calculates winnings later

- **WHEN** an official or corrected result is applied after odds configuration has changed
- **THEN** `core.sales` calculates the winning amount from the ticket line snapshot
- **AND** `core.sales` does not reread current seller-terminal or tenant odds for that historical
  ticket.

### Requirement: Result application auto-settles ticket payouts

`core.sales` SHALL treat draw result application as the normal payout settlement point. A seller
does not perform a separate "paid" action in the V1 operational flow.

#### Scenario: Ticket result is applied

- **WHEN** `RecordDrawTicketsResultCommand` applies an official result to a ticket
- **THEN** losing/no-payout tickets are auto-settled as no payout
- **AND** winning tickets are auto-settled as paid by the system
- **AND** `core.sales` publishes `TicketWinningSettlementCreatedEvent` and `TicketPayoutPaidEvent`
  after commit for each winning ticket
- **AND** the paid event includes tenant id, ticket id, draw id, amount cents, currency, seller
  terminal id, and actor.

### Requirement: Corrections publish financial reversals

`core.sales` SHALL publish reversal events when a corrected draw result invalidates or changes a
previously calculated winning settlement or auto-paid payout.

#### Scenario: Payout is reversed

- **WHEN** `MarkTicketPayoutReversedCommand` succeeds
- **THEN** `core.sales` publishes `TicketPayoutReversedEvent` after commit
- **AND** the event includes tenant id, ticket id, draw id, amount cents, currency, seller terminal
  id, and actor.

#### Scenario: Corrected result changes an already-paid ticket

- **WHEN** `ReconcileTicketsForCorrectedDrawResultCommand` corrects a ticket that had an auto-paid payout
- **THEN** `core.sales` publishes `TicketWinningSettlementReversedEvent` after commit for the previous winning amount
- **AND** `core.sales` publishes `TicketPayoutReversedEvent` after commit for the previous paid amount
- **AND** if the corrected result still has a winning amount, `core.sales` publishes a new
  `TicketWinningSettlementCreatedEvent` and `TicketPayoutPaidEvent` for the corrected amount
- **AND** if the corrected result has no winning amount, no new paid event is published.
