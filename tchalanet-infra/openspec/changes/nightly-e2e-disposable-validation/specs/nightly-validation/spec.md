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

### Requirement: Web E2E Retains Deterministic REST Contracts

The Full Validation workflow SHALL keep the browser suite's REST responses deterministic.
It MAY reuse the Firebase Auth Emulator of the disposable runtime for real browser sign-in.

#### Scenario: Disposable runtime is deployed before web E2E

- **WHEN** the runtime deployment job completes successfully
- **THEN** it exposes Firebase emulator coordinates as job outputs
- **AND** the web E2E job tunnels that emulator for browser sign-in
- **AND** the web E2E suite keeps its REST stubs enabled
- **AND** the server E2E job remains responsible for assertions against the deployed API and database.

#### Scenario: Runtime deployment is skipped for a web-only run

- **WHEN** no disposable runtime coordinates are available
- **THEN** web E2E falls back to the self-contained Firebase-emulator plus REST-stub harness
- **AND** it does not assume staging or run-specific API URLs.

### Requirement: Validation Report Supports Optional Slack Notifications

The Full Validation workflow SHALL always write its GitHub step summary. Slack notification is optional.

#### Scenario: No Slack webhook is configured

- **WHEN** neither the GitHub webhook secret nor `SLACK_WEBHOOK_OPS_ALERTS` exists in Doppler
- **THEN** the report job completes its summary without emitting a Doppler lookup error
- **AND** it logs that the GitHub-only report fallback was used.

#### Scenario: Slack webhook is configured

- **WHEN** a webhook is available from GitHub secrets or Doppler
- **THEN** the report job posts the compact validation summary to that webhook
- **AND** a notification delivery failure remains non-blocking for the report job.
