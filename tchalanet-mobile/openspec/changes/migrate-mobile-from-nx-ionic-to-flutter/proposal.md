# Change: migrate-mobile-from-nx-ionic-to-flutter

## Why

The mobile app must move away from the current Nx/Ionic/Capacitor setup and become a dedicated Flutter application.

The current project source of truth still references an Ionic/Capacitor mobile app under the Nx workspace. This change removes that legacy mobile app from Nx, creates a standalone Flutter app at the repository root, and establishes a clean MVP mobile foundation focused first on Android and login.

The goal is to avoid mixing Angular/Nx mobile concerns with the new Flutter app, reduce workspace complexity, and give the mobile team or agent a small, isolated scope.

## What changes

- Remove the old Nx/Ionic/Capacitor mobile app and its Nx project registration.
- Remove stale Ionic/Capacitor dependencies, scripts, and CI references when they are no longer used.
- Create a new standalone Flutter app under `tchalanet-mobile/` at repository root.
- Configure a minimal Flutter architecture for Android-first development.
- Add routing, theme, auth state, HTTP client, token storage abstraction, and environment configuration.
- Implement the first mobile page: login with username and password.
- Add local Android test/run documentation.
- Update `VERSIONS.md` so Flutter becomes the active mobile stack and Ionic/Capacitor is no longer documented as active.

## Non-goals

- Do not implement the vendor sell process yet.
- Do not implement dashboards yet.
- Do not implement offline sync yet.
- Do not implement Bluetooth printing yet.
- Do not implement push notifications yet.
- Do not refactor backend authentication.
- Do not change web Angular behavior.
- Do not move Flutter inside Nx.
- Do not scan or refactor server, infra, edge, or web code beyond the minimal root files required for removing the old Nx mobile registration.

## Scope

### In scope

- Old Nx mobile app folder, if present:
  - `apps/tchalanet-mobile/**`
  - `apps/tchalanet-mobile-e2e/**`
  - any similarly named Ionic/Capacitor mobile project under `apps/`
- Root workspace references:
  - `nx.json`
  - `workspace.json` or `angular.json`, if present
  - `project.json` files for old mobile projects, if present
  - `package.json`
  - lockfile, if dependency cleanup changes it
  - `.github/workflows/**`, only if a workflow explicitly targets the removed mobile app
  - `tsconfig.base.json`, only if it contains old mobile-specific aliases
- New Flutter app:
  - `tchalanet-mobile/**`
- Documentation/versioning:
  - `VERSIONS.md`
  - optional root README note for mobile location

### Out of scope

- `tchalanet-server/**`
- `tchalanet-infra/**`
- `tchalanet-edge-service/**`
- `apps/tchalanet-web/**`, except when removing a direct stale reference to the deleted mobile app from shared root config
- Backend endpoint implementation
- Public web page model changes
- Production deployment

## Claude / agent hard boundary

After the old Nx/Ionic mobile app has been removed and root Nx references are cleaned, Claude must stop scanning the wider repository and work only in:

- `tchalanet-mobile/**`
- `VERSIONS.md`
- this OpenSpec change folder

Claude must not scan, grep, read, or modify:

- `tchalanet-server/**`
- `tchalanet-infra/**`
- `tchalanet-edge-service/**`
- `apps/tchalanet-web/**`
- backend docs/specs unrelated to this mobile migration

Allowed exception: during the initial cleanup only, Claude may inspect root-level workspace files and old mobile project files to remove stale Nx/Ionic references. Once the old mobile project is gone and `nx show projects` no longer lists it, Claude must stay inside `tchalanet-mobile/**`.

## Risks

- Accidentally deleting shared web Angular dependencies still used by `apps/tchalanet-web`.
- Accidentally scanning the backend/infra and wasting tokens.
- Inventing a backend login endpoint that does not exist.
- Leaving stale Nx project references that break `pnpm nx show projects`.
- Forgetting to update `VERSIONS.md`, causing future agents to follow the old Ionic/Capacitor stack.

## Acceptance criteria

- The old Nx/Ionic/Capacitor mobile project is removed from the Nx workspace.
- `pnpm nx show projects` does not show the old mobile project.
- Root scripts and CI no longer target the deleted mobile app.
- Ionic/Capacitor dependencies are removed only if they are not used anywhere else.
- A standalone Flutter app exists at `tchalanet-mobile/`.
- `cd tchalanet-mobile && flutter pub get` succeeds.
- `cd tchalanet-mobile && flutter analyze` succeeds.
- `cd tchalanet-mobile && flutter test` succeeds.
- The Flutter app can run on an Android emulator with `flutter run`.
- The first screen is a login page with username and password.
- HTTP/auth/storage/routing are centralized and not embedded directly in widgets.
- `VERSIONS.md` documents Flutter as the active mobile stack.
