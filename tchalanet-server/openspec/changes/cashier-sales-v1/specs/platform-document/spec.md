# Specification Delta — platform.document

## ADDED Requirements

### Requirement: Document platform renders generic receipt content only

`platform.document` SHALL render generic document requests and SHALL NOT contain ticket-specific business formatting.

#### Scenario: Ticket print renders sales-provided content

- **GIVEN** `core.sales` returns `TicketReceiptPrintContent`
- **WHEN** cashier print calls `DocumentApi.render`
- **THEN** `platform.document` SHALL render the supplied title, body lines, and QR asset
- **AND** SHALL NOT compute ticket labels, stake totals, public codes, or game sections

### Requirement: Ticket print supports V1 formats

The document renderer SHALL support V1 requested formats for cashier ticket proof:

```text
PDF
ESC_POS
QR_PNG
```

#### Scenario: PDF ticket print

- **GIVEN** requested format `PDF`
- **WHEN** the document API renders the receipt
- **THEN** it SHALL return `application/pdf` bytes

#### Scenario: ESC_POS ticket print

- **GIVEN** requested format `ESC_POS`
- **WHEN** the document API renders the receipt
- **THEN** it SHALL return printer bytes suitable for the caller to send to the terminal printer

### Requirement: QR payload is caller-provided

`platform.document` SHALL encode the QR payload supplied in the document request without adding ticket-specific signing or transformation in V1.

#### Scenario: Plain verification URL QR

- **GIVEN** the QR payload `app.tchalanet.com/ticket/PSGV-4AXJ`
- **WHEN** the document renderer creates the QR
- **THEN** the QR SHALL encode exactly that payload
