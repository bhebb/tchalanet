# Spec — terminal-security

## ADDED Requirements

### Requirement: Terminals are first-class security entities

The system SHALL model POS and phone sales surfaces as terminals.

#### Scenario: Physical POS terminal

- **GIVEN** a tenant admin creates a `PHYSICAL_POS` terminal
- **WHEN** the terminal is assigned to a seller and outlet
- **THEN** it can be activated through a pairing challenge
- **AND** it cannot sell until an active binding exists

#### Scenario: Virtual phone terminal

- **GIVEN** tenant entitlement `PHONE_SALES_ENABLED=true`
- **WHEN** an admin assigns a `VIRTUAL_PHONE` terminal to a seller
- **THEN** the seller may activate it using an approved activation challenge
- **AND** phone sales require `ticket.sell.phone`

### Requirement: Terminal assignment is enforced

The system SHALL reject sensitive operations when the terminal is not actively assigned to the authenticated user.

#### Scenario: Wrong seller attempts sale

- **GIVEN** terminal T is assigned to seller A
- **WHEN** seller B attempts a sale using terminal T
- **THEN** the request is rejected

### Requirement: Revoked or locked terminal cannot transact

The system SHALL reject sensitive operations for terminal statuses `LOCKED`, `REVOKED`, or `EXPIRED`.

#### Scenario: Revoked terminal

- **GIVEN** a terminal was active
- **AND** an admin revokes it
- **WHEN** the seller attempts a sale
- **THEN** the sale is rejected
- **AND** the attempt is audit-visible

### Requirement: Plan limits apply to terminal activation

The system SHALL enforce tenant plan/entitlement limits during terminal creation or activation.

#### Scenario: Phone sales not enabled

- **GIVEN** tenant does not have `PHONE_SALES_ENABLED`
- **WHEN** an admin attempts to activate a virtual phone terminal
- **THEN** the request is rejected with a plan/entitlement error

