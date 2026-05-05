# Claude implementation prompt

You are implementing OpenSpec change:

`openspec/changes/migrate-mobile-from-nx-ionic-to-flutter`

- Don't create new branch - the folder is isolated -
- use this folder /Users/bhebb/Documents/projets/tchalanet/openspec/changes/migrate-mobile-from-nx-ionic-to-flutter
-

## Mandatory context to read first

Read only:

1. `AGENTS.md`
2. `VERSIONS.md`
3. `openspec/AGENTS.md`, if present
4. `openspec/changes/migrate-mobile-from-nx-ionic-to-flutter/proposal.md`
5. `openspec/changes/migrate-mobile-from-nx-ionic-to-flutter/design.md`
6. `openspec/changes/migrate-mobile-from-nx-ionic-to-flutter/tasks.md`
7. `openspec/changes/migrate-mobile-from-nx-ionic-to-flutter/specs/mobile-app/spec.md`

Do not scan the whole repository.

## Mission

Migrate Tchalanet mobile from old Nx/Ionic/Capacitor to a standalone Flutter app.

You must:

1. Remove the old mobile project from Nx.
2. Remove stale Ionic/Capacitor mobile references safely.
3. Create `tchalanet-mobile/` as a standalone Flutter app outside Nx.
4. Implement the login MVP structure.
5. Add Android local test docs.
6. Update `VERSIONS.md`.

## Critical repository boundary

During initial cleanup, you may inspect only:

- root workspace config files such as `nx.json`, `package.json`, `workspace.json`, `angular.json`, `tsconfig.base.json`
- `.github/workflows/**` only for jobs explicitly targeting the old mobile app
- old mobile project folders under `apps/`

After the old mobile app is removed from Nx and `pnpm nx show projects` no longer lists it, stop scanning the wider repository.

From that point onward, work only in:

- `tchalanet-mobile/**`
- `VERSIONS.md`
- `openspec/changes/migrate-mobile-from-nx-ionic-to-flutter/**`

Do not scan, grep, read, or modify:

- `tchalanet-server/**`
- `tchalanet-infra/**`
- `tchalanet-edge-service/**`
- `apps/tchalanet-web/**`
- backend docs or backend OpenSpecs

Do not run broad commands like:

```bash
rg "auth" .
rg "login" .
find . -type f
```

Do not inspect backend code to discover login endpoint behavior. If the endpoint is unknown, isolate that uncertainty in `AuthApi` with a precise TODO.

## Safe initial commands

```bash
pnpm nx show projects
rg "ionic|capacitor|tchalanet-mobile|mobile-e2e" nx.json package.json .github apps libs tsconfig.base.json workspace.json angular.json 2>/dev/null || true
```

Do not use unrestricted full-repo grep.

## Flutter creation

From repository root:

```bash
flutter create tchalanet-mobile --platforms=android --org com.tchalanet
```

The Flutter app must remain outside Nx.

## Flutter dependencies

Inside `tchalanet-mobile/`:

```bash
flutter pub add flutter_riverpod go_router dio flutter_secure_storage
flutter pub add --dev flutter_lints
```

Do not add optional codegen packages unless necessary.

## Expected Flutter structure

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

## Implementation rules

- Use `ProviderScope` in `main.dart`.
- Use `MaterialApp.router`.
- Use `go_router` for `/login` and `/home`.
- Use Riverpod for auth state.
- Use Dio only through a centralized client/provider.
- Use a token storage abstraction.
- Default local Android emulator API URL:

```dart
const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api/v1',
);
```

- Do not hardcode production URLs in widgets.
- Do not directly use secure storage in widgets.
- Do not invent backend endpoint behavior.

## Login page MVP

Implement a first screen with:

- title `Tchalanet`
- username field labelled `Nom d’utilisateur`
- password field labelled `Mot de passe`
- required validation for both fields
- loading state during submit
- page-level error area
- disabled submit while invalid/loading
- navigation to `/home` after successful login

## Validation

Root cleanup validation:

```bash
pnpm nx show projects
pnpm nx affected -t lint,test,build --base=origin/main --head=HEAD
```

Flutter validation:

```bash
cd tchalanet-mobile
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

If a command cannot be run, report it clearly with the reason.

## Final response format

Report:

1. Old mobile Nx project removed or not found.
2. Root files changed for cleanup.
3. Flutter app path created.
4. Flutter architecture files created.
5. Validation commands run and results.
6. Any TODO left at `AuthApi` boundary.
7. Confirmation that after Nx cleanup you did not scan `tchalanet-server`, `tchalanet-infra`, `tchalanet-edge-service`, or `apps/tchalanet-web`.
