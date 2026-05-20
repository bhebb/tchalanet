## ADDED Requirements

### Requirement: Cashier feature is a BFF only

`features.cashier` MUST provide seller/POS screen orchestration and MUST NOT own sales business rules.

#### Scenario: Cashier sell endpoint is called

- **WHEN** `POST /tenant/cashier/sell` is called
- **THEN** the feature maps the UI request to `core.sales` sell command
- **AND** it does not calculate limits, payouts, pricing, cutoff, or settlement

### Requirement: Cashier sell response MUST be action-oriented

The cashier sell endpoint MUST return enough action links/identifiers for the client to print receipts or trigger allowed communication actions without calling multiple setup services.

#### Scenario: Sale succeeds

- **WHEN** cashier sell succeeds
- **THEN** the response includes ticket identifiers and available receipt/communication actions
- **AND** the client may call receipt endpoints or cashier-approved communication endpoints as follow-up actions

### Requirement: Cashier BFF orchestrates session UX through core

Cashier session endpoints MUST delegate to `core.session`.

#### Scenario: Cashier opens a session

- **WHEN** a cashier session open endpoint is called
- **THEN** the feature sends the appropriate core session command
- **AND** it does not implement session lifecycle invariants

### Requirement: Cashier BFF MUST delegate receipt and communication actions

Cashier endpoints MUST expose UI-friendly receipt/reprint/communication actions without depending on the receipt feature package.

#### Scenario: Cashier requests reprint action

- **WHEN** a cashier reprint action is called
- **THEN** the feature returns an action link to the receipt endpoint or uses the lower-level `core.sales` receipt model plus `common.document`
- **AND** it does not call `features.receipt` or `features.ticketreceipt`

#### Scenario: Cashier sell requests external communication

- **WHEN** cashier sell includes an external message request
- **THEN** the feature uses `common.communication`
- **AND** Spring calls edge-service internally
- **AND** web/mobile does not call edge-service directly
