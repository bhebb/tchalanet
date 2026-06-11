# runtime-bootstrap

## ADDED Requirements

### Requirement: Private runtime state endpoint is lightweight

The system SHALL expose `GET /tenant/runtime/private-state` returning runtime status, readiness,
notification summary, blocking state, and version hints. It SHALL NOT return the full bootstrap
payload (full i18n, theme, navigation, settings, profile, page model, or dashboard data).

#### Scenario: Authenticated user refreshes runtime state

- **WHEN** an authenticated user calls `GET /tenant/runtime/private-state`
- **THEN** the response includes status, readiness, notifications summary, and version hints
- **AND** it does not include full navigation, theme, i18n, or settings

#### Scenario: Cashier session is closed

- **WHEN** the cashier session is closed
- **THEN** private-state returns status `BLOCKED` with a blocking code and a localized title/message key

#### Scenario: Runtime version changed

- **WHEN** entitlements or navigation version has changed since bootstrap
- **THEN** private-state signals `FORCE_RELOAD` so the client re-runs full bootstrap once

### Requirement: Public bootstrap endpoint is unauthenticated and public-safe

The system SHALL expose `GET /public/runtime/public-bootstrap` without authentication, returning public
settings, theme, i18n, navigation, light readiness, and a `pageModelRef`. It SHALL NOT expose user,
entitlements, private navigation, notification summary, or internal readiness.

#### Scenario: Public route bootstraps without login

- **WHEN** a client calls `GET /public/runtime/public-bootstrap` without a token
- **THEN** the response returns public settings, theme, i18n, navigation, readiness, and pageModelRef

#### Scenario: Public bootstrap hides private data

- **WHEN** the public bootstrap response is produced
- **THEN** it contains no user, entitlements, private navigation, notification summary, or internal readiness

### Requirement: Bootstrap responses carry the i18n bundle by surface

Private and public bootstrap responses SHALL include the resolved i18n bundle for the requested
surface, where the surface is selected via request header or query parameter.

#### Scenario: Surface selects the i18n bundle

- **WHEN** a bootstrap request specifies a surface via header or query param
- **THEN** the response i18n bundle contains the keys for that surface

#### Scenario: Language resolution for public i18n

- **WHEN** no explicit language is provided on a public bootstrap request
- **THEN** the language resolves to URL language, then browser language, then default `fr`
