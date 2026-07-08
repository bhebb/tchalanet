# Design: Spring Integration Business Flows

## Test Levels

### Unit

Use unit tests for pure domain/application rules with in-memory ports.

### Spring Integration

Use Spring integration tests when correctness depends on Spring wiring,
transactions, Flyway schema, Postgres constraints/RLS, idempotency aspects,
or persistence projections.

These tests SHOULD run with:

- `@SpringBootTest` or focused Spring slices only when the full context is not
  needed.
- Testcontainers Postgres with Flyway migrations.
- Deterministic clocks and deterministic seed data.
- Local JWT/test security for business-flow tests so the test is not blocked by
  an external identity provider.
- Firebase Auth Emulator only in a dedicated identity-provider integration
  suite.

### E2E

The Python suite in `testing/e2e` stays full-stack: API over HTTP plus real auth
provider/local stack, Docker compose services, and client-like requests.

Do not duplicate the same scenario in both layers unless each layer verifies a
different risk.

## Proposed Harness

Create a small reusable Spring integration test harness:

- `BusinessRuntimeIntegrationTestBase`
  - starts Testcontainers Postgres
  - exposes dynamic datasource properties
  - enables Flyway
  - sets identity provider to `local-jwt` or a test-auth profile
  - disables external communication delivery
  - provides helper methods for tenant/admin/cashier contexts

- `BusinessRuntimeScenarioFixtures`
  - creates/provisions tenant
  - creates tenant admin / seller terminal context
  - configures tenant settings, game availability, draw channel, odds, and open
    draw
  - inserts only through public application APIs/commands where possible

- `BusinessRuntimeHttpClient`
  - optional MockMvc/TestRestTemplate helper
  - asserts `ApiResponse<T>` on success
  - asserts `ProblemDetail` on errors

## Integration Case Set

### 1. Onboarding Defaults

Provision a tenant and assert persisted defaults:

- tenant display name/code
- tenant settings JSON sections
- default theme assignment
- default tenant profile features, including Maryaj gratis enablement when the
  chosen profile supports it
- subscription/entitlement baseline needed to create seller terminals

### 2. Setup Readiness

Configure the tenant through admin surfaces and assert readiness transitions:

- settings ready after required tenant settings are present
- games ready only after required game/pricing/draw limits are configured
- draw channels ready after at least one active channel is usable
- seller terminal creation remains blocked until required setup gates pass

### 3. Sale Blocked By Limit

Configure a tenant/draw-channel/number limit, then sell the blocked selection:

- preview returns a rejected/needs-change decision with a clear issue code
- final sell returns rejected outcome or ProblemDetail with the same business
  code
- no ticket is persisted
- exposure is not incremented

Use blocking/stake-exposure limits only. Potential-payout limit definitions are
not part of V0 admin integration scenarios.

### 4. Maryaj Gratis Promotion

Instantiate the default Maryaj gratis promotion and sell an eligible paid ticket:

- promotion campaign is active and persisted
- sale preparation/final sale adds the generated `HT_MARYAJ_GRATIS` line
- generated line has `origin=PROMOTION`, `pricingSource=PROMOTION`, stake zero,
  payout base amount, promotion decision id, and label/effect metadata
- seller does not need to manually choose Maryaj gratis options

### 5. Print And Reprint Promotions

Print the ticket from the Maryaj gratis scenario, then print it again:

- first print returns a receipt document with original copy marker
- reprint returns a receipt document with duplicate copy marker
- both prints include the promotional Maryaj gratis line/label
- print count is recorded without changing ticket money or lines

## Firebase Emulator

Firebase Emulator tests SHOULD be separate and limited to identity integration:

- verified Firebase token resolves to an app user or seller terminal
- bootstrap/provisioning compensation works when local persistence fails
- Firebase roles/claims are not authorization source; DB-owned permissions win

Business runtime integration tests SHOULD NOT require Firebase Emulator unless
they specifically validate identity provider behavior.

## Non-Goals

- No browser rendering.
- No Slack/email/edge delivery.
- No exhaustive controller CRUD matrix.
- No duplicate of every Python E2E test.
- No Testcontainers replacement for the full local product stack.
