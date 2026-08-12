# admin-games Delta

## ADDED Requirements

### Requirement: Games overview presents business readiness

The games overview SHALL present each game's sale configuration in business terms: active state,
POS visibility, stake readiness, payout/pricing readiness, and availability by draw.

#### Scenario: Tenant admin reviews game readiness

- **GIVEN** tenant games are loaded
- **WHEN** the admin opens the games page
- **THEN** each game shows whether it is sellable, partial, disabled, or incomplete
- **AND** the status is derived from supported configuration data rather than raw technical codes
- **AND** raw game codes are secondary or fallback display only.

### Requirement: Game settings are organized by admin task

The game configuration dialog SHALL group settings by the task the admin is trying to complete.

#### Scenario: Tenant admin edits a game

- **GIVEN** the admin opens a game settings dialog
- **WHEN** the form is rendered
- **THEN** activation and POS visibility are grouped together
- **AND** stake limits are grouped together
- **AND** payout/pricing settings are grouped together
- **AND** advanced or diagnostic fields are visually separated from common settings.

### Requirement: Maryaj Gratis remains linked, not duplicated

The games page SHALL keep Maryaj Gratis visible where relevant while routing to the dedicated Maryaj
Gratis page for detailed configuration.

#### Scenario: Tenant admin configures Maryaj Gratis from games

- **GIVEN** the tenant supports Maryaj Gratis
- **WHEN** the admin views Maryaj or promotion-related game configuration
- **THEN** a Maryaj Gratis action is available
- **AND** choosing it routes to the dedicated Maryaj Gratis page.

