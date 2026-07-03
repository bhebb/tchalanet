# admin-games Specification

## Purpose
TBD - created by archiving change admin-games-channel-matrix-v1. Update Purpose after archive.
## Requirements
### Requirement: Tenant admin games navigation

The tenant admin private navigation SHALL expose available games as a first-class section with a
games overview and a channel/game matrix.

#### Scenario: Tenant admin opens games navigation

- **GIVEN** a tenant admin is authenticated in the admin portal
- **WHEN** the private shell navigation is loaded
- **THEN** an available games section is visible
- **AND** it contains entries for the games overview and the channel/game matrix.

### Requirement: Canonical admin games routes

The admin games feature SHALL use `/app/admin/games` for the games overview and
`/app/admin/games/channel-matrix` for the channel/game matrix, while preserving legacy route
compatibility through redirects.

#### Scenario: Tenant admin opens canonical games routes

- **GIVEN** a tenant admin opens `/app/admin/games`
- **WHEN** the route resolves
- **THEN** the games overview page is shown.

#### Scenario: Tenant admin opens legacy game matrix route

- **GIVEN** a tenant admin opens a legacy game matrix route
- **WHEN** the route resolves
- **THEN** the admin portal redirects to `/app/admin/games/channel-matrix`.

### Requirement: Maryaj gratis remains owned by its dedicated page

The games overview and channel matrix SHALL link to the existing Maryaj gratis configuration page
instead of duplicating that workflow.

#### Scenario: Tenant admin configures Maryaj gratis from games

- **GIVEN** a tenant admin is on the games overview or channel matrix
- **WHEN** they choose a Maryaj gratis configuration action
- **THEN** they are routed to the dedicated Maryaj gratis page.

### Requirement: Games overview highlights setup completeness

The games overview SHALL show operational readiness using supported/active games, ready channels,
missing game-channel stake combinations, and per-game channel readiness.

#### Scenario: Tenant admin reviews game setup gaps

- **GIVEN** the tenant has games and channels configured
- **WHEN** the admin opens the games overview
- **THEN** the page shows readiness indicators
- **AND** incomplete setup items are grouped in an operator-facing completion list.

### Requirement: Channel matrix exposes tenant game settings

The channel/game matrix SHALL clarify that channels own offered/active games while tenant game
settings own POS visibility and stake limits.

#### Scenario: Tenant admin configures stake settings from the matrix

- **GIVEN** a tenant admin opens the channel/game matrix
- **WHEN** they choose to configure a game/channel combination
- **THEN** the tenant game settings dialog opens
- **AND** the labels describe POS visibility, per-line minimum stake, and per-line maximum stake in operator-facing terms.

