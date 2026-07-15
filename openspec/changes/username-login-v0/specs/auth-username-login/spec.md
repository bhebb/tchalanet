# Specification: Auth username login

## ADDED Requirements

### Requirement: Login accepts email or username

The web login form SHALL accept a single identifier field for `APP_USER` actors.

SellerTerminal POS login SHALL remain the existing Firebase email/PIN flow and SHALL NOT use the
username lookup endpoint.

#### Scenario: Email login bypasses lookup

- **GIVEN** the user enters an identifier containing `@`
- **WHEN** the user submits the login form
- **THEN** the web client SHALL call Firebase email/password sign-in directly
- **AND** it SHALL NOT call the username lookup endpoint.

#### Scenario: Username login resolves email on submit

- **GIVEN** the user enters an identifier without `@`
- **WHEN** the user submits the login form
- **THEN** the web client SHALL call the public username lookup endpoint exactly once for that submit
- **AND** it SHALL call Firebase email/password sign-in with the returned resolved identifier and the user-entered password.

#### Scenario: Login page restore does not lookup username

- **GIVEN** `/login` loads
- **WHEN** Firebase already has a persisted authenticated user
- **THEN** the app SHALL use the existing restore flow
- **AND** it SHALL NOT call the username lookup endpoint during page initialization.

#### Scenario: SellerTerminal login keeps POS exception

- **GIVEN** a SellerTerminal signs in from the POS client
- **WHEN** it authenticates with its Firebase email and PIN
- **THEN** the client SHALL send `X-Tch-Client-Type: POS`
- **AND** it SHALL NOT call the username lookup endpoint.

### Requirement: Username resolution is public but non-enumerating

The backend SHALL expose a public resolver that maps a normalized Tchalanet username
to a provider sign-in identifier without exposing provider-specific field names.

The V0 username SHALL be globally unique, 3 to 40 characters, lowercase with `Locale.ROOT`,
and restricted to `[a-z0-9._-]`.

#### Scenario: Known username

- **GIVEN** an active app user has username `admin`
- **WHEN** the client posts `admin`
- **THEN** the backend SHALL return `resolvedIdentifier` for that app user.

#### Scenario: Unknown username

- **GIVEN** no active app user matches the normalized username
- **WHEN** the client posts that username
- **THEN** the backend SHALL return a generic login failure compatible with the frontend's invalid-credentials message
- **AND** it SHALL NOT reveal whether the username exists.

#### Scenario: Inactive or unlinked username

- **GIVEN** an app user is inactive or has no usable Firebase external identity
- **WHEN** the client posts that user's username
- **THEN** the backend SHALL return the same generic login failure as an unknown username.

#### Scenario: Abuse protection

- **GIVEN** repeated lookup attempts from the same client context
- **WHEN** they exceed the configured limit
- **THEN** the backend SHALL reject or slow down the lookup without querying Firebase.
