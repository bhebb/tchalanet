# tchalanet-mobile

Standalone Flutter application for Tchalanet. Android-first (POS terminal + personal Android devices).

This app lives outside the Nx workspace — it is managed entirely by the Flutter CLI and Gradle.

## Prerequisites

- [Flutter 3.29+](https://docs.flutter.dev/get-started/install) (or use [fvm](https://fvm.app/) — run `fvm use` at repo root if a `.fvmrc` is present)
- Android Studio with the Flutter and Dart plugins installed
- An Android emulator or physical device connected via USB/Wi-Fi

## Standard commands

```bash
# Install dependencies
flutter pub get

# Analyse the code
flutter analyze

# Run unit and widget tests
flutter test

# Run on a connected device or emulator (local backend via Android emulator)
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

## Local backend URL

The Android emulator maps `10.0.2.2` to the host machine's `localhost`.
When running against a local backend, use:

```
http://10.0.2.2:8080/api/v1
```

If you are running on a physical device connected to the same Wi-Fi network,
replace `10.0.2.2` with your host machine's LAN IP address (e.g. `192.168.1.42`):

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.42:8080/api/v1
```

## Android Studio setup

1. Install [Android Studio](https://developer.android.com/studio).
2. Open Android Studio → **Plugins** → install **Flutter** (installs Dart automatically).
3. Open **Device Manager** → create an AVD (Android Virtual Device):
   - Recommended: Pixel 6, API 34 (Android 14).
4. Start the emulator.
5. Run `flutter devices` to confirm it is detected.
6. Run `flutter run` from the `tchalanet-mobile/` directory.

## Environment variable

| Variable       | Default                       | Description                            |
| -------------- | ----------------------------- | -------------------------------------- |
| `API_BASE_URL` | `http://10.0.2.2:8080/api/v1` | Base URL for the Tchalanet backend API |

Set via `--dart-define` at runtime — never hardcoded in source.

## Project structure

```
lib/
├── main.dart                     # App entry point (ProviderScope)
├── app/
│   ├── app.dart                  # Root widget (MaterialApp.router)
│   ├── app_router.dart           # go_router routes and auth redirects
│   └── app_theme.dart            # Material 3 theme
├── core/
│   ├── config/app_config.dart    # --dart-define env vars
│   ├── network/                  # Dio client, auth interceptor, ApiException
│   └── storage/                  # TokenStorage abstraction + SecureTokenStorage
└── features/
    └── auth/
        ├── domain/               # AuthRepository interface, AuthSession, LoginCredentials
        ├── data/                 # AuthApi (HTTP), AuthRepositoryImpl
        ├── application/          # AuthController (Riverpod Notifier), AuthState
        └── presentation/         # LoginPage widget
```
