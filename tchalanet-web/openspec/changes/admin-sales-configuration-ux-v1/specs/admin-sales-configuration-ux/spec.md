# admin-sales-configuration-ux Delta

## ADDED Requirements

### Requirement: Setup distinguishes required readiness from optional operations

The admin setup page SHALL visually separate required sale-readiness configuration from optional or
operational configuration.

#### Scenario: Tenant admin reviews setup

- **GIVEN** a tenant admin opens the setup page
- **WHEN** required and optional cards are rendered
- **THEN** required cards are grouped under a required sale-readiness area
- **AND** optional or operational cards are grouped separately
- **AND** optional or operational cards do not affect required progress.

### Requirement: POS and printing are operational setup guidance

The admin setup page SHALL expose POS and printing configuration as operational guidance without
making it a sale-readiness blocker.

#### Scenario: Tenant has no POS-specific print setup

- **GIVEN** tenant sale readiness is otherwise complete
- **WHEN** POS and printing setup is missing or unknown
- **THEN** the setup page still allows required readiness to be complete
- **AND** the POS / printing card links to the tenant print configuration area.

### Requirement: Configuration ownership remains split by domain

Admin setup SHALL route users to the owning configuration page instead of duplicating forms.

#### Scenario: Tenant admin fixes a setup item

- **GIVEN** a setup card shows a missing or optional configuration item
- **WHEN** the admin chooses its action
- **THEN** the admin is routed to the owning page for that domain
- **AND** setup does not render an inline copy of that domain's full configuration form.

### Requirement: Configuration pages remain reachable after operations

The private admin navigation SHALL keep the sales configuration pages easy to reach after the
operations menu group.

#### Scenario: Tenant admin opens admin navigation

- **GIVEN** the tenant admin is authenticated
- **WHEN** the private admin navigation is rendered
- **THEN** setup, tenant settings, games, draw channels, and seller terminals are discoverable as
configuration surfaces
- **AND** operation pages are not replaced by configuration pages.

