# admin-seller-terminal Delta

## ADDED Requirements

### Requirement: Seller terminals distinguish tenant defaults from overrides

Seller terminal configuration SHALL clearly show whether printer and POS values are inherited from
tenant defaults or overridden for the terminal.

#### Scenario: Terminal uses tenant print default

- **GIVEN** a seller terminal has no terminal-specific printer override
- **WHEN** the admin opens the terminal configuration
- **THEN** the printer value is shown
- **AND** the source is shown as tenant default.

#### Scenario: Terminal overrides paper size

- **GIVEN** a seller terminal has a terminal-specific paper size
- **WHEN** the admin opens the terminal configuration
- **THEN** the paper size is shown
- **AND** the source is shown as a terminal override.

### Requirement: Terminal-specific printer operations stay on seller terminals

Seller terminal pages SHALL remain the owner for device-specific printer options and test print.

#### Scenario: Admin tests a terminal printer

- **GIVEN** an admin is configuring a seller terminal
- **WHEN** they need to test print
- **THEN** the test-print action is available on the seller terminal surface
- **AND** the action is not duplicated as a global tenant settings operation.

