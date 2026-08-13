# admin-sales-configuration-ux Delta

## ADDED Requirements

### Requirement: Setup distinguishes required readiness from optional operations

The admin setup page SHALL visually separate required sale-readiness configuration from optional or
operational configuration while preserving existing backend readiness semantics.

#### Scenario: Tenant admin reviews setup

- **GIVEN** a tenant admin opens the setup page
- **WHEN** required and optional cards are rendered
- **THEN** required cards are grouped under a required sale-readiness area
- **AND** optional or operational cards are grouped separately
- **AND** optional or operational cards do not affect required progress.

#### Scenario: Operational configuration is missing

- **GIVEN** POS/printing, limits, commission, subscription, notifications, or Maryaj Gratis are not configured
- **WHEN** required sale-readiness cards are complete
- **THEN** setup still communicates that the tenant is ready to sell
- **AND** operational items remain visually secondary to blockers.

### Requirement: Setup summary answers sale readiness

The setup summary SHALL clearly communicate whether the tenant is ready to sell and, when it is not,
how many blocking items remain.

#### Scenario: Tenant is ready to sell

- **GIVEN** all blocking sale-readiness items are complete
- **WHEN** the admin opens setup
- **THEN** the summary communicates ready to sell.

#### Scenario: Tenant is not ready to sell

- **GIVEN** one or more blocking sale-readiness items are incomplete
- **WHEN** the admin opens setup
- **THEN** the summary communicates not ready to sell
- **AND** the number of blocking items is visible.

### Requirement: POS and printing are operational setup guidance

The admin setup page SHALL expose POS and printing configuration as operational guidance without
making it a sale-readiness blocker.

#### Scenario: Tenant has no POS-specific print setup

- **GIVEN** tenant sale readiness is otherwise complete
- **WHEN** POS and printing setup is missing or unknown
- **THEN** the setup page still allows required readiness to be complete
- **AND** the POS / printing card shows Configured, Not configured, Recommended, or Not enabled
- **AND** the POS / printing card links to the tenant print configuration area.

### Requirement: Configuration ownership remains split by domain

Admin setup SHALL route users to the owning configuration page instead of duplicating forms.

#### Scenario: Tenant admin fixes a setup item

- **GIVEN** a setup card shows a missing or optional configuration item
- **WHEN** the admin chooses its action
- **THEN** the admin is routed to the owning page for that domain
- **AND** setup does not render an inline copy of that domain's full configuration form.

### Requirement: Corrective actions have deterministic destinations

Every actionable setup warning SHALL expose one primary corrective destination.

#### Scenario: Admin reviews setup warnings

- **GIVEN** setup contains multiple configuration warnings
- **WHEN** the warnings are rendered
- **THEN** each warning exposes one primary action
- **AND** no warning shows multiple competing primary CTAs.

#### Scenario: Specific setup problem has a specific destination

- **GIVEN** a setup problem has a specific owning page
- **WHEN** the admin chooses the corrective action
- **THEN** the admin is not sent to a generic settings page.

### Requirement: Configuration pages remain reachable after operations

The private admin navigation SHALL keep the sales configuration pages easy to reach after the
operations menu group.

#### Scenario: Tenant admin opens admin navigation

- **GIVEN** the tenant admin is authenticated
- **WHEN** the private admin navigation is rendered
- **THEN** setup, tenant settings, games, draw channels, and seller terminals are discoverable as
configuration surfaces
- **AND** operation pages are not replaced by configuration pages.

### Requirement: Admin configuration surfaces use business language

Setup, games, draw-channel, tenant settings, and seller-terminal configuration surfaces SHALL use
business terminology for primary labels and keep technical identifiers secondary.

#### Scenario: Admin reads configuration pages

- **GIVEN** the admin opens a configuration surface
- **WHEN** labels, statuses, and actions are rendered
- **THEN** implementation terms such as provider client, source config, result slot,
tenant-game mapping, generated entity, and BFF are not primary copy
- **AND** technical identifiers are only shown when useful for support or debugging.

### Requirement: Core configuration tasks are responsive

Core admin configuration tasks SHALL remain usable on compact mobile, tablet, and desktop layouts.

#### Scenario: Admin configures at 360 dp

- **GIVEN** the viewport is approximately 360 dp wide
- **WHEN** setup, games, draw-channel, or seller-terminal configuration is used
- **THEN** core actions are accessible without horizontal scrolling
- **AND** cards or sections stack for compact layouts.
