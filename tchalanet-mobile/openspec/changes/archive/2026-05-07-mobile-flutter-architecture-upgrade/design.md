# Design: Flutter mobile architecture and version policy

## Architecture baseline

Use Flutter official architecture guidance as the baseline:

- UI layer: Views + ViewModels.
- Data/model layer: Repositories + Services.
- Views render UI and forward user actions.
- ViewModels own screen state, loading state, validation state, and commands.
- Repositories are the source of truth for app data.
- Services interact with external APIs, secure storage, platform plugins, or local persistence.

For Tchalanet, implement this as:

- View = Flutter page/widget in `presentation/`.
- ViewModel = Riverpod notifier/controller in `application/`.
- Domain = models and repository contracts in `domain/`.
- Data = API clients and repository implementations in `data/`.
- Core = shared config, network, storage, routing, theme, errors.

## Recommended stack

Use latest compatible versions resolved by `flutter pub add`:

- Flutter stable channel.
- Dart version bundled with Flutter stable.
- `flutter_riverpod` for state management and dependency injection.
- `go_router` for routing and route guards.
- `dio` for HTTP client.
- `flutter_secure_storage` behind a `TokenStorage` abstraction.
- `flutter_lints` for baseline linting.
- Optional after MVP: `freezed`, `json_serializable`, `build_runner`.

Do not pin versions manually unless there is a real compatibility problem. Prefer:

```bash
flutter pub add flutter_riverpod go_router dio flutter_secure_storage
flutter pub add --dev flutter_lints
```

## Local Flutter version policy

Before coding:

```bash
flutter --version
flutter channel stable
flutter upgrade
flutter doctor -v
```

If the local Flutter SDK is too old or broken, install a clean stable SDK instead of fighting the old installation:

```bash
mkdir -p ~/dev-tools
cd ~/dev-tools
git clone https://github.com/flutter/flutter.git -b stable
```

Then ensure the shell points to the new SDK:

```bash
export PATH="$HOME/dev-tools/flutter/bin:$PATH"
```

Persist it in `~/.zshrc` or the shell profile used by the developer.

After upgrade/install, record actual versions in `VERSIONS.md`:

```bash
flutter --version
flutter doctor -v
```

## Application structure

```txt
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
│   ├── result/
│   │   └── app_result.dart
│   └── storage/
│       ├── token_storage.dart
│       └── secure_token_storage.dart
└── features/
    └── auth/
        ├── application/
        │   └── auth_view_model.dart
        ├── data/
        │   ├── auth_api.dart
        │   └── auth_repository_impl.dart
        ├── domain/
        │   ├── auth_repository.dart
        │   ├── auth_session.dart
        │   └── login_credentials.dart
        └── presentation/
            └── login_page.dart
```

Future features follow the same pattern:

```txt
features/
├── auth/
├── home/
├── dashboard/
├── sell/
├── ticket_verify/
├── profile/
└── settings/
```

## Dependency rules

- Widgets must not call Dio directly.
- Widgets must not access secure storage directly.
- Widgets must not know token implementation details.
- ViewModels may call repositories.
- Repositories may call APIs/services/storage.
- API clients return DTOs or typed response models.
- Repository implementations map DTOs into domain models.
- Router reads auth state through a central provider.

## Auth MVP

Start with login only:

- Route `/login`: public default route for unauthenticated users.
- Route `/home`: protected placeholder route after successful login.
- `AuthViewModel` owns username, password, loading, error, and session state.
- `AuthRepository` exposes:

```dart
abstract interface class AuthRepository {
  Future<AuthSession> login(LoginCredentials credentials);
  Future<AuthSession?> restoreSession();
  Future<void> logout();
}
```

Do not invent backend endpoints. If the backend currently uses Keycloak/OIDC directly, keep `AuthApi` isolated so the implementation can switch from a placeholder endpoint to OIDC without touching the UI.

## Environment configuration

Use `--dart-define`:

```dart
class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api/v1',
  );
}
```

For Android emulator to reach backend localhost, use:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

For a physical device, use the LAN IP of the dev machine, not `localhost`.

## Error handling

Introduce a shared API exception:

```dart
class ApiException implements Exception {
  const ApiException({
    required this.code,
    required this.message,
    this.statusCode,
  });

  final String code;
  final String message;
  final int? statusCode;
}
```

The UI should render errors in-page for forms. Toast-only errors are not enough for login.

## Validation commands

Run these before marking the change done:

```bash
cd tchalanet-mobile
flutter pub get
dart format lib test
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

At repo root, only if cleanup touched Nx/root files:

```bash
pnpm nx show projects
pnpm nx affected -t lint,test,build --base=origin/main --head=HEAD
```
