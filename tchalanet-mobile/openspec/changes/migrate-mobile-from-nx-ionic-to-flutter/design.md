# Design: standalone Flutter mobile app

## Target repository layout

```text
tchalanet/
├── apps/
│   └── tchalanet-web/              # Angular/Nx web remains here
├── tchalanet-server/               # Spring Boot, out of scope
├── tchalanet-edge-service/         # Fastify/TypeScript, out of scope
├── tchalanet-infra/                # Docker/infra, out of scope
├── tchalanet-mobile/               # New Flutter app, outside Nx
├── nx.json
├── package.json
└── VERSIONS.md
```

## Repository boundary rule

This change is intentionally narrow.

Claude may inspect the old Nx mobile app and root workspace files during cleanup. After that cleanup, Claude must not scan backend, infra, edge, or web code. All implementation work must remain inside `tchalanet-mobile/**`, plus `VERSIONS.md`.

This protects tokens and prevents accidental cross-project changes.

## Flutter stack

Recommended MVP packages:

- `flutter_riverpod` for state management.
- `go_router` for routing and auth redirects.
- `dio` for HTTP.
- `flutter_secure_storage` for token persistence.
- `flutter_lints` for linting.

Optional later:

- `freezed`
- `json_serializable`
- `build_runner`

Do not add optional code-generation packages unless the first implementation really needs them.

## Flutter app structure

```text
tchalanet-mobile/lib/
├── main.dart
├── app/
│   ├── app.dart
│   ├── app_router.dart
│   └── app_theme.dart
├── core/
│   ├── config/
│   │   └── app_config.dart
│   ├── network/
│   │   ├── api_client.dart
│   │   ├── api_exception.dart
│   │   └── auth_interceptor.dart
│   └── storage/
│       ├── token_storage.dart
│       └── secure_token_storage.dart
└── features/
    └── auth/
        ├── data/
        │   ├── auth_api.dart
        │   └── auth_repository_impl.dart
        ├── domain/
        │   ├── auth_repository.dart
        │   ├── auth_session.dart
        │   └── login_credentials.dart
        ├── application/
        │   └── auth_controller.dart
        └── presentation/
            └── login_page.dart
```

## Rules by layer

### presentation

- Contains Flutter widgets only.
- Does not create `Dio` directly.
- Does not read/write secure storage directly.
- Uses Riverpod providers/controllers.
- Shows validation and loading/error states.

### application

- Coordinates UI state and use cases.
- Owns `AuthController` or equivalent state notifier.
- Converts repository errors into UI-friendly state.

### domain

- Contains small contracts and domain records/classes.
- No Flutter widgets.
- No HTTP client dependency.

### data

- Contains API calls and repository implementations.
- Depends on `core/network` and `core/storage`.
- Maps HTTP/API errors into `ApiException` or domain-friendly failures.

### core

- Shared config, HTTP, storage, and low-level utilities.
- No feature-specific UI.

## Authentication design

Define a stable repository boundary:

```dart
abstract interface class AuthRepository {
  Future<AuthSession> login(LoginCredentials credentials);
  Future<AuthSession?> restoreSession();
  Future<void> logout();
}
```

The first implementation may call a placeholder/auth endpoint only if it exists in the backend. If the backend login flow is Keycloak/OIDC, keep `AuthApi` isolated so it can be replaced later without changing widgets.

Claude must not invent backend endpoint behavior. If the backend auth endpoint is unknown, implement the boundary and make the exact endpoint configurable/TODO clearly.

## Auth state

Use either a sealed state or `AsyncValue<AuthSession?>`.

Example sealed state:

```dart
sealed class AuthState {}

final class AuthUnknown extends AuthState {}

final class AuthUnauthenticated extends AuthState {}

final class AuthAuthenticated extends AuthState {
  AuthAuthenticated(this.session);
  final AuthSession session;
}
```

## Routing

Use `MaterialApp.router` and `go_router`.

Minimum routes:

- `/login` public route.
- `/home` protected placeholder route.

Redirect rules:

- unauthenticated user trying to access protected route -> `/login`
- authenticated user on `/login` -> `/home`

## HTTP and configuration

Use a single Dio instance configured centrally.

Base URL must come from Dart define:

```dart
const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api/v1',
);
```

Android emulator uses `10.0.2.2` to reach the host machine.

Do not hardcode production URLs in widgets.

## Token storage

Use an abstraction:

```dart
abstract interface class TokenStorage {
  Future<String?> readAccessToken();
  Future<void> writeAccessToken(String token);
  Future<void> clear();
}
```

Default implementation can use `flutter_secure_storage`.

## Login MVP UX

The login page must include:

- App title: `Tchalanet`.
- Username field:
  - label: `Nom d’utilisateur`
  - required validation
  - text input action: next
- Password field:
  - label: `Mot de passe`
  - required validation
  - obscured input
  - submit on keyboard done
- Submit button:
  - disabled when form invalid or loading
  - shows loading state while login is running
- Error display:
  - visible inside the page, not toast-only
- Accessibility:
  - labels on fields
  - reasonable touch targets

## Android-first test strategy

Minimum validation commands:

```bash
cd tchalanet-mobile
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

## Version documentation

`VERSIONS.md` must be updated so future agents know:

- Flutter is the active mobile stack.
- Dart version is recorded from local `flutter --version` output.
- Android build is generated/managed by Flutter.
- The previous Ionic/Capacitor Nx mobile app was removed by this change.
