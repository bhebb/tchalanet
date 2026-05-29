# Spec — terminal-security

## ADDED Requirements

### Requirement: Terminals are first-class security entities

The system SHALL model POS and phone sales surfaces as terminals governed by terminal lifecycle, assignment, binding, and capability rules.

#### Scenario: Physical POS terminal

- **GIVEN** a tenant admin creates a `PHYSICAL + POS` terminal
- **WHEN** the terminal is assigned to a user and the terminal/outlet/session gates are configured
- **THEN** it can be activated through a pairing challenge
- **AND** it cannot sell until an active compatible binding exists

#### Scenario: Virtual phone terminal

- **GIVEN** tenant entitlement allows phone sales
- **WHEN** an admin assigns a `VIRTUAL + MOBILE` terminal to a seller user
- **THEN** the seller may activate it using an approved activation challenge
- **AND** phone sales require terminal capability `SELL_PHONE`

### Requirement: Terminal lifecycle uses separated aggregates

The system SHALL manage terminal identity, user assignment, device binding, and activation challenge as separate lifecycle-bearing aggregates.

#### Scenario: Challenge expires

- **GIVEN** a pending terminal activation challenge
- **WHEN** the challenge expires
- **THEN** the challenge is no longer usable
- **AND** the terminal row is not revoked or deleted solely because the challenge expired

#### Scenario: Device is changed

- **GIVEN** a terminal has an active binding
- **WHEN** the terminal is re-bound to a new device
- **THEN** the previous binding is revoked
- **AND** a new binding is created
- **AND** the binding secret or public key is not mutated in place

### Requirement: Terminal assignment is enforced

The system SHALL reject sensitive operations when the terminal is not actively assigned to the authenticated actor.

#### Scenario: Wrong seller attempts sale

- **GIVEN** terminal T is assigned to seller A
- **WHEN** seller B attempts a sale using terminal T
- **THEN** the request is rejected with an explicit terminal assignment error

### Requirement: Revoked, locked, retired, or inactive terminal cannot transact

The system SHALL reject sensitive operations for terminal statuses other than `ACTIVE`.

#### Scenario: Revoked terminal

- **GIVEN** a terminal was active
- **AND** an admin revokes it
- **WHEN** the seller attempts a sale
- **THEN** the sale is rejected
- **AND** the attempt is audit-visible

### Requirement: Activation challenges are single-use and attempt-limited

The system SHALL store activation codes only as hashes and SHALL prevent reuse or brute force.

#### Scenario: Pairing code mismatch

- **GIVEN** a pending challenge with remaining attempts
- **WHEN** the user submits a wrong code
- **THEN** the attempt count increases
- **AND** the clear code is not logged or stored

#### Scenario: Pairing code succeeds

- **GIVEN** a pending challenge with a matching code
- **WHEN** verification succeeds
- **THEN** the challenge becomes `CONSUMED`
- **AND** the terminal binding is created
- **AND** the same challenge cannot be used again

### Requirement: Challenge delivery is policy-driven

The system SHALL keep terminal challenge state independent from the delivery channel used to send or expose the clear code.

#### Scenario: E2E captures challenge code without SMS

- **GIVEN** the application runs in e2e challenge delivery mode
- **WHEN** a mobile terminal activation challenge is created
- **THEN** the challenge is still persisted with only `codeHash`
- **AND** the clear code is available only through a test-only capture mechanism
- **AND** no SMS is sent

#### Scenario: Live mobile OTP uses SMS only for activation or step-up

- **GIVEN** the application runs in live challenge delivery mode
- **WHEN** a mobile OTP challenge is created for activation, device change, reset binding, fraud suspicion, or risk step-up
- **THEN** the default delivery channel is `SMS`
- **AND** normal refresh-token renewal does not send SMS

### Requirement: Terminal validation keeps independent gates separate

The system SHALL treat user permission, terminal capability, outlet flags, and session validity as independent gates.

#### Scenario: User has permission but terminal lacks capability

- **GIVEN** a user has permission `ticket.sell`
- **AND** the active terminal lacks capability `SELL_TICKET`
- **WHEN** the user attempts a ticket sale
- **THEN** the sale is rejected with a terminal capability error

#### Scenario: Terminal has capability but outlet is disabled

- **GIVEN** a terminal has capability `SELL_TICKET`
- **AND** the outlet does not allow sales
- **WHEN** the user attempts a ticket sale
- **THEN** the sale is rejected by outlet validation

### Requirement: Plan and entitlement limits apply to terminal activation

The system SHALL enforce tenant entitlement features and limits during phone terminal activation and terminal quota-sensitive operations.

#### Scenario: Phone sales not enabled

- **GIVEN** tenant entitlement does not allow phone sales
- **WHEN** an admin attempts to activate a virtual phone terminal
- **THEN** the request is rejected with an entitlement error

#### Scenario: Terminal limit missing

- **GIVEN** a quota-sensitive operation requires a terminal limit
- **AND** the tenant plan does not define that limit
- **WHEN** the operation is evaluated
- **THEN** the system treats it as configuration error or explicit denial according to the entitlement contract
- **AND** it does not interpret missing as `0` or `-1`
