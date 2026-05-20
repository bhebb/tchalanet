# sales-ticket-printing Specification

## ADDED Requirements

### Requirement: Print view assembly outside persistence

Ticket print view assembly SHALL be performed outside persistence adapters.

#### Scenario: Build PDF print view

- **GIVEN** a ticket id
- **WHEN** `GetTicketPrintPdfQueryHandler` handles the query
- **THEN** it SHALL load the ticket via `TicketReaderPort.findWithLinesById`
- **AND** assemble print data via `TicketPrintViewAssembler`
- **AND** format a receipt model
- **AND** build the PDF

#### Scenario: Persistence adapter responsibilities

- **GIVEN** `JpaTicketRepositoryAdapter`
- **WHEN** it is inspected
- **THEN** it SHALL NOT inject `DrawLookupPort`
- **AND** it SHALL NOT inject `OutletReaderPort`
- **AND** it SHALL NOT inject `SalesSessionReaderPort`
- **AND** it SHALL NOT inject `TerminalReaderPort`
- **AND** it SHALL NOT inject `TicketPrintViewMapper`

### Requirement: TicketPrintViewAssembler

`TicketPrintViewAssembler` SHALL orchestrate read-only lookups needed for print.

#### Scenario: Assemble complete ticket print view

- **GIVEN** a ticket with session and terminal
- **WHEN** the assembler runs
- **THEN** it SHALL resolve draw information
- **AND** resolve session information
- **AND** resolve outlet information
- **AND** resolve terminal label
- **AND** return a `TicketPrintView`

### Requirement: Terminal label only

Printed tickets SHALL display a human terminal label, not a terminal UUID.

#### Scenario: Terminal has label

- **GIVEN** a terminal labeled `POS-001`
- **WHEN** a ticket is printed
- **THEN** the receipt SHALL contain `POS-001`
- **AND** it SHALL NOT contain the terminal UUID
- **AND** it SHALL NOT contain a masked terminal UUID

### Requirement: Explicit print timezone

Ticket print formatting SHALL use an explicit timezone.

#### Scenario: Format sold timestamp

- **GIVEN** a ticket print view with `createdAt`
- **AND** a print timezone
- **WHEN** the receipt is formatted
- **THEN** the timestamp SHALL be formatted using the print timezone
- **AND** `ZoneId.systemDefault()` SHALL NOT be used

### Requirement: Receipt line grouping

Ticket receipt lines SHALL be grouped by `gameCode + betType + betOption`.

#### Scenario: Same bet type across different games

- **GIVEN** ticket lines with the same bet type but different game codes
- **WHEN** the receipt is formatted
- **THEN** the lines SHALL appear in separate sections

### Requirement: Dynamic PDF height

Ticket PDF height SHALL be dynamic.

#### Scenario: Large ticket

- **GIVEN** a receipt model with many lines
- **WHEN** the PDF is built
- **THEN** the page height SHALL grow to include all lines
- **AND** lines SHALL NOT be silently cut because of a fixed height

### Requirement: Optional QR code

Ticket PDF generation SHALL tolerate missing QR bytes.

#### Scenario: QR missing

- **GIVEN** a receipt model
- **AND** null or empty QR bytes
- **WHEN** the PDF is built
- **THEN** PDF generation SHALL succeed
- **AND** no QR image SHALL be rendered

### Requirement: Print endpoints

The tenant ticket print surface SHALL expose PDF and ESC/POS endpoints.

#### Scenario: PDF print

- **WHEN** `GET /tenant/tickets/{ticketId}/print.pdf` is called
- **THEN** the response SHALL be `application/pdf`
- **AND** `Cache-Control: no-store` SHALL be set

#### Scenario: ESC/POS print

- **WHEN** `GET /tenant/tickets/{ticketId}/print.escpos` is called
- **THEN** the response SHALL be `application/octet-stream`
- **AND** `Cache-Control: no-store` SHALL be set

### Requirement: Base64 print endpoint deprecated

The base64 text print endpoint SHOULD be removed or deprecated.

#### Scenario: Legacy print endpoint

- **WHEN** `/tenant/tickets/{ticketId}/print` exists
- **THEN** it SHALL be documented as deprecated
- **AND** new clients SHALL use `/print.pdf` or `/print.escpos`
