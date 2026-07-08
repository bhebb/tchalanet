# Change: spring-integration-business-flows-v1

## Why

Onboarding, setup readiness, sales blocking, Maryaj gratis, and receipt
print/reprint are business-critical runtime flows. Unit tests cover many domain
pieces and Python E2E covers a real deployed stack, but we need a small Spring
integration suite that runs inside the server build and validates transaction,
Postgres, persistence projection, idempotency, and API error contracts together.

## What Changes

- Add a Spring integration-test plan for business runtime flows.
- Keep integration tests separate from `testing/e2e`.
- Use Testcontainers Postgres for DB/RLS/persistence behavior.
- Use local-jwt/test security for most business tests; use Firebase Emulator
  only for identity-provider integration tests.
- Cover the minimum high-risk flows:
  - tenant provisioning/default setup persistence
  - setup readiness after tenant settings/game/draw configuration
  - limit assignment blocks preview/sell with a clear issue and no ticket
  - Maryaj gratis campaign generation produces a promotional ticket line
  - print and reprint preserve promotional lines and copy markers

## Impact

- No production runtime change.
- Future tests live under `tchalanet-app` or the owning module test source,
  with shared Spring/Testcontainers fixtures.
- E2E remains responsible for browser/full-stack smoke; Spring integration
  becomes the fast pre-merge safety net for server business invariants.
