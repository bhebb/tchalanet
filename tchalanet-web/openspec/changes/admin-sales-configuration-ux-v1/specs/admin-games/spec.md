# admin-games Delta

## ADDED Requirements

### Requirement: Games overview presents business readiness

The games overview SHALL present each game's sale configuration in business terms: active state,
POS visibility, stake readiness, payout/pricing readiness, and availability by draw.

The UI SHALL preserve backend tenant-game truth: `enabled`, `visibleInPos`, backend display names,
tenant-game IDs, pricing rows, and limit/stake fields are the source data. UI readiness labels MAY
summarize missing configuration, but they SHALL NOT replace a backend-enabled game with a different
activation state.

#### Scenario: Tenant admin reviews game readiness

- **GIVEN** tenant games are loaded
- **WHEN** the admin opens the games page
- **THEN** each game shows whether it is Ready, Needs attention, or Disabled
- **AND** the status is derived from supported configuration data rather than raw technical codes
- **AND** raw game codes are secondary or fallback display only.

#### Scenario: Backend-enabled game has missing auxiliary setup

- **GIVEN** the backend returns a tenant game with `enabled=true`
- **AND** stake limits or pricing need attention
- **WHEN** the admin opens the games page
- **THEN** the game remains presented as enabled/active
- **AND** the missing stake or pricing setup is shown as a separate Needs attention item.

#### Scenario: Tenant admin reviews game card groups

- **GIVEN** a game card is rendered
- **WHEN** the admin scans the card
- **THEN** activation, POS visibility, stake limits, pricing/payout, and advanced options are distinct configuration groups.

### Requirement: Games overview exposes availability by draw

The games overview SHALL expose where each game can currently be sold without requiring admins to
infer availability from technical mappings.

#### Scenario: Tenant admin reviews availability

- **GIVEN** tenant games and draw-channel availability data are loaded
- **WHEN** the games overview renders a game
- **THEN** the card shows the number of draws or channels where the game can currently be sold
- **AND** a Review availability action is available.

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
- **THEN** Maryaj Gratis configuration status is visible
- **AND** a Maryaj Gratis action is available
- **AND** choosing it routes to the dedicated Maryaj Gratis page.

### Requirement: Games page avoids implementation terminology

The games page SHALL prefer business labels over internal entity or mapping terminology.

#### Scenario: Tenant admin scans game labels

- **GIVEN** a game card or game settings dialog is rendered
- **WHEN** the admin reads labels and statuses
- **THEN** labels prefer Available on, Visible on POS, Stake limits, Pricing / payout, and Needs attention
- **AND** internal mapping/entity terminology is not primary copy.
