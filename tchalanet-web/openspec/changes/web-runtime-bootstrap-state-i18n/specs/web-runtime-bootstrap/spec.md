# web-runtime-bootstrap

## ADDED Requirements

### Requirement: i18n is delivered by bootstrap through a store-backed loader

The web app SHALL load translations from a runtime i18n store filled by the bootstrap response, with
local `fr/en/ht` bundles used only as offline fallback. It SHALL NOT perform a separate `/public/i18n`
merge request.

#### Scenario: Bootstrap fills the i18n store

- **WHEN** a bootstrap response returns an i18n bundle for the active surface
- **THEN** the translate loader resolves keys from the store without an extra i18n HTTP call

#### Scenario: Key missing from the bootstrap bundle

- **WHEN** a requested key is absent from the bootstrap bundle
- **THEN** the loader falls back to the local bundle and, if still missing, a stable key-derived fallback

### Requirement: Public routes bootstrap without Keycloak

Public routes SHALL initialize via `GET /public/runtime/public-bootstrap` without triggering Keycloak login,
then render the public shell and load the PageModel from `pageModelRef`.

#### Scenario: Public route loads

- **WHEN** a user opens a public route
- **THEN** the app calls public bootstrap, applies public theme and i18n, renders the public shell, and loads the PageModel from `pageModelRef.endpoint`
- **AND** it does not call Keycloak login, private bootstrap, or private-state

#### Scenario: Public bootstrap fails

- **WHEN** public bootstrap fails
- **THEN** the app shows a fallback public state instead of crashing

### Requirement: Private session polls lightweight runtime state

After private bootstrap the app SHALL poll `GET /tenant/runtime/private-state` to refresh notifications,
readiness, and blocking state, and SHALL call full bootstrap again only on version change or
`FORCE_RELOAD`.

#### Scenario: Polling cadence

- **WHEN** a private session is active
- **THEN** the monitor polls private-state every 10 minutes, forces a refresh at 30 minutes, and refreshes on tab focus when the last check is older than 2 minutes

#### Scenario: Version change triggers a single reload

- **WHEN** private-state reports a changed runtime version or `FORCE_RELOAD`
- **THEN** the app calls full private bootstrap once (with cooldown) and does not loop on failure

#### Scenario: Logout stops polling

- **WHEN** the user logs out
- **THEN** private-state polling stops

### Requirement: Blocking state disables risky actions

When private-state returns `BLOCKED` the app SHALL show a shell blocking banner/overlay and disable
risky actions while keeping logout, profile, help, and safe navigation available.

#### Scenario: Cashier is blocked

- **WHEN** private-state returns `BLOCKED` for a cashier
- **THEN** sell, print, and payout actions are disabled and a blocking message is shown
- **AND** logout and help remain available
