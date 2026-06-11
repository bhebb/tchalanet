# Spec: Mobile POS Foundation

## ADDED Requirements

### Requirement: Mobile contracts are typed

The Mobile application SHALL define typed Dart contracts for API responses, errors, session, settings, i18n, theme, and operational context before implementing runtime flows.

#### Scenario: API error maps to ProblemDetail

- **GIVEN** the backend returns a `ProblemDetail` response
- **WHEN** the mobile HTTP client receives it
- **THEN** the mobile app SHALL map it to a typed error model
- **AND** it SHALL avoid untyped error handling in UI widgets.

### Requirement: Mobile auth proves protected POS access

The Mobile application SHALL configure mobile Keycloak/OIDC authentication and prove protected POS dashboard access.

#### Scenario: Anonymous user cannot open POS dashboard

- **GIVEN** no active session exists
- **WHEN** the user opens the POS dashboard route
- **THEN** the app SHALL redirect to login or show an unauthenticated state.

#### Scenario: Authenticated user sees role

- **GIVEN** an authenticated user
- **WHEN** the POS dashboard opens
- **THEN** the dashboard SHALL display the detected role and user/session information.

### Requirement: Tokens are stored securely

The Mobile application SHALL store auth tokens in secure storage and SHALL clear them on logout.

#### Scenario: Logout clears session

- **GIVEN** an authenticated user
- **WHEN** the user taps logout
- **THEN** tokens SHALL be removed from secure storage
- **AND** protected routes SHALL no longer be accessible.

### Requirement: Mobile i18n merges local translations with backend overrides

The Mobile application SHALL merge local mobile translations with backend runtime overrides.

#### Scenario: Backend override wins

- **GIVEN** local translation key `pos.dashboard.title = "Dashboard"`
- **AND** backend override key `pos.dashboard.title = "Tableau POS"`
- **WHEN** translations are merged
- **THEN** the effective translation SHALL be `"Tableau POS"`.

### Requirement: Mobile theme runtime applies Tchalanet default

The Mobile application SHALL apply the Tchalanet default theme at startup and support runtime theme updates.

#### Scenario: Default theme applies at startup

- **GIVEN** no tenant-specific mobile theme is loaded
- **WHEN** the app starts
- **THEN** Tchalanet default theme SHALL be active.

### Requirement: POS operational context is visible early

The Mobile POS dashboard SHALL display operational context readiness for terminal, outlet, and sales session.

#### Scenario: Missing operational context is visible

- **GIVEN** no terminal/outlet/session is attached
- **WHEN** the POS dashboard renders
- **THEN** it SHALL show a clear missing-context state
- **AND** it SHALL not pretend the POS is ready for sensitive operations.

#### Scenario: Ready operational context is visible

- **GIVEN** terminal, outlet, and sales session are attached
- **WHEN** the POS dashboard renders
- **THEN** it SHALL display the terminal, outlet, session, and ready status.

### Requirement: Mobile does not force Web PageModel in V1

The Mobile V1 foundation SHALL NOT require Web PageModel runtime unless a separate accepted requirement introduces dynamic mobile screens.

#### Scenario: POS dashboard boots without PageModel

- **GIVEN** settings, i18n, theme, auth, and operational context are available
- **WHEN** the POS dashboard opens
- **THEN** it SHALL render without requiring a PageModel response.

### Requirement: Mobile dependencies are documented

The Mobile project SHALL document every new package dependency with purpose, category, owner, alternative considered, and removal/replacement trigger.

#### Scenario: New mobile dependency is proposed

- **GIVEN** a package is added to `pubspec.yaml`
- **WHEN** the change is reviewed
- **THEN** dependency documentation SHALL explain why it is needed.
