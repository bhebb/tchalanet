# Tasks: migrate mobile from Nx/Ionic to Flutter

## 0. Read required context

- [ ] Read root `AGENTS.md`.
- [ ] Read `VERSIONS.md`.
- [ ] Read this OpenSpec change folder.
- [ ] Do not scan `tchalanet-server/**`, `tchalanet-infra/**`, `tchalanet-edge-service/**`, or `apps/tchalanet-web/**` for this change.

## 1. Inventory old mobile references only

Allowed commands:

```bash
pnpm nx show projects
rg "ionic|capacitor|tchalanet-mobile|mobile-e2e" nx.json package.json .github apps libs tsconfig.base.json workspace.json angular.json 2>/dev/null || true
```

Rules:

- [ ] Inspect only root workspace config and old mobile app folders.
- [ ] Do not grep the full repository.
- [ ] Do not inspect server/infra/edge/web implementation files.

## 2. Remove old Nx/Ionic mobile app

- [ ] Identify the old mobile project name from `pnpm nx show projects`.
- [ ] Delete old mobile app folder under `apps/`, if present.
- [ ] Delete old mobile e2e folder under `apps/`, if present.
- [ ] Remove project registration from Nx/workspace files, if needed.
- [ ] Remove old mobile-specific path aliases from `tsconfig.base.json`, if present.
- [ ] Remove scripts that target the deleted mobile app.
- [ ] Remove CI workflow jobs that target the deleted mobile app.
- [ ] Remove Ionic/Capacitor dependencies only if no remaining project uses them.
- [ ] Run package manager install if package dependencies changed.

Validation:

```bash
pnpm nx show projects
pnpm nx affected -t lint,test,build --base=origin/main --head=HEAD
```

Important:

- If `pnpm nx affected` is too expensive locally, run at least `pnpm nx show projects` and document the skipped validation.
- Do not scan backend/infra/edge/web after this point.

## 3. Hard stop after Nx cleanup

After the old mobile project no longer appears in Nx:

- [ ] Stop scanning the root repository.
- [ ] Continue only in `tchalanet-mobile/**`, `VERSIONS.md`, and this OpenSpec change folder.
- [ ] Do not read server/infra/edge/web code.
- [ ] Do not search the whole repository to discover auth behavior.

## 4. Create standalone Flutter app

From repository root:

```bash
flutter create tchalanet-mobile --platforms=android --org com.tchalanet
```

Then:

- [ ] Set Android application id/package to `com.tchalanet.mobile` if Flutter did not generate the expected id.
- [ ] Keep the Flutter app outside Nx.
- [ ] Do not add Nx Flutter plugins.

## 5. Add Flutter dependencies

Inside `tchalanet-mobile/`:

```bash
flutter pub add flutter_riverpod go_router dio flutter_secure_storage
flutter pub add --dev flutter_lints
```

Optional only if needed:

```bash
flutter pub add freezed_annotation json_annotation
flutter pub add --dev build_runner freezed json_serializable
```

- [ ] Avoid code generation in MVP unless it clearly reduces complexity.

## 6. Create mobile architecture skeleton

Create:

```text
lib/app/app.dart
lib/app/app_router.dart
lib/app/app_theme.dart
lib/core/config/app_config.dart
lib/core/network/api_client.dart
lib/core/network/api_exception.dart
lib/core/network/auth_interceptor.dart
lib/core/storage/token_storage.dart
lib/core/storage/secure_token_storage.dart
lib/features/auth/data/auth_api.dart
lib/features/auth/data/auth_repository_impl.dart
lib/features/auth/domain/auth_repository.dart
lib/features/auth/domain/auth_session.dart
lib/features/auth/domain/login_credentials.dart
lib/features/auth/application/auth_controller.dart
lib/features/auth/presentation/login_page.dart
```

- [ ] Keep widgets free from direct Dio usage.
- [ ] Keep widgets free from direct secure storage usage.
- [ ] Keep config centralized.

## 7. Implement app bootstrap

