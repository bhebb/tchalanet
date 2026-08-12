# admin-draw-channels-resilience Delta

## ADDED Requirements

### Requirement: Draw channels present sellability in operator terms

The draw channels page SHALL present provider/channel configuration using operator-facing labels and
statuses.

#### Scenario: Tenant admin reviews draw channels

- **GIVEN** draw channel providers are loaded
- **WHEN** the admin opens the draw channels page
- **THEN** provider, draw, schedule, result mode, sale status, and generated draw coverage are visible
- **AND** raw acquisition mode values are translated into business copy
- **AND** provider and draw display names are preserved.

### Requirement: Draw channel actions lead to game availability

The draw channels page SHALL provide a direct path from a draw/channel to the games sold on that draw.

#### Scenario: Tenant admin fixes incomplete draw sale setup

- **GIVEN** a draw channel is incomplete because some games are not sellable on it
- **WHEN** the admin chooses the configuration action
- **THEN** the admin is routed to the game/channel matrix or equivalent focused configuration surface
- **AND** the route preserves enough context to understand which draw/channel needs attention.

### Requirement: Draw channel incomplete states avoid raw technical codes

The draw channels page SHALL map missing or partial configuration reasons to local i18n copy.

#### Scenario: Draw channel has incomplete configuration

- **GIVEN** the backend returns reason codes for missing or partial draw-channel setup
- **WHEN** the admin page renders the incomplete state
- **THEN** the page shows localized operator-facing copy
- **AND** the raw reason code is not shown as the primary message.

