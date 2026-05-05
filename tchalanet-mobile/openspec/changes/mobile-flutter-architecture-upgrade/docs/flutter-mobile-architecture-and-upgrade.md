# Flutter mobile architecture and upgrade guide

## Goal

Start Tchalanet mobile with a clean, recent Flutter stable SDK and an architecture aligned with Flutter official guidance.

## Architecture decision

Use MVVM:

- View: Flutter widgets/pages.
- ViewModel: Riverpod notifier/controller that owns UI state and actions.
- Repository: source of truth for feature data.
- Service/API: external API, secure storage, platform plugin, or local source.

## Recommended commands

```bash
which flutter
flutter --version
flutter channel stable
flutter upgrade
flutter doctor -v
```

If the SDK is broken:

```bash
mkdir -p ~/dev-tools
cd ~/dev-tools
git clone https://github.com/flutter/flutter.git -b stable
export PATH="$HOME/dev-tools/flutter/bin:$PATH"
flutter doctor -v
```

## Create project

```bash
flutter create tchalanet-mobile --platforms=android --org com.tchalanet
```

## Add dependencies

```bash
cd tchalanet-mobile
flutter pub add flutter_riverpod go_router dio flutter_secure_storage
flutter pub add --dev flutter_lints
```

## Run locally

Android emulator:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

Physical Android device:

```bash
flutter run --dart-define=API_BASE_URL=http://<DEV_MACHINE_LAN_IP>:8080/api/v1
```

## Validate

```bash
dart format lib test
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

## Boundary reminder

This change is mobile-only. Do not scan server, infra, edge, or web code after the legacy Nx/Ionic cleanup.
