# common-document Specification

## ADDED Requirements

### Requirement: Generic document rendering primitives

`common.document` SHALL contain only technical, domain-neutral document rendering primitives.

#### Scenario: Receipt model is generic

- **WHEN** `ReceiptModel`, `ReceiptLine`, or `ReceiptSpan` is used
- **THEN** these classes SHALL live under `common.document.receipt`
- **AND** they SHALL NOT reference ticket, payout, draw, approval, or tenant-specific business types.

#### Scenario: PDF receipt renderer

- **WHEN** a `ReceiptModel` and optional QR image are provided
- **THEN** `ReceiptPdfRenderer` SHALL render bytes for a PDF receipt
- **AND** it SHALL NOT be named `TicketPdfBuilder`
- **AND** it SHALL NOT import `core.sales`.

#### Scenario: QR renderer

- **WHEN** QR rendering is needed
- **THEN** `QrRenderer` SHALL live under `common.document.qr`
- **AND** renderers MAY support `PNG` and `ESC_POS`.

### Requirement: No business workflows in common.document

`common.document` SHALL NOT own ticket receipt business assembly, payout receipt business assembly, print permissions, or delivery workflows.

#### Scenario: Ticket receipt formatter

- **WHEN** a class uses `TicketPrintView`, `TicketPrintLine`, or `BetType`
- **THEN** it SHALL NOT live in `common.document`
- **AND** it SHALL live near `core.sales` or a feature orchestrator.
