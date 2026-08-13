# admin-draw-channels-resilience Delta

## ADDED Requirements

### Requirement: Draw channels present sellability in operator terms

The draw channels page SHALL present provider/channel configuration using operator-facing labels and
statuses.

The draw-channel UX SHALL consume backend channel/game availability data. It SHALL preserve backend
provider/channel display names and stable codes, and it SHALL NOT use local mock data as the source
of sale-readiness truth.

#### Scenario: Tenant admin reviews draw channels

- **GIVEN** draw channel providers are loaded
- **WHEN** the admin opens the draw channels page
- **THEN** result source, draw, draw time, sales close, result mode, sale status, available games, and upcoming draws are visible
- **AND** raw acquisition mode values are translated into business copy
- **AND** provider and draw display names are preserved.

#### Scenario: Backend returns a channel/game matrix

- **GIVEN** the backend returns draw channels and channel-game availability
- **WHEN** the draw-channel UX derives sale coverage
- **THEN** channel active state, channel display name, offered games, enabled channel games, and POS visibility come from backend data
- **AND** the UI uses fallback codes only when display labels are missing.

### Requirement: Draw channel sale status is separate from result mode

Draw channel UI SHALL model sale readiness separately from automatic/manual result-source mode.

#### Scenario: Manual draw channel is ready

- **GIVEN** a manual draw channel has available games and upcoming draws
- **WHEN** the draw channel is rendered
- **THEN** its primary sale status is Ready
- **AND** Manual is shown as a secondary result-source attribute.

#### Scenario: Automatic draw channel is incomplete

- **GIVEN** an automatic draw channel has no available games or no upcoming draws
- **WHEN** the draw channel is rendered
- **THEN** its primary sale status is Needs attention
- **AND** Automatic is shown as a secondary result-source attribute.

#### Scenario: Disabled draw channel

- **GIVEN** a draw channel is disabled
- **WHEN** the draw channel is rendered
- **THEN** its primary sale status is Disabled.

### Requirement: Draw channel actions lead to game availability

The draw channels page SHALL provide a direct path from a draw/channel to the games sold on that draw.

#### Scenario: Tenant admin fixes incomplete draw sale setup

- **GIVEN** a draw channel is incomplete because some games are not sellable on it
- **WHEN** the admin chooses the configuration action
- **THEN** the admin is routed to the game/channel matrix or equivalent focused configuration surface
- **AND** the route preserves enough context to understand which draw/channel needs attention.

### Requirement: Missing sellable coverage is differentiated

The draw channels page SHALL distinguish a configured channel with no upcoming/generated draws from
a configured channel with upcoming draws but no available games.

#### Scenario: No upcoming generated draws

- **GIVEN** a configured draw channel has games available but no upcoming/generated draws
- **WHEN** the page renders the channel
- **THEN** the channel shows Needs attention
- **AND** the primary corrective action is Review schedule.

#### Scenario: Upcoming draws exist but no games are available

- **GIVEN** a configured draw channel has upcoming/generated draws but no games available for sale
- **WHEN** the page renders the channel
- **THEN** the channel shows Needs attention
- **AND** the primary corrective action is Configure game availability.

### Requirement: Draw channel incomplete states avoid raw technical codes

The draw channels page SHALL map missing or partial configuration reasons to local i18n copy.

#### Scenario: Draw channel has incomplete configuration

- **GIVEN** the backend returns reason codes for missing or partial draw-channel setup
- **WHEN** the admin page renders the incomplete state
- **THEN** the page shows localized operator-facing copy
- **AND** the raw reason code is not shown as the primary message.

### Requirement: Optional channel coverage does not redefine tenant readiness

Draw-channel UX SHALL not mark the whole tenant unready solely because an individual optional channel
has no sale coverage unless existing backend readiness semantics already do so.

#### Scenario: Optional channel has no coverage

- **GIVEN** an optional draw channel has no useful sale coverage
- **AND** tenant readiness is otherwise complete according to existing backend semantics
- **WHEN** the draw-channel page renders
- **THEN** the channel shows Needs attention
- **AND** tenant readiness semantics are not changed by the UI.
