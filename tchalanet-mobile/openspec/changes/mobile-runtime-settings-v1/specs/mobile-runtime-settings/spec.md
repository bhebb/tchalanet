# Mobile Runtime Bootstrap Requirements

## ADDED Requirements

### Requirement: Mobile consumes server-composed runtime bootstraps

The Mobile application SHALL use `GET /public/runtime/bootstrap` before authentication
and `GET /tenant/runtime/bootstrap` after authentication as its runtime composition
contracts.

The Mobile application SHALL NOT call settings, i18n, theme, entitlement, or
notification catalogs directly to compose runtime state.

#### Scenario: Application starts before authentication

- **GIVEN** no authenticated tenant session exists
- **WHEN** the application initializes its runtime
- **THEN** it loads the global public-safe bootstrap
- **AND** it applies the returned public settings, i18n overrides, navigation,
  readiness, and notices
- **AND** public feature entries provide the public-safe gates

#### Scenario: POS session becomes authenticated

- **GIVEN** a cashier or operator has an authenticated tenant session
- **WHEN** the application initializes the private runtime
- **THEN** it loads the tenant runtime bootstrap
- **AND** tenant-resolved settings, i18n overrides, entitlements, readiness,
  notification summary, and notices replace the public runtime values where
  applicable

#### Scenario: Bootstrap contains a PageModel reference

- **GIVEN** a public or tenant bootstrap contains `pageModelRef`
- **WHEN** mobile maps the bootstrap response
- **THEN** it ignores the PageModel reference
- **AND** feature ViewModels load their data through explicit typed POS Repositories

#### Scenario: Bootstrap contains theme data

- **GIVEN** a public or tenant bootstrap contains theme data
- **WHEN** mobile composes its Material theme
- **THEN** it keeps the single Tchalanet Mobile Material 3 theme
- **AND** it does not dynamically apply tenant theme data in V1

### Requirement: Mobile runtime bootstrap fails safely

The Mobile application SHALL expose bootstrap data through typed app-scoped state and
use bundled or declared safe fallbacks for missing or unavailable values.

#### Scenario: Missing setting defaults safely

- **GIVEN** a runtime setting is absent or bootstrap is unavailable
- **WHEN** mobile checks the setting
- **THEN** it uses the declared safe default
- **AND** no View calls a settings endpoint directly

#### Scenario: Runtime i18n is unavailable

- **GIVEN** bootstrap i18n messages are absent or unavailable
- **WHEN** mobile renders localized content
- **THEN** it uses bundled translations
- **AND** Haitian Creole remains the startup fallback locale

#### Scenario: POS checks a feature setting

- **GIVEN** the tenant runtime bootstrap has loaded
- **WHEN** a POS feature checks a flag or configuration value
- **THEN** it uses typed app-scoped bootstrap state
- **AND** backend business invariants remain authoritative

#### Scenario: POS checks an authorization gate

- **GIVEN** the tenant runtime bootstrap has loaded
- **WHEN** a ViewModel decides whether an action is available
- **THEN** it uses typed entitlements from app-scoped bootstrap state
- **AND** the backend remains authoritative for authorization

### Requirement: Mobile refreshes authenticated runtime state

During an authenticated session, the Mobile application SHALL use
`GET /tenant/runtime/state` as the lightweight refresh contract. It SHALL NOT poll the
full tenant bootstrap at a fixed interval.

The Mobile application SHALL refresh runtime state after the initial tenant bootstrap,
every 10 minutes while active, when returning to the foreground if the last refresh is
older than 2 minutes, on manual refresh, and after critical actions that can change
runtime availability.

#### Scenario: Runtime version hint changes

- **GIVEN** a tenant bootstrap and its runtime versions are active
- **WHEN** runtime state reports a changed bootstrap, entitlements, i18n, settings,
  navigation, or theme version
- **THEN** mobile requests one fresh tenant bootstrap
- **AND** replaces the affected app-scoped runtime state atomically
- **AND** applies a cooldown so repeated state responses do not create a re-bootstrap
  loop

#### Scenario: Permission is removed during a session

- **GIVEN** the seller is using an action allowed by current entitlements
- **WHEN** runtime state reports a changed entitlements or bootstrap version
- **THEN** mobile refreshes the tenant bootstrap
- **AND** subsequent ViewModel gate decisions use the refreshed entitlements
- **AND** the backend remains authoritative during the refresh window

#### Scenario: Runtime i18n override changes

- **GIVEN** tenant runtime i18n overrides are active
- **WHEN** runtime state reports a changed i18n or bootstrap version
- **THEN** mobile refreshes the tenant bootstrap
- **AND** updates runtime translations without discarding bundled fallbacks

#### Scenario: Runtime versions are unchanged

- **GIVEN** the current tenant bootstrap versions are known
- **WHEN** runtime state reports the same versions
- **THEN** mobile updates lightweight readiness, notifications, blocking, and notices
- **AND** it does not request a full tenant bootstrap

#### Scenario: Runtime-state polling returns notification summary

- **GIVEN** authenticated runtime-state polling is active
- **WHEN** a runtime-state response contains the notification summary
- **THEN** mobile updates the shared notification state
- **AND** it does not start a second periodic notification-summary poll

#### Scenario: Runtime session is no longer usable

- **GIVEN** an authenticated POS session
- **WHEN** runtime state reports `BLOCKED`, `FORCE_RELOAD`, or `SESSION_EXPIRED`
- **THEN** mobile applies the corresponding blocking, single-refresh, or logout flow
- **AND** destructive or unauthorized actions are not left available

#### Scenario: Authenticated session ends

- **GIVEN** runtime-state polling is active
- **WHEN** the user logs out or the session expires
- **THEN** polling is cancelled
- **AND** tenant runtime state is cleared
