# inter-domain-communication Specification Delta

## ADDED Requirements

### Requirement: Cross-domain reads SHALL use stable read contracts

A domain that needs data owned by another domain SHALL read through a stable query/read contract, not through the owner domain's infra.

Accepted patterns:

- `QueryBus.ask(new GetXxxForYyyQuery(...))`
- stable API/read interface exposed by the owner domain
- catalog API for read-mostly reference data

#### Scenario: Payout needs ticket information

- **WHEN** payout needs ticket status/winning amount
- **THEN** payout asks `core.sales` for `TicketForPayoutView`
- **AND** sales reads its own ticket persistence
- **AND** payout does not own a ticket JPA adapter.

### Requirement: Cross-domain effects SHALL use events after commit

A domain SHALL NOT write another domain's aggregate directly. Cross-domain effects SHALL be expressed as domain events published after commit.

#### Scenario: Payout is paid and sales must mark the ticket paid

- **WHEN** payout transitions to PAID
- **THEN** payout publishes `PayoutPaidEvent` after commit
- **AND** sales consumes it after commit
- **AND** sales executes a local command to update the ticket.

### Requirement: No domain SHALL depend on another domain's infra

A domain SHALL NOT import or depend on `core.<other>.infra..`.

#### Scenario: Draw handler imports sales persistence adapter

- **WHEN** `core.draw` imports `core.sales.infra.persistence`
- **THEN** ArchUnit fails the build.

### Requirement: Features SHALL orchestrate through buses

Features/BFF modules SHALL orchestrate multiple domains through CommandBus/QueryBus and stable catalog APIs.

#### Scenario: Cashier flow sells and optionally sends receipt

- **WHEN** the cashier feature orchestrates the flow
- **THEN** it executes sales command through CommandBus
- **AND** asks receipt/document data through stable queries
- **AND** does not access repositories directly.
