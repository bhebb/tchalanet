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

### Weight per priority domain (where the *decisive* test lives)

Every critical domain **splits**: its pure rules stay Unit (all permutations),
but its decisive risk may sit one layer up. Read the "decisive" column as *the
layer that proves the thing that actually breaks* — not the only layer touched.

| Domain | Decisive | Unit owns (permutations) | Integration owns (one wired composition) |
|---|---|---|---|
| **Limit policy** | **Unit** | engine + evaluators (below / at / above) | configured limit blocks → **no ticket persisted** |
| **Draw** (schedule) | **Unit** | cutoff / schedule / due calculators | draw generation persisted |
| **Sales** | **IT + E2E** | acceptance / charge / money / context resolver | prepare→confirm pipeline + **idempotency-store** + persistence |
| **DrawResult / settlement** | **IT** (money) | lifecycle / status / variant / explanation | apply idempotent → **one settlement**, stats persisted |
| **Maryaj gratis** (promotion) | **IT** | effect appliers (charge / odds / snapshot) | active campaign → **promo line persisted** (+ auto-select TTL) |
| **Game config** (catalog) | **IT** | bet-option combinations, profile resolution | config → **sellable catalog** persisted + exposed |
| **core.pricing** | **IT** (money + cache) | odds/payout resolution + override precedence | rule upsert → **money snapshot** + cache invalidation + RLS; Ensure-Haiti-rules |
| **Auth flow** | **IT** (framework) | pure policies (scope / surface / actor / verification) | Spring Security filter-chain: **JWT iss/aud validation**, scope routing, provider select, tenant override, user bootstrap |
| **Haiti** (DEFAULT_HAITI_LOTTERY profile) | **IT / E2E** | ~no pure logic | provision Haiti → **expected games/draws/pricing/catalog** exist |
| **Features** (BFF / orchestration) | **IT** | ~no pure logic | orchestration + **contract** (ApiResponse / ProblemDetail) |

> **Auth is the cautionary case.** JWT `iss`/`aud` validation, provider selection
> and scope routing are framework seams a Spring Security integration test proves
> and a unit test cannot. A single missing `FIREBASE_PROJECT_ID` (wrong `iss`)
> once turned the whole E2E suite red — the kind of regression an integration
> test catches cheaply, long before the stack.

**Do not** re-test in Integration what Unit already proved (every limit / maryaj /
odds permutation). Integration takes **one** wired case per branch, never the
cartesian product.

---

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**
They validate domain + application behavior with **in-memory ports** (preferred)
or minimal fakes. All permutations of a pure rule live here — this is the base of
the pyramid, so the layers above stay thin.

Test only **logic carriers**: evaluators, appliers, calculators, resolvers, state
machines. **Not** commands, handlers, DTOs, entities, adapters, controllers.

### Do NOT unit-test — data carriers (exclude from tests AND coverage)

These have no behavior to prove (Lombok/records, plain fields); testing them tests
the compiler. Exclude by name pattern from unit tests **and** from the JaCoCo
coverage denominator so they don't dilute the signal:

- `**/*Command.java` / `**/*Command.class` — CQRS command objects
- `**/*Query.java` / `**/*Query.class` — CQRS query objects
- `**/*Request.java` — inbound HTTP DTOs
- `**/*Response.java` — outbound HTTP DTOs
- (same treatment for `*Result`, `*View`, `*Dto`, `*Entity`, `*Mapper` generated code)

> If a would-be data carrier grows real logic (validation, derivation), that logic
> belongs in a **named validator/resolver** that *is* unit-tested — not inline in
> the Command/Request. Keep carriers dumb.

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

## 1b) Filters, interceptors & servlet-level components

Servlet filters straddle Unit and Integration. **Most of a filter's own decision
logic is a UNIT test** — you do not need a Spring context to prove it. Use the
Spring servlet mocks and fake the injected collaborators, exactly like
`RequiredRequestIdFilterTest` and `TchContextFilterSlice5Test`:

- `MockHttpServletRequest` / `MockHttpServletResponse` (set path + headers)
- `Mockito.mock(FilterChain.class)` (or `MockFilterChain`)
- mock/fake the collaborators (resolvers, context factory, binder, `ObjectProvider`)

Then drive `filter.doFilter(req, res, chain)` and assert **the filter's own
behavior**:

1. the branch taken (status via `res.getStatus()`, error code in the body);
2. whether the chain proceeded — `verify(chain, times(1)/never()).doFilter(...)`;
3. side effects it owns (request attributes bound, `MDC`/context set **and
   cleared in `finally`**, response headers).

### Worked example — `TchContextFilter` (`tchalanet-common`)

High-risk filter (tenant override, RLS activation via bind, act-as-terminal
bridge). Each branch below is a **unit** test with mocked collaborators:

- **`shouldNotFilter`** — portal-handoff `.../consume` paths are bypassed.
- **Tenant override without reason** — `resolvedAccess.tenantOverride()` true and
  `X-Tch-Override-Reason` blank → `sendError(403, ...)`, chain **never** called.
- **Act-as-terminal role gate** — `X-Tch-Act-As-Terminal` set: applied when ctx
  is TENANT_ADMIN/SUPER_ADMIN, **ignored** otherwise.
- **Malformed act-as-terminal UUID** — bad value → warn + ignored, chain **still**
  proceeds (downstream rejects).
- **Null context** — hydrate/resolve returns null → chain **never** called (response
  already handled by the resolver).
- **Cleanup** — `contextBinder.clear(req)` runs in `finally`, including when the
  chain throws.

### One INTEGRATION test on top (what mocks cannot prove)

Add a **single** wired test — MockMvc / real Spring Security chain +
Testcontainers — for the two things a unit test structurally cannot:

- **Chain placement/order** — `@Order(LOWEST_PRECEDENCE - 50)` means the filter
  runs after access resolution; a mock can't prove ordering.
- **RLS actually activates** — after `contextBinder.bind`, a tenant-scoped query
  sees only its tenant's rows (real Postgres RLS), and a cross-tenant fetch → 404.

Do **not** re-enumerate every header permutation in the integration test — those
are unit. IT proves the seam is wired; unit proves the decisions.

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
