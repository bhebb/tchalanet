# Tasks

## 0. Preconditions

- [ ] Confirm the legacy Nx/Ionic mobile removal change has been applied or is in progress.
- [ ] Read `AGENTS.md`, `VERSIONS.md`, and this OpenSpec change folder.
- [ ] Do not scan backend, infra, edge, or web folders.

## 1. Check and upgrade local Flutter

- [ ] Run `which flutter`.
- [ ] Run `flutter --version`.
- [ ] Run `flutter doctor -v`.
- [ ] Switch to stable channel: `flutter channel stable`.
- [ ] Upgrade: `flutter upgrade`.
- [ ] Run `flutter doctor -v` again.
- [ ] If the SDK is too old/broken, install a clean stable SDK in `~/dev-tools/flutter`.
- [ ] Ensure PATH points to the selected SDK.
- [ ] Accept Android licenses if needed: `flutter doctor --android-licenses`.

## 2. Update version documentation

- [ ] Update `VERSIONS.md` with actual Flutter version.
- [ ] Update `VERSIONS.md` with actual Dart version bundled with Flutter.
- [ ] Mention Android-first target.
- [ ] Mention that old Ionic/Capacitor mobile was removed or is legacy.

Suggested section:

```md
## Mobile (Flutter)

- Flutter: <actual output from flutter --version>
- Dart: <actual output from flutter --version>
- Target platform: Android first
- Project path: `tchalanet-mobile/`
- Package manager/build: Flutter CLI + generated Gradle wrapper
- Previous Ionic/Capacitor mobile app: removed from Nx workspace by OpenSpec change `<change-name>`
```

## 3. Create or normalize Flutter app

- [ ] Ensure app root is `tchalanet-mobile/` at repository root.
- [ ] If not created yet, run:

```bash
flutter create tchalanet-mobile --platforms=android --org com.tchalanet
```

- [ ] Confirm Android application id is appropriate, preferably `com.tchalanet.mobile`.
- [ ] Do not place Flutter app under `apps/`.
- [ ] Do not add Flutter to Nx.

## 4. Install latest compatible dependencies

Inside `tchalanet-mobile/`:

```bash
flutter pub add flutter_riverpod go_router dio flutter_secure_storage
flutter pub add --dev flutter_lints
```

- [ ] Let Flutter/Pub resolve latest compatible versions.
- [ ] Do not copy stale dependency versions from old examples.
- [ ] Run `flutter pub outdated` and review, but do not blindly force incompatible prereleases.
- [ ] Run `flutter pub get`.

## 5. Apply official MVVM structure

- [ ] Create `lib/app/app.dart`.
- [ ] Create `lib/app/app_router.dart`.
- [ ] Create `lib/app/app_theme.dart`.
- [ ] Create `lib/core/config/app_config.dart`.
- [ ] Create `lib/core/network/api_client.dart`.
- [ ] Create `lib/core/network/api_exception.dart`.
- [ ] Create `lib/core/network/auth_interceptor.dart`.
- [ ] Create `lib/core/storage/token_storage.dart`.
- [ ] Create `lib/core/storage/secure_token_storage.dart`.
- [ ] Create auth feature folders: `application`, `data`, `domain`, `presentation`.

## 6. Implement auth MVP architecture

- [ ] Create `LoginCredentials` domain model.
- [ ] Create `AuthSession` domain model.
- [ ] Create `AuthRepository` domain contract.
- [ ] Create `AuthApi` data service.
- [ ] Create `AuthRepositoryImpl` data implementation.
- [ ] Create `AuthViewModel` Riverpod notifier/controller.
- [ ] Create `LoginPage` view.
- [ ] Add `/login` and `/home` routes.
- [ ] Add route guard/redirect based on auth state.
- [ ] Ensure LoginPage does not call Dio/storage directly.

## 7. Add tests and validation

- [ ] Add widget test for login page rendering.
- [ ] Add unit test for auth view model validation/failure where practical.
- [ ] Run:

```bash
dart format lib test
flutter analyze
flutter test
```

- [ ] Run on Android emulator:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

## 8. Documentation

- [ ] Add or update `tchalanet-mobile/README.md`.
- [ ] Document Android Studio/emulator setup.
- [ ] Document `10.0.2.2` for emulator backend access.
- [ ] Document physical device LAN IP usage.
- [ ] Document standard commands: pub get, analyze, test, run.

## 9. Repository boundary validation

- [ ] Confirm no code was scanned or edited under `tchalanet-server/**`.
- [ ] Confirm no code was scanned or edited under `tchalanet-infra/**`.
- [ ] Confirm no code was scanned or edited under `tchalanet-edge-service/**`.
- [ ] Confirm no Angular web code was edited.
- [ ] If root cleanup was needed, confirm it was limited to stale mobile references only.
