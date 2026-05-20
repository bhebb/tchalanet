## ADDED Requirements

### Requirement: Public ticket verification is JSON only

Public ticket verification MUST return JSON and MUST NOT expose PDF, ESC/POS, or QR rendering.

#### Scenario: Public code is verified

- **WHEN** a public client verifies a ticket code
- **THEN** the response is a masked JSON response
- **AND** no receipt artifact is generated

### Requirement: Public verification masks internal identifiers

Public verification responses MUST NOT expose internal UUID identifiers.

#### Scenario: Public response is returned

- **THEN** the response does not contain `ticketId`, `drawId`, `tenantId`, `address.id`, or terminal UUID fragments
- **AND** it may expose business-safe values such as public code, draw date, draw channel label/code, and masked outlet view

### Requirement: Public payout status is based on actual ticket state

Public payout status MUST be derived from sale status, result status, settlement status, and winning amount.

#### Scenario: Winning ticket is not settled

- **GIVEN** a ticket with `resultStatus = WON` and `settlementStatus = UNSETTLED`
- **WHEN** public verification is requested
- **THEN** payout status is `WON_UNCLAIMED`

#### Scenario: Winning ticket is settled

- **GIVEN** a ticket with `resultStatus = WON` and `settlementStatus = SETTLED`
- **WHEN** public verification is requested
- **THEN** payout status is `WON_PAID`

#### Scenario: Ticket lost

- **GIVEN** a ticket with `resultStatus = LOST`
- **WHEN** public verification is requested
- **THEN** payout status is `LOST`

### Requirement: Public verification uses core sales query only

`features.ticketverify` MUST consume ticket verification data through `QueryBus` and MUST NOT access repositories, JPA entities, or JDBC directly.

#### Scenario: Public verification service loads a record

- **WHEN** `TicketVerifyService` verifies a code
- **THEN** it sends a core sales query
- **AND** it maps the returned record into a public response
