# business-runtime-integration Spec Delta

## ADDED Requirements

### Requirement: Spring Integration Test Boundary

The server SHALL maintain Spring integration tests for critical business runtime
flows separately from Python E2E tests.

#### Scenario: Business runtime integration suite is introduced

- **WHEN** onboarding/setup/sell business flows require DB, transaction,
  idempotency, RLS, or projection validation
- **THEN** the coverage SHALL live in Spring integration tests
- **AND** the tests SHALL use Testcontainers Postgres when Postgres behavior is
  part of the assertion
- **AND** the tests SHALL NOT depend on the Python `testing/e2e` harness.

#### Scenario: E2E and integration scopes are compared

- **WHEN** a new regression test is planned
- **THEN** Spring integration SHALL cover server invariants and persisted
  outcomes
- **AND** E2E SHALL cover full-stack deployment/auth/client-like smoke
- **AND** the same test SHALL NOT be duplicated in both layers unless each layer
  asserts a distinct risk.

### Requirement: Onboarding And Setup Integration Coverage

The Spring integration suite SHALL cover tenant onboarding defaults and setup
readiness transitions.

#### Scenario: Tenant is provisioned

- **WHEN** a tenant is provisioned through the onboarding application/API path
- **THEN** tenant defaults SHALL be persisted for settings, theme, profile
  features, and baseline entitlements
- **AND** the persisted defaults SHALL be read back through runtime/admin read
  surfaces.

#### Scenario: Setup gates transition to ready

- **WHEN** tenant settings, game availability, draw channels, pricing, and draw
  readiness prerequisites are configured
- **THEN** setup readiness SHALL reflect the expected ready/blocked states
- **AND** seller terminal creation SHALL remain blocked until required gates pass.

### Requirement: Sale Limit Blocking Integration Coverage

The Spring integration suite SHALL verify that configured limits block sales
with clear business feedback.

#### Scenario: Blocked selection is sold

- **GIVEN** an active tenant, seller terminal, open draw, and active limit
  assignment for a selection
- **WHEN** the cashier previews or sells that selection
- **THEN** the sale SHALL be rejected or require changes with a stable issue code
- **AND** no ticket SHALL be persisted for a blocked final sale
- **AND** exposure SHALL NOT be incremented for that blocked sale.

### Requirement: Maryaj Gratis Promotion Integration Coverage

The Spring integration suite SHALL verify Maryaj gratis sale, print, and reprint
behavior.

#### Scenario: Eligible paid sale gets Maryaj gratis

- **GIVEN** the default Maryaj gratis campaign is active
- **WHEN** a cashier sells an eligible paid ticket
- **THEN** the persisted ticket SHALL contain a promotional `HT_MARYAJ_GRATIS`
  line
- **AND** the promotional line SHALL carry promotion origin, promotion pricing
  source, zero stake, payout base amount, decision id, and label/effect metadata
- **AND** the seller SHALL NOT need to manually choose Maryaj gratis options.

#### Scenario: Promotional ticket is printed and reprinted

- **GIVEN** a sold ticket containing a Maryaj gratis promotional line
- **WHEN** the ticket is printed and then reprinted
- **THEN** both receipt projections SHALL include the promotional line
- **AND** the first print SHALL carry the original copy marker
- **AND** the reprint SHALL carry the duplicate copy marker
- **AND** print state SHALL update without changing ticket money or lines.

### Requirement: Firebase Emulator Separation

Firebase Emulator SHALL be used only for identity-provider integration concerns,
not as a prerequisite for business runtime integration tests.

#### Scenario: Business flow integration tests run

- **WHEN** onboarding/setup/sell Spring integration tests run
- **THEN** they SHALL use local JWT/test security or equivalent deterministic
  authentication
- **AND** they SHALL NOT require Firebase Emulator.

#### Scenario: Firebase behavior is tested

- **WHEN** token verification, bootstrap, provider mapping, or provisioning
  compensation is under test
- **THEN** the test MAY use Firebase Emulator
- **AND** it SHALL live in a dedicated identity-provider integration suite.
