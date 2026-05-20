# core-sales Specification

## MODIFIED Requirements

### Requirement: Ticket receipt model remains sales-owned

`core.sales` SHALL own the canonical ticket receipt/read model and ticket-specific receipt formatting.

#### Scenario: Ticket-specific formatter

- **WHEN** a formatter uses `TicketPrintView`, `TicketPrintLine`, `BetType`, ticket code, public code, terminal, outlet, draw labels, or Haitian game code labels
- **THEN** it SHALL live in `core.sales.application.receipt`
- **AND** SHALL output generic `common.document.receipt.ReceiptModel`.

#### Scenario: Generic renderer

- **WHEN** PDF, QR, or ESC/POS bytes are rendered
- **THEN** `core.sales` SHALL delegate to `common.document`
- **AND** SHALL NOT own generic PDF/QR renderer classes.
