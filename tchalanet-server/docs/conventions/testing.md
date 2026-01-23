# Testing Rules (Server)

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server  
> **Goal**: fast, reliable tests that validate business logic and prevent regressions without over-testing Spring.

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**  
They validate domain + application behavior with **in-memory ports** (preferred) or minimal fakes.

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
They are reserved for critical, high-risk flows where unit tests cannot fully validate correctness.

### Allowed scopes for integration tests

Run integration tests only for:

- **Security / auth / permissions** (Keycloak/JWT claims, scope routing)
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
- Focus on **end-to-end outcome**, not internal structure
- Assert **tenant isolation explicitly** for multi-tenant tables
- For HTTP-level integration tests: validate
  - `ApiResponse<T>` wrapping on 2xx
  - `ProblemDetail` (never wrapped) on errors
  - required headers (e.g., requestId if applicable)

### MUST NOT

- Don't create integration tests for every controller/handler
- Don't rely on time, external services, or random data (unless fixed seed)
- Don't duplicate unit test coverage

---

## 3) Practical guidance (how we choose)

Use this decision rule:

- **If logic can be validated with in-memory ports** → unit test
- **If correctness depends on Postgres** (RLS, constraints, functions, transaction boundaries) → integration test
- **If it's not critical/high-risk** → no integration test

---

## 4) Recommended minimal integration test set (baseline)

Keep a small suite like:

- **RLS isolation**: tenant A cannot read tenant B data
- **deleted_visibility**: active/deleted/all behaviors
- **AfterCommit behavior**: side effects only after commit (or explicit REQUIRES_NEW behavior)
- **Idempotency**: duplicate idempotency key does not duplicate money effects
- **ApiResponse vs ProblemDetail**: 2xx wrapped, errors not wrapped
