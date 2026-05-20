# Claude implementation prompt

You are implementing OpenSpec change `mobile-flutter-architecture-upgrade` for Tchalanet.

## Read first

Read only:

- `AGENTS.md`
- `VERSIONS.md`
- `openspec/changes/mobile-flutter-architecture-upgrade/**`
- `tchalanet-mobile/**` if it already exists
- root-level files only if needed to validate previous Nx/Ionic cleanup

## Hard boundary

Do not scan, grep, index, refactor, or edit these folders:

- `tchalanet-server/**`
- `tchalanet-infra/**`
- `tchalanet-edge-service/**`
- `apps/tchalanet-web/**`
- unrelated backend, infra, edge, or web libraries

After the old Nx/Ionic mobile cleanup is done, remain inside:

- `tchalanet-mobile/**`
- `VERSIONS.md`
- this OpenSpec folder

Do not spend tokens reading server/infra/edge/web code. The mobile app must be built from mobile contracts and placeholders until backend endpoints are explicitly confirmed by the user.

## Mission

Pass behind the first migration and align the new Flutter mobile app with the official Flutter architecture guidance:

- MVVM baseline.
- View + ViewModel in UI/application layer.
- Repositories and services in data/model layer.
- Feature-first folders.
- Riverpod for ViewModels/state/dependency injection.
- go_router for routing.
- Dio for HTTP.
- flutter_secure_storage behind a TokenStorage abstraction.

## Flutter version rule

Before generating or editing Flutter code, check the local SDK:

```bash
which flutter
flutter --version
flutter doctor -v
```

Use the latest stable Flutter available by upgrading:

```bash
flutter channel stable
flutter upgrade
flutter doctor -v
```

If the local Flutter SDK is too old or broken, instruct the developer to install a clean stable SDK, for example:

```bash
mkdir -p ~/dev-tools
cd ~/dev-tools
git clone https://github.com/flutter/flutter.git -b stable
export PATH="$HOME/dev-tools/flutter/bin:$PATH"
flutter doctor -v
```

Use current dependency versions resolved by Flutter/Pub. Do not hardcode stale dependency versions from old examples.

Install dependencies with:

```bash
cd tchalanet-mobile
flutter pub add flutter_riverpod go_router dio flutter_secure_storage
flutter pub add --dev flutter_lints
```

## Required structure

Create/normalize this structure:

```txt
lib/
├── main.dart
├── app/
│   ├── app.dart
│   ├── app_router.dart
│   └── app_theme.dart
├── core/
│   ├── config/app_config.dart
│   ├── network/api_client.dart
│   ├── network/api_exception.dart
│   ├── network/auth_interceptor.dart
│   ├── storage/token_storage.dart
│   └── storage/secure_token_storage.dart
└── features/auth/
    ├── application/auth_view_model.dart
    ├── data/auth_api.dart
    ├── data/auth_repository_impl.dart
    ├── domain/auth_repository.dart
    ├── domain/auth_session.dart
    ├── domain/login_credentials.dart
    └── presentation/login_page.dart
```

## Rules

- Widgets must not call Dio.
- Widgets must not access secure storage.
- ViewModels call repositories.
- Repository implementations call APIs/services/storage.
- API base URL comes from `--dart-define=API_BASE_URL=...`.
- Use `http://10.0.2.2:8080/api/v1` as Android emulator default only.
- Do not invent backend endpoints. Keep `AuthApi` isolated so the endpoint can be adapted later.
- Login errors must render inside the form/page, not toast-only.
- Update `VERSIONS.md` with actual Flutter/Dart versions after upgrade.

## Validation

Run:

```bash
cd tchalanet-mobile
flutter pub get
dart format lib test
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

Only if root/Nx cleanup was touched, run root validation:

```bash
pnpm nx show projects
pnpm nx affected -t lint,test,build --base=origin/main --head=HEAD
```

## Output expected

Return:

- Files created/changed.
- Flutter/Dart versions used.
- Dependency versions resolved in `pubspec.yaml`.
- Validation commands and results.
- Any missing backend endpoint uncertainty, without modifying backend code.
