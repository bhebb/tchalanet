# Spec: features.cashier

## ADDED Requirements

### Requirement: cashier feature owns cashier-facing ticket pages

`features.cashier` SHALL expose endpoints/pages oriented around cashier workflows and compose data from core APIs.

#### Scenario: cashier ticket list page

- **WHEN** a cashier opens the tickets page
- **THEN** `features.cashier` SHALL call `core.sales.api.query.ListCashierTicketsQuery` or `ListTicketsQuery`
- **AND** it SHALL map the result to cashier UI response models
- **AND** it SHALL not access sales persistence directly

#### Scenario: cashier ticket details page

- **WHEN** a cashier opens a ticket details page
- **THEN** `features.cashier` SHALL call `core.sales.api.query.GetCashierTicketDetailsQuery` or `GetTicketDetailsQuery`
- **AND** it SHALL return all details needed by the cashier screen
- **AND** it SHALL not own ticket lifecycle rules

### Requirement: cashier feature orchestrates ticket print rendering

`features.cashier` MAY orchestrate ticket print rendering by combining sales print snapshots with platform document rendering.

#### Scenario: cashier requests print view

- **WHEN** a cashier asks to print a ticket
- **THEN** `features.cashier` SHALL ask `GetTicketPrintViewQuery` from `core.sales.api.query`
- **AND** it MAY call `platform.document.api` to render PDF/ESC-POS/QR bytes
- **AND** it SHALL not decide whether the ticket is printable; sales decides that

### Requirement: features.cashier is a leaf module

`features.cashier` SHALL NOT expose a Java API consumed by other modules.

#### Scenario: another module needs ticket data

- **WHEN** another module needs ticket data
- **THEN** it SHALL depend on `core.sales.api`, not `features.cashier`
