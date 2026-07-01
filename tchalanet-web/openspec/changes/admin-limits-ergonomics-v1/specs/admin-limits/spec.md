## ADDED Requirements

### Requirement: Admin limits overview

The admin limits feature SHALL expose an overview at the base limits route so tenant admins can understand the main limit scopes before editing detailed rules.

#### Scenario: Tenant admin opens the limits base route

- **GIVEN** a tenant admin is in the admin portal
- **WHEN** they open `/app/admin/limits`
- **THEN** the page shows an overview of global, seller, number, and channel limit areas
- **AND** each area links to its dedicated detail route
- **AND** the page shows quick links for common tasks such as blocking a number, limiting a draw, and limiting a seller.

### Requirement: Sidebar-owned limit navigation

The admin limits feature SHALL use the private shell sidebar as the primary navigation between limit pages.

#### Scenario: Tenant admin navigates limit sections

- **GIVEN** the sidebar already contains links for global, seller, number, and channel limits
- **WHEN** a tenant admin enters the limits feature
- **THEN** the feature shell does not render a second tab navigation for the same sections.

### Requirement: Business-readable active rules

The admin limits detail pages SHALL render active limit assignments in business-readable cards instead of exposing a dense technical table.

#### Scenario: Tenant admin reviews active global limits

- **GIVEN** global limit assignments exist
- **WHEN** the tenant admin opens the global limits page
- **THEN** each assignment is shown as a card with a readable rule sentence
- **AND** the card shows status, breach action, configured value, and edit/delete actions.

### Requirement: Overview active protection actions

The admin limits overview SHALL let tenant admins act on active global protections without leaving the overview.

#### Scenario: Tenant admin edits or removes an active protection from overview

- **GIVEN** active global protections exist
- **WHEN** the tenant admin opens `/app/admin/limits`
- **THEN** each active protection is shown as a business-readable sentence
- **AND** the tenant admin can modify or remove that protection from the overview
- **AND** the overview refreshes after the action succeeds.
