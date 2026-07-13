# Testing Rules (Server)

> **Status**: NORMATIVE
> **Applies to**: tchalanet-server (backend). Web/mobile have their own guides.
> **Goal**: prove business risk at the **cheapest sufficient layer**, fast and
> reliably, without re-testing the same rule twice or over-testing the framework.

## 0) Objective — why we test, and the pyramid

We test to **prove a risk**, not to raise a coverage number. Before writing a
test, answer: *what breaks if this is wrong, and what is the smallest layer that
proves it won't?*

The platform has one **cross-project contract** that decides where each thing is
tested — read it once and keep it open:
**`openspec/changes/test-strategy-separation-v1`** (boundary rule = §0 / design,
anti-duplication checklist = §5). This backend doc is the server-side application
of that contract.

| Layer | Tool | Owns | Owner change |
|---|---|---|---|
| **Unit** (default, most tests) | Java · JUnit 5 + AssertJ, no Spring, no DB | Every branch of a pure logic class — **all permutations** | `unit-coverage-critical-domains-v1` |
| **Integration** (few, critical) | Java · Testcontainers Postgres | **One** representative per branch: persistence, RLS, idempotency-store, transaction, HTTP contract | `spring-integration-business-flows-v1` |
| **E2E** (thin tip) | Python · `testing/e2e`, full deployed stack + real auth | **The** happy path per journey + idempotent replay + multi-tenant isolation | `e2e-business-runtime-v1` |
| **Load** (adjacent) | Python · Locust | Capacity / latency only — **no** business assertion | `perf-load-testing-locust-v1` |
| **Web** (adjacent) | Playwright · `tchalanet-web/apps/web-e2e` | UI-observable behavior only (rendering, guards, form feedback) | `web-e2e-critical-flows-v1` |

**The boundary rule (single source of truth):**

> **Pure decision / calculation** → Unit ·
> needs **real DB / transaction / RLS / idempotency-store**, but one seeded
> endpoint suffices → **Integration** ·
> needs the **whole stack + real auth + multiple client roles** → **E2E**.

**Anti-duplication:** a permutation is asserted **once** (Unit). Integration takes
**one** representative per branch. E2E takes **the** happy path. No layer repeats
what the layer below already proved. If an assertion is already proven one layer
down — **delete it**.

---

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**
They validate domain + application behavior with **in-memory ports** (preferred)
or minimal fakes. All permutations of a pure rule live here — this is the base of
the pyramid, so the layers above stay thin.

Test only **logic carriers**: evaluators, appliers, calculators, resolvers, state
machines. **Not** commands, handlers, DTOs, entities, adapters, controllers.

### MUST

- **JUnit 5**
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assertions with `assertAll(...)`** when relevant
- Use `@Nested` for scenarios
- Prefer **in-memory ports** over mocks
- Method names MUST be Java-compatible **camelCase**
- Use `@DisplayName("should <expected> when <condition>")` on test methods (canonical report description)
- Naming convention for methods: `should<Expected>When<Condition>`

### MUST NOT

- Don’t test Spring wiring in unit tests
- Don’t mock everything (avoid testing mocks instead of logic)
- Don’t assert implementation details (private calls, internal ordering, etc.)

### Example

```java
@Nested
@DisplayName("When URL lang is provided")
class WhenUrlLangProvided {

  @Test
  @DisplayName("should use URL lang when allowed")
  void shouldUseUrlLangWhenAllowed() {
    // given
    var input = "...";

    // when
    var res = resolve(input);

    // then
    assertThat(res).isEqualTo("fr");
  }
}
```

---

## 2) Integration tests (CRITICAL FEATURES ONLY)

Integration tests are limited on purpose.
They are reserved for critical, high-risk flows where unit tests cannot fully
validate correctness. Each item asserts **one wired composition** — never the
per-rule permutations (those belong to Unit).

### Allowed scopes for integration tests

Run integration tests only for:

- **Security / auth / permissions** (Firebase/JWT claims, scope routing)
- **Tenant isolation / RLS** (tenant leakage prevention, deleted_visibility)
- **Money / settlement** (ledger correctness, payout flows, idempotency)
- **Batch / scheduler critical pipelines** (results fetch/apply/settlement)
- **Persistence correctness** where DB behavior matters:
  - constraints, unique keys for idempotency
  - triggers/functions used by RLS
  - Envers revisions metadata integrity
- **API contract** for key endpoints (response envelope + ProblemDetail)

### MUST

- Use **Testcontainers** for Postgres when RLS/SQL behavior matters
- Keep integration tests **few, stable, deterministic**
- Assert **one representative** per branch, focused on **end-to-end outcome**, not internal structure
- Assert **tenant isolation explicitly** for multi-tenant tables
- For HTTP-level integration tests: validate
  - `ApiResponse<T>` wrapping on 2xx
  - `ProblemDetail` (never wrapped) on errors
  - required headers (e.g., requestId if applicable)

### MUST NOT

- Don't create integration tests for every controller/handler
- Don't rely on time, external services, or random data (unless fixed seed)
- Don't duplicate unit test coverage (no per-rule permutations here)

---

## 3) E2E and Load (out of the unit/integration scope)

Owned by dedicated changes, not this doc — pointers only so backend authors know
where a whole-stack or capacity concern belongs.

- **E2E** (`testing/e2e`, Python, `e2e-business-runtime-v1`) — drives the real
  deployed stack (API + PG + edge + Firebase emulator) as a client, multiple
  roles. Asserts **the** happy path + idempotent replay + multi-tenant isolation.
  E2E does **not** re-assert ProblemDetail shapes, idempotency-store internals, or
  enumerate the stake/limit/promo matrix — those are Integration / Unit.
- **Load** (`testing/e2e/loadtest`, Locust, `perf-load-testing-locust-v1`) —
  capacity / latency (p50/p95/p99, RPS, error-rate) only. **No** business
  assertion; reuses the E2E client. Never mix load into E2E.

---

## 4) Practical guidance (how we choose)

Decision rule (apply top to bottom, stop at the first match):

1. **Pure decision/calc, validated with in-memory ports** → unit test. Stop.
2. **Correctness depends on Postgres** (RLS, constraints, functions, transaction
   boundaries) or the HTTP contract → integration test, **one** representative.
3. **Needs the whole stack + real auth + multiple roles** → E2E, **happy path** only.
4. **Same assertion already proven one layer down?** → **delete it.**
5. **Not critical / not high-risk** → no integration/E2E test.

---

## 5) Recommended minimal integration test set (baseline)

Keep a small suite like:

- **RLS isolation**: tenant A cannot read tenant B data
- **deleted_visibility**: active/deleted/all behaviors
- **AfterCommit behavior**: side effects only after commit (or explicit REQUIRES_NEW behavior)
- **Idempotency**: duplicate idempotency key does not duplicate money effects
- **ApiResponse vs ProblemDetail**: 2xx wrapped, errors not wrapped
