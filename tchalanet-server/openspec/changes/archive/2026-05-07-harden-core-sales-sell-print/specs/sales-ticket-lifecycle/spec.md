# sales-ticket-lifecycle Specification

## MODIFIED Requirements

### Requirement: Canonical sell command

`core.sales` SHALL use `SellTicketCommand` as the canonical command for ticket sale.

#### Scenario: POS BFF places a sale

- **GIVEN** a POS BFF request named `PlaceSaleRequest`
- **WHEN** the BFF receives the request
- **THEN** it SHALL map it to `SellTicketCommand`
- **AND** it SHALL NOT create a separate `PlaceTicketCommand`

### Requirement: Session required for new sales

New ticket sales SHALL require an active sales session in MVP.

#### Scenario: Sale without session

- **GIVEN** a `SellTicketCommand`
- **AND** no active session can be resolved
- **WHEN** the command is handled
- **THEN** the sale SHALL be rejected
- **AND** no ticket SHALL be persisted
- **AND** no `TicketPlacedEvent` SHALL be published

### Requirement: Canonical ticket line preparation

`TicketLinePreparationService` SHALL expose a canonical `prepare(tenantId, lines)` method.

#### Scenario: Prepare sale lines

- **GIVEN** raw sell line commands
- **WHEN** `prepare` is called
- **THEN** it SHALL validate the list is non-empty
- **AND** normalize selections
- **AND** validate bet options
- **AND** canonicalize stakes to scale 2
- **AND** merge duplicate lines
- **AND** resolve odds snapshots
- **AND** calculate potential payout
- **AND** return immutable `TicketLine` values

### Requirement: Stake scale validation

Ticket stakes SHALL allow at most two decimal places.

#### Scenario: Stake has more than two decimals

- **GIVEN** a line stake `10.123`
- **WHEN** ticket line preparation runs
- **THEN** the sale SHALL fail with a validation error

### Requirement: Odds must exist

Ticket line preparation SHALL fail when no pricing odds exist.

#### Scenario: Missing odds

- **GIVEN** a valid ticket line
- **AND** no odds are configured for its tenant, game, bet type and option
- **WHEN** ticket line preparation runs
- **THEN** the sale SHALL fail
- **AND** no ticket SHALL be persisted

### Requirement: Mixed game validation before save

If MVP supports only one primary game per ticket event, mixed game validation SHALL occur before persistence.

#### Scenario: Mixed game codes

- **GIVEN** a sell command with lines from multiple game codes
- **WHEN** the command is handled
- **THEN** the handler SHALL reject the sale before `TicketWriterPort.save`
- **AND** no ticket SHALL be persisted

### Requirement: Approval request id persistence

A pending approval ticket SHALL persist its approval request id.

#### Scenario: Sale requires approval

- **GIVEN** a sale that breaches a limit with approval outcome
- **WHEN** the command is handled
- **THEN** the handler SHALL generate an approval request id before creating the ticket
- **AND** the ticket SHALL be persisted with that approval request id
- **AND** `SellTicketResult` SHALL return the same approval request id

### Requirement: No web response context in command handler

`SellTicketCommandHandler` SHOULD NOT mutate `ApiResponseContext`.

#### Scenario: Sale returns warnings

- **GIVEN** a sale with warnings or approval requirement
- **WHEN** the command is handled
- **THEN** the handler SHALL return warnings or approval details in `SellTicketResult`
- **AND** the web layer or BFF SHALL map them to API notices

### Requirement: Public code required

Every ticket SHALL have a non-blank `publicCode`.

#### Scenario: Create ticket without public code

- **GIVEN** ticket creation inputs with a null or blank public code
- **WHEN** `Ticket.sell` or `Ticket.pendingApproval` is called
- **THEN** ticket creation SHALL fail

### Requirement: Ticket code uniqueness retry

The system SHALL retry ticket code generation on unique constraint collision.

#### Scenario: Unique constraint collision

- **GIVEN** a generated `ticketCode` or `publicCode` collides with an existing row
- **WHEN** the ticket is saved
- **THEN** the system SHALL regenerate ticket codes
- **AND** retry up to 3 attempts

#### Scenario: Retry exhausted

- **GIVEN** ticket code generation collides 3 times
- **WHEN** the ticket save is attempted
- **THEN** the system SHALL throw `TicketCodeGenerationException`
- **AND** the HTTP layer SHALL map it to `503 Service Unavailable`

### Requirement: Writer port naming

The sales writer port SHALL be named `TicketWriterPort`.

#### Scenario: Code references old typo

- **GIVEN** source code in `core.sales`
- **WHEN** the project is compiled
- **THEN** no reference to `TicketWritterPort` SHALL remain
