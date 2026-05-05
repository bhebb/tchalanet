# mobile-app Specification

## ADDED Requirements

### Requirement: Standalone Flutter mobile application

The system SHALL provide a standalone Flutter mobile application outside the Nx workspace.

#### Scenario: Flutter app exists at repository root

- **GIVEN** the repository root
- **WHEN** a developer lists project folders
- **THEN** a `tchalanet-mobile/` Flutter application SHALL exist
- **AND** it SHALL not be registered as an Nx project

#### Scenario: Flutter app runs independently

- **GIVEN** a developer enters `tchalanet-mobile/`
- **WHEN** they run `flutter pub get`
- **THEN** dependencies SHALL resolve without requiring Nx

### Requirement: Old Nx mobile app removal

The system SHALL remove the legacy Nx/Ionic/Capacitor mobile application from the active workspace.

#### Scenario: Old mobile project is not listed by Nx

- **GIVEN** the root workspace
- **WHEN** a developer runs `pnpm nx show projects`
- **THEN** the old Ionic/Capacitor mobile app SHALL NOT appear

#### Scenario: Old mobile scripts are removed

- **GIVEN** root `package.json`
- **WHEN** scripts are inspected
- **THEN** no script SHALL target the deleted Ionic/Capacitor mobile project

### Requirement: Repository scan boundary for mobile agents

Agents implementing this change SHALL avoid scanning unrelated server, infra, edge, and web code after the old Nx mobile cleanup is complete.

#### Scenario: Claude finishes Nx cleanup

- **GIVEN** the old mobile app no longer appears in `pnpm nx show projects`
- **WHEN** Claude continues implementation
- **THEN** Claude SHALL work only in `tchalanet-mobile/**`, `VERSIONS.md`, and this OpenSpec folder
- **AND** Claude SHALL NOT scan `tchalanet-server/**`
- **AND** Claude SHALL NOT scan `tchalanet-infra/**`
- **AND** Claude SHALL NOT scan `tchalanet-edge-service/**`
- **AND** Claude SHALL NOT scan `apps/tchalanet-web/**`

#### Scenario: Claude needs auth endpoint details

- **GIVEN** the mobile auth endpoint is unclear
- **WHEN** Claude implements the login boundary
- **THEN** Claude SHALL isolate the uncertainty in `AuthApi`
- **AND** Claude SHALL leave a precise TODO instead of scanning server code
- **AND** Claude SHALL NOT inspect backend code to infer behavior

### Requirement: Flutter architecture is feature-oriented

The Flutter app SHALL use a feature-oriented structure with shared core infrastructure.

#### Scenario: Auth feature is implemented

- **GIVEN** the Flutter app source
- **WHEN** files are inspected under `lib/features/auth/`
- **THEN** auth code SHALL be separated into domain, data, application, and presentation concerns

#### Scenario: Shared HTTP code is centralized

- **GIVEN** Flutter presentation widgets
- **WHEN** login UI code is inspected
- **THEN** widgets SHALL NOT instantiate `Dio` directly
- **AND** widgets SHALL NOT read secure storage directly

### Requirement: Centralized routing

The Flutter app SHALL use centralized routing.

#### Scenario: Unauthenticated user opens protected route

- **GIVEN** no active auth session
- **WHEN** the user attempts to open a protected route
- **THEN** the router SHALL redirect the user to `/login`

#### Scenario: Authenticated user opens login route

- **GIVEN** an active auth session
- **WHEN** the user opens `/login`
- **THEN** the router SHALL redirect the user to `/home`

### Requirement: Centralized auth state

The Flutter app SHALL manage authentication state through an application-level controller/provider.

#### Scenario: App starts

- **GIVEN** the app is launched
- **WHEN** auth state initializes
- **THEN** the app SHALL attempt to restore the previous session from token storage

#### Scenario: Login succeeds

- **GIVEN** valid credentials
- **WHEN** login succeeds
- **THEN** the auth state SHALL become authenticated
- **AND** the token SHALL be persisted through the storage abstraction

#### Scenario: Login fails

- **GIVEN** invalid credentials or a network error
- **WHEN** login fails
- **THEN** the login page SHALL show an error message inside the page
- **AND** the app SHALL remain unauthenticated

### Requirement: Centralized HTTP client

The Flutter app SHALL use a single configured HTTP client for API calls.

#### Scenario: API base URL is configured

- **GIVEN** the app starts with `--dart-define=API_BASE_URL=...`
- **WHEN** the API client is created
- **THEN** it SHALL use that value as its base URL

#### Scenario: Android emulator local backend

- **GIVEN** no explicit API base URL is provided
- **WHEN** the app runs on Android emulator during local development
- **THEN** the default base URL SHALL be `http://10.0.2.2:8080/api/v1`

### Requirement: Secure token storage abstraction

The Flutter app SHALL access persisted tokens only through a storage abstraction.

#### Scenario: Token is stored

- **GIVEN** login succeeds
- **WHEN** the repository persists the access token
- **THEN** it SHALL use `TokenStorage`
- **AND** the presentation layer SHALL NOT write tokens directly

#### Scenario: Logout occurs

- **GIVEN** an authenticated user
- **WHEN** logout is called
- **THEN** token storage SHALL be cleared
- **AND** auth state SHALL become unauthenticated

### Requirement: Login page MVP

The Flutter app SHALL provide a login page as the first MVP screen.

#### Scenario: Login form is displayed

- **GIVEN** the app starts unauthenticated
- **WHEN** the login page is shown
- **THEN** the user SHALL see a username field labelled `Nom d’utilisateur`
- **AND** a password field labelled `Mot de passe`
- **AND** a submit button

#### Scenario: Form validation blocks empty submit

- **GIVEN** empty username or password
- **WHEN** the user attempts to submit
- **THEN** the form SHALL show validation feedback
- **AND** login SHALL NOT be attempted

#### Scenario: Submit shows loading state

- **GIVEN** valid credentials
- **WHEN** login is submitted
- **THEN** the submit button SHALL show a loading state or be disabled while the request is running

### Requirement: Local Android testing documentation

The Flutter mobile app SHALL include local Android setup and run documentation.

#### Scenario: Developer wants to run mobile locally

- **GIVEN** a developer opens the Flutter mobile README or docs
- **WHEN** they follow the local Android instructions
- **THEN** they SHALL find commands for `flutter pub get`, `flutter analyze`, `flutter test`, and `flutter run`
- **AND** they SHALL find guidance for Android emulator API access using `10.0.2.2`

### Requirement: Version source of truth updated

`VERSIONS.md` SHALL document Flutter as the active mobile stack.

#### Scenario: Future agent checks mobile stack

- **GIVEN** a future agent reads `VERSIONS.md`
- **WHEN** they look for the active mobile technology
- **THEN** they SHALL see Flutter/Dart as the active mobile stack
- **AND** the old Ionic/Capacitor mobile app SHALL NOT be documented as active
