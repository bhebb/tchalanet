# Spec — transaction-security

## ADDED Requirements

### Requirement: Ticket sale requires trusted operational context

The ticket sale use case SHALL reject requests without trusted operational context.

#### Scenario: Sale with client claim only

- **GIVEN** a valid Keycloak token
- **AND** `X-Terminal-Id` is sent without valid binding or server-side selection
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

The system SHALL validate that the session matches terminal, outlet, tenant, and actor where the operation requires a sales session.

#### Scenario: Terminal/session mismatch

- **GIVEN** an open session for terminal A
- **WHEN** a sale is attempted with terminal B
- **THEN** the sale is rejected

#### Scenario: Phone sale session

- **GIVEN** a seller uses a trusted `VIRTUAL + MOBILE` terminal
- **WHEN** the seller attempts a phone sale
- **THEN** the system validates the phone-compatible operational context and permission
- **AND** applies entitlement before ticket creation

### Requirement: Seller identity is resolved server-side

The system SHALL resolve the business seller from the authenticated user and trusted operational outlet/session before persisting a sale.

#### Scenario: User has no seller profile

- **GIVEN** an authenticated user with a trusted terminal context
- **AND** no active seller profile is linked to the user
- **WHEN** the user attempts to sell a ticket
- **THEN** the sale is rejected with a seller error

#### Scenario: Seller is not assigned to outlet

- **GIVEN** a seller linked to the authenticated user
- **AND** the trusted operational outlet is not actively assigned to that seller
- **WHEN** the seller attempts a sale
- **THEN** the sale is rejected

#### Scenario: Seller is resolved for sale

- **GIVEN** a trusted operational context for outlet O
- **AND** an active seller linked to the authenticated user is assigned to outlet O
- **WHEN** the seller sells a ticket
- **THEN** the ticket snapshots `sellerId` and `sellerAssignmentId`

### Requirement: Sensitive operations are audited

The system SHALL audit terminal lifecycle, activation, operational context selection, ticket sale, payout, offline sync, and denied sensitive attempts where security posture requires visibility.

#### Scenario: Terminal activation audit

- **GIVEN** a terminal activation succeeds
- **THEN** an audit record is produced with actor, tenant, terminal id, activation channel, and outcome

#### Scenario: Denied terminal sale audit

- **GIVEN** a user attempts a sale through a revoked or wrongly assigned terminal
- **WHEN** the operation is denied
- **THEN** the denial is audit-visible without logging secrets or raw binding tokens
