# tchalanet-mobile

Standalone Flutter application for Tchalanet. Android-first (POS terminal + personal Android devices).

This app lives outside the Nx workspace — it is managed entirely by the Flutter CLI and Gradle.

## Prerequisites

- Flutter 3.44.0 via [FVM](https://fvm.app/) (`.fvmrc` is committed)
- Android Studio with the Flutter and Dart plugins installed
- An Android emulator or physical device connected via USB/Wi-Fi

From the repository root:

```bash
cd tchalanet-mobile
fvm use
```

## Standard commands

```bash
# Install dependencies
fvm flutter pub get

# Show dependency drift
fvm flutter pub outdated

# Analyse the code
fvm flutter analyze

# Run unit and widget tests
fvm flutter test

# Run on a connected Android emulator against the local Traefik/backend facade
adb reverse tcp:8443 tcp:8443
fvm flutter run \
  --dart-define=API_BASE_URL=https://api.localtest.me:8443/api/v1
```

## Local backend URL

The default mobile API URL is:

```
https://api.localtest.me:8443/api/v1
```

For Android emulator access, expose the local HTTPS facade to the device:

```bash
adb reverse tcp:8443 tcp:8443
```

For macOS desktop/Chrome local runs, use:

```bash
fvm flutter run \
  --dart-define=API_BASE_URL=https://api.localtest.me/api/v1
```

For a physical Android device on the same Wi-Fi network, pass your host LAN URL
or the environment-specific API URL:

```bash
fvm flutter run \
  --dart-define=API_BASE_URL=https://<host-or-env>/api/v1
```

## Android Studio setup

1. Install [Android Studio](https://developer.android.com/studio).
2. Open Android Studio → **Plugins** → install **Flutter** (installs Dart automatically).
3. Open **Device Manager** → create an AVD (Android Virtual Device):
   - Recommended: Pixel 6, API 34 (Android 14).
4. Start the emulator.
5. Run `fvm flutter devices` to confirm it is detected.
6. Run `fvm flutter run` from the `tchalanet-mobile/` directory.

## Runtime defines

| Variable | Default | Description |
| --- | --- | --- |
| `API_BASE_URL` | `https://api.localtest.me:8443/api/v1` | Base URL for the Tchalanet backend API |
| `TERMINAL_EMAIL_DOMAIN` | `terminal.tchalanet.local` | Domain used to derive Firebase terminal emails |
| `POS_DEVICE_BINDING` | `e2e-cred-dev` | Dev device-binding credential sent as `X-Device-Binding` |
| `POS_DEVICE` | `false` | Enables POS terminal layout hints when set to `true` |

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
