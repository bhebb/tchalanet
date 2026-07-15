# nightly-e2e-disposable-validation

## Why

Nightly E2E validation was tied to whichever staging runtime happened to be available, and the
recent switch to real Firebase made failures harder to diagnose: a down server, stale runtime,
missing Firebase secrets, or application regression all failed in the same place.

## What

- Make Full Validation able to build immutable API and edge-service images for the current SHA.
- Ensure staging core infra exists before server E2E.
- Deploy runtime services in a validation mode that can use Firebase Auth Emulator.
- Run the Firebase-emulator `full_flow` by default for scheduled server E2E.
- Keep Locust as an explicit optional smoke, separate from functional E2E.

## Impact

- Scheduled validation no longer depends on real Firebase user secrets.
- The main functional proof becomes provision → configure → sell against an isolated identity
  emulator.
- Staging real-Firebase smoke remains available through manual workflow inputs.
