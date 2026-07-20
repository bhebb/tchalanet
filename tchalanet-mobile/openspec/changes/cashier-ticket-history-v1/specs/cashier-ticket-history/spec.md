## ADDED Requirements

### Requirement: Ticket history uses the authoritative POS query filters
The mobile ticket-history surface SHALL request its selected business date and
ticket-code search through `features.pos`. It SHALL NOT filter a fixed local
sample as the source of truth.

#### Scenario: Seller changes the history day
- **WHEN** the seller selects Today or Yesterday
- **THEN** mobile requests the cashier ticket list with matching `fromDate` and
  `toDate` parameters
- **AND** the returned list is rendered without a second date filter.

#### Scenario: Seller searches for a ticket
- **WHEN** the seller submits a ticket search
- **THEN** mobile sends the normalized query as the server-side `q` parameter
- **AND** the current selected date remains part of the query.

### Requirement: Seller-initiated reprints are auditable
The History and ticket-detail reprint actions SHALL show an editable audit
reason pre-filled with a stable seller-request value and common presets.

#### Scenario: Seller starts a reprint
- **WHEN** the seller selects a reprint action
- **THEN** mobile shows an editable reason field pre-filled with
  `SELLER_REQUESTED_REPRINT`
- **AND** the seller may select a common preset or change the field value
- **WHEN** the seller confirms a reason containing at least ten characters
- **THEN** mobile requests `POST /tenant/cashier/tickets/{ticketId}/print`
  with `recordPrint=true` and the confirmed `reprintReason`
- **AND** the returned binary receipt is handed to the native print flow.

### Requirement: Ticket identity remains localized on seller surfaces
Ticket history and ticket detail SHALL use stable result provider and result
slot identifiers where supplied by `features.pos` to resolve the active app
locale. Server display labels remain a fallback only.

#### Scenario: Ticket has stable provider and slot identifiers
- **WHEN** a ticket response contains `resultProvider=NY` and
  `resultSlotKey=NY_MID`
- **THEN** a Haitian-Creole seller sees the localized provider and slot labels
- **AND** the UI does not render the seeded English display label.
