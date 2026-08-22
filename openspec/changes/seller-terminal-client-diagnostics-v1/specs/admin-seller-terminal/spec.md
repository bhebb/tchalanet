# admin-seller-terminal Delta

## ADDED Requirements

### Requirement: Seller terminal pages manage diagnostics

Seller terminal configuration SHALL provide the controls needed to activate,
deactivate, and inspect diagnostics for that terminal.

#### Scenario: Admin enables diagnostics

- **GIVEN** an admin has diagnostics management permission
- **WHEN** they enable diagnostics from a seller terminal page
- **THEN** they must choose a duration and enter a reason
- **AND** the page shows the diagnostics expiry after activation
- **AND** the chosen duration cannot exceed the backend maximum activation duration.

#### Scenario: Admin disables diagnostics

- **GIVEN** diagnostics are active for a seller terminal
- **WHEN** an authorized admin disables diagnostics
- **THEN** the terminal diagnostics policy becomes inactive
- **AND** the page shows diagnostics as off.

### Requirement: Seller terminal pages show recent diagnostic events

Seller terminal pages SHALL show recent sanitized diagnostic events for support
triage without becoming a general log analytics console.

#### Scenario: Admin filters events

- **GIVEN** recent diagnostic events exist for a seller terminal
- **WHEN** an authorized admin opens the diagnostics panel
- **THEN** they can scan recent compact event rows by severity, category, request ID, and ticket ID
- **AND** the event detail view shows only whitelisted sanitized fields.

#### Scenario: User lacks diagnostics permission

- **GIVEN** a user lacks diagnostics read permission
- **WHEN** they open a seller terminal page
- **THEN** diagnostic event data is not requested or displayed.
