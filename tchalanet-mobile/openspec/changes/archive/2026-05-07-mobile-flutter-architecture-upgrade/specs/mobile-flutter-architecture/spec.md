# Specification: mobile-flutter-architecture

## ADDED Requirements

### Requirement: Standalone Flutter SDK baseline

The mobile application SHALL use the latest stable Flutter SDK available to the developer after running `flutter channel stable` and `flutter upgrade`, or after installing a clean stable SDK if the existing local SDK is outdated or broken.

#### Scenario: Developer has an old Flutter SDK

- **GIVEN** the developer machine has an old Flutter SDK
- **WHEN** implementing this change
- **THEN** Claude SHALL instruct the developer to run `flutter channel stable`, `flutter upgrade`, and `flutter doctor -v`
- **AND** Claude SHALL record the resulting Flutter and Dart versions in `VERSIONS.md`.

#### Scenario: Existing SDK is broken

- **GIVEN** `flutter doctor -v` shows a broken or unusable SDK
- **WHEN** implementation starts
- **THEN** Claude SHALL recommend installing a clean stable SDK under a new tools directory
- **AND** SHALL not generate app code against an unknown broken SDK state.

### Requirement: Official Flutter MVVM baseline

The mobile application SHALL follow Flutter official architecture guidance using MVVM with Views and ViewModels in the UI/application layer, and repositories/services in the data/model layer.

#### Scenario: Login page is implemented

- **GIVEN** the login page exists
- **WHEN** the user enters credentials and submits
- **THEN** the View SHALL delegate behavior to `AuthViewModel`
- **AND** `AuthViewModel` SHALL call `AuthRepository`
- **AND** the View SHALL not call Dio or secure storage directly.

### Requirement: Feature-first structure

The mobile application SHALL organize feature code by bounded feature under `lib/features/<feature>/`.

#### Scenario: Auth feature exists

- **GIVEN** the auth feature is implemented
- **WHEN** viewing the source tree
- **THEN** auth code SHALL be organized under `features/auth/application`, `features/auth/data`, `features/auth/domain`, and `features/auth/presentation`.

### Requirement: Centralized routing

The mobile application SHALL use a centralized router under `lib/app/app_router.dart`.

#### Scenario: User is unauthenticated

- **GIVEN** no valid session is restored
- **WHEN** the app starts
- **THEN** the router SHALL show `/login`.

#### Scenario: User is authenticated

- **GIVEN** a valid session exists
- **WHEN** the app starts or login succeeds
- **THEN** the router SHALL allow navigation to `/home`.

### Requirement: Riverpod ViewModels

The mobile application SHALL use Riverpod providers/notifiers for ViewModels and dependency wiring.

#### Scenario: Login form submits

- **GIVEN** the login page is visible
- **WHEN** the submit action is triggered
- **THEN** the page SHALL call the Riverpod-backed `AuthViewModel`
- **AND** loading/error state SHALL come from the ViewModel.

### Requirement: Centralized HTTP client

The mobile application SHALL create HTTP access through a centralized API client and shall not instantiate Dio in widgets.

#### Scenario: API call is needed

- **GIVEN** a repository needs backend data
- **WHEN** it performs an HTTP request
- **THEN** it SHALL use the centralized API client or API service
- **AND** UI widgets SHALL not import or instantiate Dio.

### Requirement: Secure token storage abstraction

The mobile application SHALL store tokens through a `TokenStorage` abstraction backed by secure storage.

#### Scenario: Login succeeds

- **GIVEN** backend or auth provider returns tokens/session data
- **WHEN** repository persists the session
- **THEN** it SHALL use `TokenStorage`
- **AND** UI widgets SHALL not use `FlutterSecureStorage` directly.

### Requirement: Environment via dart-define

The mobile application SHALL configure the API base URL with `--dart-define`.

#### Scenario: Android emulator run

- **GIVEN** backend is running on the developer machine
- **WHEN** running the app on Android emulator
- **THEN** the app SHALL support `--dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1`.

### Requirement: Current compatible package versions

Dependencies SHALL be installed using `flutter pub add` so Pub resolves current compatible versions.

#### Scenario: Adding dependencies

- **GIVEN** the Flutter app exists
- **WHEN** adding Riverpod, routing, HTTP, and secure storage dependencies
- **THEN** Claude SHALL use `flutter pub add flutter_riverpod go_router dio flutter_secure_storage`
- **AND** SHALL not paste stale hardcoded versions unless needed for compatibility.

### Requirement: Repository boundary after Nx cleanup

After the old Nx/Ionic mobile cleanup, Claude SHALL remain within mobile paths and SHALL not scan server, infra, edge, or web folders.

#### Scenario: Mobile architecture implementation starts

- **GIVEN** old mobile cleanup is complete
- **WHEN** Claude implements Flutter architecture
- **THEN** Claude SHALL work only in `tchalanet-mobile/**`, `VERSIONS.md`, and this OpenSpec folder
- **AND** SHALL not scan `tchalanet-server/**`, `tchalanet-infra/**`, `tchalanet-edge-service/**`, or `apps/tchalanet-web/**`.
