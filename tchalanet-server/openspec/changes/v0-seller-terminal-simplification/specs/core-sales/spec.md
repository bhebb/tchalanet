# Spec: core-sales

## MODIFIED Requirements

### Requirement: Sale execution uses seller-terminal operational identity

Core sales SHALL resolve V0 sell-time operational identity from `SellerTerminalId`, not from separate seller, old terminal, outlet or sales-session identity.

#### Scenario: Sale persists seller terminal identity

- **GIVEN** an active seller terminal belongs to the current tenant
- **WHEN** a ticket is sold
- **THEN** the persisted ticket references `seller_terminal_id`.

#### Scenario: Sale snapshots seller terminal facts

- **GIVEN** a ticket is sold through seller terminal `ST-001`
- **WHEN** the ticket is persisted
- **THEN** the ticket or ticket lines snapshot seller terminal code/display name, commission, odds and limit-policy facts needed for historical reporting.

#### Scenario: Missing seller terminal blocks sale

- **GIVEN** the operational context cannot resolve a seller terminal
- **WHEN** a sell command is handled
- **THEN** the sale is rejected before ticket persistence.

#### Scenario: Sale command does not require outlet or session

- **GIVEN** an authenticated seller-terminal POS actor
- **WHEN** it sells a ticket
- **THEN** the sell command is accepted with tenant, seller terminal, lines and idempotency facts
- **AND** it does not require outlet id or sales-session id.

#### Scenario: Ticket queries can aggregate by seller terminal

- **GIVEN** tickets are persisted with `seller_terminal_id`
- **WHEN** reporting queries aggregate by seller terminal and sold date
- **THEN** the backend can use an index keyed by tenant, seller terminal and sold timestamp.

### Requirement: Sell-ticket idempotency remains active

`POST /tenant/tickets` and equivalent sell-ticket command handling SHALL keep idempotency protection in V0.

#### Scenario: Duplicate sell request is idempotent

- **GIVEN** a sell request has already succeeded for an idempotency key
- **WHEN** the same request is retried with the same key
- **THEN** the backend returns the original outcome or rejects the duplicate according to the existing idempotency contract
- **AND** it does not create a second ticket.

#### Scenario: Idempotency hash excludes retired operational facts

- **GIVEN** outlet and sales session are not V0 sell-time facts
- **WHEN** the sell request hash is computed
- **THEN** it does not include outlet id or sales-session id.

### Requirement: Ticket events carry seller-terminal identity

Ticket domain events used by active projectors SHALL carry `SellerTerminalId` instead of old seller, terminal, outlet or session identity.

#### Scenario: Ticket sold event includes seller terminal

- **GIVEN** a ticket is sold
- **WHEN** `TicketSoldEvent` is published
- **THEN** the event includes tenant id, ticket id and seller terminal id.

#### Scenario: Retired projectors are parked

- **GIVEN** a projector only supports payout, ledger, offline sync, autonomy, outlet or session reporting
- **WHEN** V0 seller-terminal simplification is implemented
- **THEN** the projector is removed, disabled or documented as parked for V1+.