- [ ] `main.dart` uses `ProviderScope`.
- [ ] `App` uses `MaterialApp.router`.
- [ ] Theme is centralized in `app_theme.dart`.
- [ ] Router is centralized in `app_router.dart`.

## 8. Implement environment config

- [ ] Add `AppConfig` reading `API_BASE_URL` from `--dart-define`.
- [ ] Default local Android emulator URL: `http://10.0.2.2:8080/api/v1`.
- [ ] Do not hardcode production URL in widgets.

## 9. Implement HTTP client

- [ ] Create a single Dio provider/factory.
- [ ] Configure base URL, connect timeout, receive timeout.
- [ ] Add auth interceptor that reads token from `TokenStorage`.
- [ ] Convert Dio errors to `ApiException`.
- [ ] Do not expose Dio exceptions directly to presentation widgets.

## 10. Implement auth boundary

- [ ] Define `AuthRepository` interface.
- [ ] Define `LoginCredentials`.
- [ ] Define `AuthSession`.
- [ ] Implement `AuthRepositoryImpl` using `AuthApi` and `TokenStorage`.
- [ ] Keep backend endpoint details isolated in `AuthApi`.
- [ ] Do not invent backend endpoint behavior. If uncertain, leave a precise TODO at the `AuthApi` boundary only.

## 11. Implement auth state and redirects

- [ ] Add `AuthController` with restore/login/logout.
- [ ] Add loading and error state for login.
- [ ] Add router redirect based on auth state.
- [ ] Minimum routes:
  - `/login`
  - `/home`

## 12. Implement login page MVP

- [ ] Username field labelled `Nom d’utilisateur`.
- [ ] Password field labelled `Mot de passe`.
- [ ] Required validation for both fields.
- [ ] Submit disabled while invalid or loading.
- [ ] Loading state visible on submit.
- [ ] Error message visible inside page.
- [ ] Submit works with keyboard done action.
- [ ] Successful login navigates to `/home`.

## 13. Add minimal tests

- [ ] Widget test for login page rendering.
- [ ] Unit/controller test for failed login state, if simple enough for MVP.
- [ ] Keep tests local to `tchalanet-mobile/test/**`.

Validation:

```bash
cd tchalanet-mobile
flutter pub get
flutter analyze
flutter test
```

## 14. Add local Android test documentation

- [ ] Add `tchalanet-mobile/README.md` or `docs/flutter-mobile-local-test.md`.
- [ ] Explain Android Studio install/plugin/emulator setup.
- [ ] Explain `10.0.2.2` for Android emulator.
- [ ] Explain physical device LAN IP usage.
- [ ] Include standard Flutter commands.

## 15. Update VERSIONS.md

- [ ] Replace active mobile stack reference from Ionic/Capacitor to Flutter.
- [ ] Record Flutter version from `flutter --version`.
- [ ] Record Dart version from `flutter --version`.
- [ ] Mention Flutter app path: `tchalanet-mobile/`.
- [ ] Mention old Nx/Ionic mobile was removed by this change.

Suggested section:

```md
## Mobile (Flutter)

- Flutter: <output of flutter --version>
- Dart: <output of flutter --version>
- App path: `tchalanet-mobile/`
- Android target: generated by Flutter under `tchalanet-mobile/android`
- Build tool: Flutter CLI + Gradle wrapper generated by Flutter
- Previous Ionic/Capacitor mobile app: removed from Nx workspace by OpenSpec change `migrate-mobile-from-nx-ionic-to-flutter`
```

## 16. Final validation

From repository root:

```bash
pnpm nx show projects
```

From Flutter app:

```bash
cd tchalanet-mobile
flutter pub get
flutter analyze
flutter test
```

Optional Android run:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

## 17. Final report expected from Claude

Claude must report:

- Old mobile projects removed from Nx.
- Files changed in root cleanup.
- Flutter app created path.
- Flutter validation results.
- Any skipped validation and why.
- Any auth endpoint TODO left intentionally.
- Confirmation that server/infra/edge/web were not scanned after Nx cleanup.
