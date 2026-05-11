## ADDED Requirements

### Requirement: Ticket HTTP controllers are split by responsibility

The current oversized ticket controller MUST be split into focused controllers.

#### Scenario: Tenant sells or changes ticket sale state

- **WHEN** a tenant sell, cancel, approve, or reject endpoint is handled
- **THEN** the endpoint lives in `TicketSalesController`

#### Scenario: Tenant reads tickets

- **WHEN** a tenant list or detail endpoint is handled
- **THEN** the endpoint lives in `TicketQueryController`

#### Scenario: Admin overrides a ticket result

- **WHEN** an admin result override endpoint is handled
- **THEN** the endpoint lives in `AdminTicketController`

### Requirement: Public verification is not served by core sales web controller

Public ticket verification MUST be owned by `features.ticketverify`.

#### Scenario: Public code is verified

- **WHEN** `GET /public/tickets/verify/{code}` is called
- **THEN** `features.ticketverify.TicketVerifyController` handles the request
- **AND** no `core.sales` controller exposes a public verification endpoint

### Requirement: Receipt endpoints are not served by core sales web controller

Ticket receipt PDF, ESC/POS, and QR endpoints MUST be owned by `features.receipt`.

An interim `features.ticketreceipt` package MAY exist only during migration and must not be treated as the target name.

#### Scenario: Tenant requests ticket PDF

- **WHEN** `GET /tenant/tickets/{ticketId}/print.pdf` is called
- **THEN** a `features.receipt` controller handles the request

#### Scenario: Tenant requests deprecated base64 print endpoint

- **WHEN** `/tenant/tickets/{ticketId}/print` exists
- **THEN** it is removed or deprecated in favor of binary receipt endpoints

### Requirement: Ticket details not-found is handled in the query boundary

Ticket details lookup MUST NOT return `null` for not-found.

#### Scenario: Ticket does not exist

- **WHEN** `GetTicketDetailsQueryHandler` cannot find a ticket
- **THEN** it returns an explicit empty result or throws a mapped not-found exception
- **AND** the controller does not check for `null`

### Requirement: Ticket search respects requested pageable

Ticket search MUST respect the `Pageable` received from the query/controller.

#### Scenario: Client requests custom sort

- **WHEN** a valid `Pageable` with sort is passed to `JpaTicketRepositoryAdapter.search`
- **THEN** the adapter uses that pageable
- **AND** it does not replace it with a hardcoded sort
