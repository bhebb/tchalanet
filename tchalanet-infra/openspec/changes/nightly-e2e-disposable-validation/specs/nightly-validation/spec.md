# nightly-validation Delta

## ADDED Requirements

### Requirement: Full Validation Can Prepare a Disposable Runtime

The Full Validation workflow SHALL be able to prepare a staging validation runtime before server E2E tests run.

#### Scenario: Scheduled validation deploys current runtime images

- **WHEN** the scheduled Full Validation workflow runs
- **THEN** it builds immutable API and edge-service images from the workflow SHA
- **AND** it ensures staging core infrastructure is available
- **AND** it deploys API and edge-service from those images before server E2E.

### Requirement: Nightly Server E2E Uses Firebase Emulator By Default

Scheduled server E2E validation SHALL use Firebase Auth Emulator by default instead of real Firebase credentials.

#### Scenario: Scheduled validation runs full flow

- **WHEN** the scheduled Full Validation workflow reaches server E2E
- **THEN** it configures the API runtime with `TCH_IDENTITY_PROVIDER=firebase-emulator`
- **AND** it starts Firebase Auth Emulator alongside the runtime services
- **AND** it runs the `full_flow` pytest marker by default.

### Requirement: Performance Smoke Is Explicit

Load testing SHALL remain an explicit workflow choice and SHALL not run as part of the functional E2E path by default.

#### Scenario: Operator requests perf smoke

- **WHEN** Full Validation is manually dispatched with `run_perf=true`
- **THEN** it runs Locust after successful server E2E
- **AND** it uploads Locust artifacts for inspection.
