# Spec — transaction-security

## ADDED Requirements

### Requirement: Ticket sale requires trusted operational context

The ticket sale use case SHALL reject requests without trusted operational context.

#### Scenario: Sale with client claim only

- **GIVEN** a valid Keycloak token
- **AND** `X-Terminal-Id` is sent without valid binding
- **WHEN** the user attempts to sell a ticket
- **THEN** the sale is rejected

### Requirement: Ticket sale requires idempotency

`POST /tenant/tickets` SHALL require `Idempotency-Key`.

#### Scenario: Missing idempotency key

- **GIVEN** a valid sale request
- **WHEN** `Idempotency-Key` is missing
- **THEN** the API returns `400 idempotency.missing`

#### Scenario: Same key same payload

- **GIVEN** a sale request with key K and payload P
- **WHEN** the client retries with key K and same payload P
- **THEN** the same ticket result is returned

#### Scenario: Same key different payload

- **GIVEN** a completed sale with key K and payload P1
- **WHEN** the client sends key K with payload P2
- **THEN** the API returns `409 idempotency.payload_mismatch`

### Requirement: Session compatibility is enforced

The system SHALL validate that the session matches terminal, outlet and seller.

#### Scenario: Terminal/session mismatch

- **GIVEN** an open session for terminal A
- **WHEN** a sale is attempted with terminal B
- **THEN** the sale is rejected

### Requirement: Sensitive operations are audited

The system SHALL audit terminal lifecycle, operational context selection, ticket sale, payout and offline sync actions.

#### Scenario: Terminal activation audit

- **GIVEN** a terminal activation succeeds
- **THEN** an audit record is produced with actor, tenant, terminal id and activation channel

