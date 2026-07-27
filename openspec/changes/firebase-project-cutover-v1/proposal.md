# Change: firebase-project-cutover-v1

## Why

Firebase was migrated from `tchalanet-39115` to `tchalanet`, but runtime
defaults, Doppler configurations, the mobile distribution fallback, and
operational documents still reference the old project. A local IDE run or a
manually triggered mobile build can therefore use stale identity credentials.

## What

- Make `tchalanet` the explicit Firebase project for the server runtime.
- Make `local-ide` use real Firebase credentials while preserving the Docker
  `dev` Firebase Auth Emulator for deterministic E2E tests.
- Align staging and local-IDE Doppler values and validate service-account
  credentials against the configured project.
- Update the mobile distribution fallback and operational references to the
  new Firebase project.

## Impact

The change affects local authentication setup, staging deployment inputs, and
the manual Android staging distribution workflow. It does not change the
PostgreSQL database topology or business authorization rules.

## Non-goals

- Do not migrate existing application users or seller terminals between
  Firebase projects.
- Do not commit Firebase service-account credentials.
- Do not change the Docker/E2E emulator runtime.
