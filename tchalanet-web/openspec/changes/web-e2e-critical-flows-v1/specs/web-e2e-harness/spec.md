# Spec: Web E2E Harness

## ADDED Requirements

### Requirement: Role-scoped authentication fixtures

The harness SHALL expose per-role authentication so a test can run as `public`,
`admin` (TENANT_ADMIN), `cashier`, or `superAdmin` without repeating login steps.

- Default authentication SHALL use programmatic firebase-emulator sign-in and
  persist a `storageState` reused across tests of that role.
- Emulator identities SHALL mirror the Python E2E (`super_admin` / `admin` /
  `cashier`, project `demo-tchalanet-local`).

#### Scenario: Authenticated role starts already signed in

- **WHEN** a test declares a role fixture other than `public`
- **THEN** the browser context loads with that role's persisted session and no
  interactive login is required.

### Requirement: Backend is a consumed fixture, not provisioned by the suite

The web suite SHALL assume a running emulator stack and one seeded/provisioned
tenant. It SHALL NOT provision tenants, users, or catalog itself.

#### Scenario: Suite runs against the shared emulator stack

- **WHEN** the web e2e suite starts
- **THEN** it targets the same emulator + API bring-up used by Python E2E and
  treats existing tenant/user data as given.

### Requirement: Stable selector convention

Critical controls SHALL be targeted by `data-testid`. Tests SHALL NOT couple to
visible text or CSS classes for critical assertions.

#### Scenario: Missing testid is added at source

- **WHEN** a critical control has no `data-testid`
- **THEN** the attribute is added to the portal source as part of the flow's
  task, and the test targets it.

### Requirement: UI-observable boundary is enforced by the harness

The harness SHALL provide navigation, auth, and DOM helpers only. It SHALL NOT
ship helpers that assert backend business outcomes (limit results, payout math,
idempotency, ProblemDetail shapes).

#### Scenario: No business-assertion helper exists

- **WHEN** an author looks for a way to assert a limit outcome from the browser
- **THEN** the harness offers none, steering the assertion to the owning layer.
