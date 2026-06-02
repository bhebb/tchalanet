# Skill — Mobile/POS Testing for Tchalanet Flutter

Use this skill only for `tchalanet-mobile` tasks.

## Principle

Mobile tests protect POS reliability: identity, terminal binding, operational context, ticket sale, print/receipt, QR verification, and offline/sync readiness.

## Unit tests

Good targets:

- Riverpod providers/state notifiers;
- DTO parsing;
- Dio API client wrappers with fake clients;
- local validators;
- money/date formatting;
- terminal binding state machine;
- receipt view models;
- permission/capability UI decisions.

## Widget tests

Good targets:

- login screen;
- terminal binding/challenge screen;
- operational context banner;
- POS dashboard;
- sell form;
- ticket preview/receipt screen;
- error/offline/sync banners;
- payout confirmation screen.

## Integration tests

Use for:

- secure storage abstraction;
- key generation/storage abstraction with fake crypto;
- signed challenge flow against fake backend;
- API interceptor attaching auth/device/operational headers;
- local persistence/sync queue when implemented.

## E2E candidates

Start small:

- cashier login;
- bind terminal;
- select/reuse outlet + terminal + open session;
- sell ticket;
- show printable/QR receipt;
- verify public code;
- payout happy path;
- permission removed -> sell blocked.

## Rules

- Backend API e2e may cover most POS business flows first.
- Flutter e2e should cover device-specific risks and top happy paths.
- Do not rely on real printers in normal CI; use print adapter fakes or golden receipt payloads.
- Never store private keys in test logs.
- Use deterministic fake clocks and seeded fixtures.

## Do NOT test

- Real printers in CI (use fake adapters or golden receipt payloads).
- Private keys or sensitive crypto material in logs (redact or use fake keys).
- Backend API responses duplicated in Flutter unit tests (test contract consumption, not backend logic).
- Flutter framework widgets unless you've customized their behavior.
- Device-specific behavior that's already covered by backend API e2e tests.
