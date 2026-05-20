# Flutter mobile local Android test guide

## Prerequisites

Install:

- Flutter SDK stable channel.
- Android Studio.
- Android SDK and Android Emulator through Android Studio.
- Flutter and Dart plugins in Android Studio, or VS Code Flutter/Dart extensions.

Check setup:

```bash
flutter --version
flutter doctor -v
```

Accept Android licenses if needed:

```bash
flutter doctor --android-licenses
```

## Create or open an emulator

In Android Studio:

1. Open Device Manager.
2. Create a virtual Android device.
3. Prefer a recent Pixel emulator image.
4. Start the emulator.

Confirm Flutter sees it:

```bash
flutter devices
```

## Run the app locally

From repository root:

```bash
cd tchalanet-mobile
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

`10.0.2.2` is the Android emulator alias for the host machine. Use it when the backend runs on your laptop at `localhost:8080`.

## Run on physical Android device

A physical device cannot use `10.0.2.2` for your laptop.

Use your laptop LAN IP instead:

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.50:8080/api/v1
```

Replace `192.168.1.50` with the real local IP of the machine running the backend.

## Standard validation

Run before committing:

```bash
cd tchalanet-mobile
flutter pub get
flutter analyze
flutter test
dart format lib test
```

## Useful commands

```bash
flutter devices
flutter run -d <device_id>
flutter clean && flutter pub get
flutter build apk --debug
flutter build apk --release
```

## Troubleshooting

### Flutter cannot find Android SDK

Run:

```bash
flutter doctor -v
```

Then verify Android Studio SDK path and install missing SDK components.

### Android licenses not accepted

Run:

```bash
flutter doctor --android-licenses
```

### App cannot reach local backend from emulator

Use:

```bash
--dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

Do not use `localhost` from inside the Android emulator.

### App cannot reach backend from physical device

Use the host machine LAN IP and ensure phone and laptop are on the same network.

### Clear generated Android build state

```bash
flutter clean
flutter pub get
```
