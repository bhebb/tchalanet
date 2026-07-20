## ADDED Requirements

### Requirement: Complete a confirmed sale without resubmission

The mobile POS MUST show a completion surface after a successful prepared-sale
confirmation. It MUST print using the confirmed ticket identifier and MUST NOT
send lines or a sale payload again.

#### Scenario: Initial print

- **WHEN** the preparation confirmation returns a sold ticket
- **THEN** the mobile client initiates the print action for that ticket
- **AND** it does not issue a second confirm or sell request

### Requirement: Localized completion content

The completion surface MUST resolve its seller-facing text from the active
HT/FR/EN catalog.

#### Scenario: Haitian Creole default

- **WHEN** the active locale is Haitian Creole
- **THEN** success, code-copy, print, and new-ticket labels are Haitian Creole
