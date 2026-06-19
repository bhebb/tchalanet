## ADDED Requirements

### Requirement: Superadmin Tenant Provisioning Page

The web platform superadmin tenant provisioning route SHALL render a functional provisioning form instead of a placeholder.

#### Scenario: Provisioning form is available

- **GIVEN** a superadmin opens the platform tenant provisioning page
- **THEN** the page displays tenant identity, regional settings, provisioning profile, and initial admin sections
- **AND** the page uses the shared admin page shell
- **AND** the page does not introduce a nested router outlet

#### Scenario: Valid tenant request can be previewed and submitted

- **GIVEN** the tenant provisioning form has valid values
- **WHEN** the form changes
- **THEN** the page requests a debounced provisioning preview without blocking editing
- **AND** preview loading or error state is visible in the side panel
- **WHEN** the superadmin submits the form
- **THEN** the page calls the platform provisioning API
- **AND** submit loading or error state is visible
- **AND** successful provisioning displays confirmation without resetting the form

### Requirement: Reusable Admin Detail Components

The web platform SHALL provide reusable admin UI primitives for complex admin forms.

#### Scenario: Detail layout supports main, aside, and footer slots

- **GIVEN** a complex admin page uses the detail layout component
- **THEN** the component arranges main content, aside content, and footer actions responsively
- **AND** the component uses tokenized styles with local `--comp-*` variables and `--tch-*` fallbacks

#### Scenario: Tenant identity and health are visible

- **GIVEN** a complex admin page renders a side panel
- **THEN** the identity card can display an eyebrow, title, code, status, icon, and metadata
- **AND** the provisioning health card can display readiness, progress, loading, error, and status rows
- **AND** brand colors are referenced through Tchalanet design tokens rather than hardcoded brand hex values
