# sales-receipt-print Specification

## Purpose
TBD - created by archiving change harden-critical-ticket-flows-v1. Update Purpose after archive.
## Requirements
### Requirement: Sales must own canonical receipt content

`core.sales` SHALL own the canonical receipt content used for print, backup, and customer messages.

Features SHALL NOT assemble business receipt content.

#### Scenario: Cashier prints a ticket

Given a sold ticket
When cashier requests print
Then `features.cashier` asks `core.sales` for canonical print content
And `platform.document` renders that content
And `features.cashier` does not decide line labels, promotion labels, totals, or footer/header business content.

### Requirement: TicketPrintDocumentMapper must be technical-only

`TicketPrintDocumentMapper` SHALL only map sales receipt content to `DocumentRenderRequest`.

It MAY decide:

- template key;
- output document format;
- paper options;
- QR document asset mapping;
- line style mapping.

It SHALL NOT decide:

- tenant header;
- outlet header;
- draw label;
- game labels;
- bet option labels;
- promotion display;
- totals;
- money formatting;
- footer content.

#### Scenario: Promotion label appears on receipt

Given a ticket with Maryaj gratuit
When the receipt is rendered
Then the text `Maryaj gratuit` comes from `core.sales` receipt content
And not from `TicketPrintDocumentMapper`.

### Requirement: Receipt content must include tenant and outlet branding

Canonical receipt print content SHALL include:

- tenant display name/header;
- outlet name/header;
- ticket identity;
- public display code;
- sale timestamp;
- terminal and seller when configured;
- draw channel label and scheduled time;
- lines grouped/displayed consistently;
- totals;
- outlet footer;
- tenant footer;
- verification URL/QR.

#### Scenario: Tenant and outlet headers configured

Given tenant receipt header is configured
And outlet receipt header is configured
When a ticket is printed
Then both headers appear in the canonical receipt content in a stable order.

### Requirement: Receipt content must display promotions consistently

Canonical receipt content SHALL display promotion effects from sales snapshots.

#### Scenario: Maryaj gratuit line

Given a ticket line created by FREE_GAME_LINE
When the receipt is formatted
Then the receipt displays the line as promotional
And displays `Maryaj gratuit` or configured promotion label
And does not recalculate eligibility.

#### Scenario: Boosted odds line

Given a ticket line with boosted odds
When the receipt is formatted
Then the receipt displays the final odds/potential payout from the sales snapshot.

#### Scenario: Waived charge

Given a waived SMS charge
When totals are formatted
Then the final total reflects the waived charge
And the receipt can display the waived charge if configured.

### Requirement: Print and send must validate operational context

Cashier print, reprint, and send receipt actions SHALL validate operational context.

Supported seller operations SHALL include:

- SELL;
- PRINT_TICKET;
- REPRINT_TICKET;
- SEND_RECEIPT.

#### Scenario: Missing trusted POS context

Given a cashier request without trusted operational context
When print is requested
Then the request is forbidden.

#### Scenario: Trusted context and open session

Given trusted terminal/outlet/session context
And the session matches the seller
When print is requested
Then print succeeds.

### Requirement: Print history must include operational metadata

`RecordTicketPrintCommand` SHALL capture enough metadata for audit/dispute investigation:

- ticket id;
- format;
- reprint reason;
- actor user id;
- terminal id;
- outlet id;
- sales session id;
- correlation id.

#### Scenario: Reprint with reason

Given a ticket was already printed
When cashier reprints with a reason
Then print history stores actor, terminal, outlet, session, format, correlation, and reason.

### Requirement: Backup must use canonical receipt model

The backup returned by sell SHALL be assembled from the same canonical sales receipt model as print/message.

#### Scenario: Sell returns backup

Given a successful sell
When `SellTicketResult` is returned
Then its backup content is consistent with the printed receipt for the same ticket.

