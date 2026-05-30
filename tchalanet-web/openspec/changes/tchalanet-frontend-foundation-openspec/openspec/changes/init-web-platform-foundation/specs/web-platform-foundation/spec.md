# Spec: Web Platform Foundation

## ADDED Requirements

### Requirement: Web contracts are typed before runtime services

The Web application SHALL define typed frontend contracts for API responses, errors, navigation, actions, settings, i18n, theme, PageModel, session, and operational context before implementing runtime services that consume them.

#### Scenario: API success response is typed

- **GIVEN** a backend endpoint returns a successful JSON response
- **WHEN** the frontend API client consumes it
- **THEN** the response SHALL be represented as `ApiResponse<T>`
- **AND** notices and service statuses SHALL be typed.

#### Scenario: API error response is typed

- **GIVEN** a backend endpoint returns a 4xx or 5xx error
- **WHEN** the frontend error mapper handles it
- **THEN** the error SHALL be represented as `ProblemDetail`
- **AND** it SHALL NOT be wrapped as `ApiResponse`.

### Requirement: Auth proof protects role-specific dashboards

The Web application SHALL configure Keycloak authentication and prove access control through protected role-specific dashboard routes.

#### Scenario: Anonymous user cannot access protected dashboard

- **GIVEN** an anonymous user
- **WHEN** the user opens `/app/admin`
- **THEN** the app SHALL redirect to login or block access.

#### Scenario: Wrong role is forbidden

- **GIVEN** an authenticated user without `TENANT_ADMIN`
- **WHEN** the user opens `/app/admin`
- **THEN** the app SHALL show `/forbidden` or equivalent forbidden state.

#### Scenario: Correct role sees dashboard

- **GIVEN** an authenticated user with `TENANT_ADMIN`
- **WHEN** the user opens `/app/admin`
- **THEN** the dashboard SHALL render
- **AND** it SHALL display the detected user and role information from Keycloak/session state.

### Requirement: Runtime bootstrap separates settings, i18n, theme, and PageModel

The Web application SHALL bootstrap runtime UI with four distinct capabilities: settings, i18n, theme, and PageModel.

#### Scenario: Public home bootstrap loads four capabilities

- **GIVEN** the user opens the public home route
- **WHEN** the public bootstrap starts
- **THEN** the app SHALL request settings, i18n overrides, theme, and PageModel as separate capabilities
- **AND** PageModel SHALL NOT contain complete settings, translation bundles, or theme definitions.

### Requirement: i18n merges frontend local translations with backend overrides

The Web application SHALL merge local frontend translations with backend runtime overrides.

#### Scenario: Backend override wins

- **GIVEN** local translation key `public.home.title = "Bienvenue"`
- **AND** backend override key `public.home.title = "Bienvenue sur Tchalanet"`
- **WHEN** translations are merged
- **THEN** the effective translation SHALL be `"Bienvenue sur Tchalanet"`.

#### Scenario: Local fallback remains

- **GIVEN** a local translation key exists
- **AND** backend does not return an override for that key
- **WHEN** translations are merged
- **THEN** the local value SHALL remain effective.

### Requirement: Runtime settings provide V1 feature toggles

The Web application SHALL use runtime settings as the V1 feature-toggle/config mechanism until a future Unleash integration is introduced.

#### Scenario: Feature flag defaults safely

- **GIVEN** a feature flag is missing from backend settings
- **WHEN** the UI checks the feature flag
- **THEN** the UI SHALL use a safe default
- **AND** it SHALL NOT fail open for sensitive functionality.

### Requirement: Theme runtime applies Tchalanet default and presets

The Web application SHALL define Tchalanet default theme and prepare selectable Material-equivalent presets.

#### Scenario: Default theme applies

- **GIVEN** no tenant-specific theme is selected
- **WHEN** the application starts
- **THEN** the Tchalanet default theme SHALL be active.

#### Scenario: Preset can be applied without rebuild

- **GIVEN** a theme preset is selected at runtime
- **WHEN** the theme service applies it
- **THEN** visible tokens SHALL update without requiring a frontend rebuild.

### Requirement: UI core is minimal before real business pages

The Web application SHALL initially implement only generic UI components required for runtime proof and defer richer business components until real tenant admin/POS pages need them.

#### Scenario: Deferred components are not built prematurely

- **GIVEN** no tenant admin list page exists yet
- **WHEN** implementing the initial UI core
- **THEN** `TchDataTable` and `TchPagedList` MAY be deferred.

### Requirement: Dependencies are documented

The Web project SHALL document each new dependency with purpose, owner, category, alternative considered, and removal/replacement trigger.

#### Scenario: New dependency is proposed

- **GIVEN** a developer adds a new package to `package.json`
- **WHEN** the change is reviewed
- **THEN** the dependency governance document SHALL explain why it is needed.

