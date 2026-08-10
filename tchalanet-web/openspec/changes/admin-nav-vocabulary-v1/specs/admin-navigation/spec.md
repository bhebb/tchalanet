## ADDED Requirements

### Requirement: Tenant admin navigation prioritizes operations

The tenant admin navigation SHALL present daily operations before setup/configuration.

#### Scenario: Admin opens the drawer on mobile

- **WHEN** the tenant admin drawer renders from either runtime navigation or static fallback
- **THEN** the operations section lists dashboard, draws, sellers, limits, tickets, then reports
- **AND** setup/configuration entries remain in the configuration section.

### Requirement: Tenant admin labels use business vocabulary

Tenant admin navigation labels SHALL prefer borlette business vocabulary over technical terminal
labels when the action is about sellers.

#### Scenario: Seller management appears in the admin menu

- **WHEN** the seller management group is rendered
- **THEN** the visible label communicates sellers/machann first
- **AND** machine/POS terminology is reserved for device configuration labels.

### Requirement: Seller list exposes critical actions on mobile

Tenant admin seller-list cards SHALL expose common security and support actions directly on mobile.

#### Scenario: Admin reviews a seller card on mobile

- **WHEN** the seller list renders a seller card
- **THEN** the card exposes the detail link, ticket link, PIN reset action, and block/unblock action
- **AND** destructive or rare actions may remain in the overflow menu.
