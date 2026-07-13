# Design — Test Strategy & Layer Separation

The concrete allocation. Three lists — **unit classes**, **integration flows**,
**e2e flows** — built so nothing is tested twice.

## 0. Layer contract (what each layer MUST and MUST NOT do)

| Layer | Tool | MUST assert | MUST NOT do |
|---|---|---|---|
| **Unit** | Java JUnit/AssertJ | Every branch of a pure logic class (all permutations) | Touch DB, Spring context, HTTP |
| **Integration** | Java + Testcontainers PG | **One** representative per branch: persistence, RLS, idempotency-store, transaction, HTTP contract (ProblemDetail) | Re-test permutations Unit owns; drive multiple client roles end-to-end |
| **E2E** | Python (deployed stack) | **The** happy path per flow + idempotent replay + multi-tenant isolation | Re-assert the API contract; enumerate the matrix; deep payout math |
| **Load** | Python Locust | Capacity/latency (p50/p95/p99, RPS, error-rate) | Any business assertion |

---

## 1. UNIT — Java classes to test (all permutations live here)

Logic carriers only (evaluator / applier / calculator / resolver / state
machine). **Not** commands, handlers, DTOs, entities, adapters, controllers.

### Limits (`core/limitpolicy`) — create
- `LimitEvaluationEngine` — orchestration: below / at / above / limit-absent
- `MaxStakeExposurePerSelectionPerDrawEvaluator`
- `MaxSalesCountPerSelectionPerDrawEvaluator`
- `MaxPotentialPayoutPerLineEvaluator`
- `MaxPotentialPayoutPerTicketEvaluator`
- `MaxPotentialPayoutExposurePerSelectionPerDrawEvaluator`
- `BlockBetTypeEvaluator`
- `BlockSelectionPerDrawEvaluator`
- `EffectiveLimitRule` / `EffectiveLimits` / `LimitBreach` / `LimitEvaluationResult`

### Sale decision & charge (`core/sales`) — reinforce
- `SaleAcceptanceEvaluator` — accept / block / warn branches
- `SaleChargeCalculator` — with and without promotion effects
- `PosSaleContextResolver` — context edge cases

### Promotion / Maryaj at sell time (`core/sales/.../promotion`) — reinforce
- `SalePromotionEffectApplier` — applies vs no-op
- `PromotionChargeApplier` — charge waived vs unchanged
- `PromotionOddsBoostApplier` — boosted vs base odds untouched
- `AppliedSalePromotionEffects` — snapshot correctness
- `PromotionContextHasher` — stable/differing hash

### Terminal odds override (`core/sales`) — reinforce
- `SellerTerminalOddsOverride` — active / inactive / expired; precedence vs promo boost

### Draw result / settlement (`core/drawresult`, `core/sales`) — reinforce
- `SettlementLifecycle` — legal/illegal transitions
- `TicketSettlementStatus` — status derivation
- `ResultSlotScheduleCalculator`
- `ResultSlotSourceConfigResolver`
- `DrawProcessingDuePolicy` — due/not-due boundaries

### Print / receipt (`core/sales/.../print`) — reinforce
- `TicketPrintPolicyService`
- `DocumentPrintProfileResolver`
- `TicketReceiptI18nResolver` / `TicketReceiptLabelResolver`

### Access (`platform`) — audit
- `ApiScopeResolver`, `ClientSurfacePolicy`, `ClientSurfaceResolver`, `BffSlicePolicy`

> Already unit-tested — do NOT duplicate: `SaleMoneyCalculator`,
> `SaleCommandValidator`, `SalePreparationStateMachine`,
> `PromotionCampaignStateMachine`, `PromotionRuleEvaluator`,
> `PromotionCampaignActivationPolicy`, `DrawCutoffRule`, `DrawScheduleCalculator`,
> `SettlementVariantResolver`, `SettlementExplanationService`, `EntitlementService`.

Owner change: `tchalanet-server/.../unit-coverage-critical-domains-v1`.

---

## 2. INTEGRATION — Java + Testcontainers flows (one representative each)

Each item = **one** wired scenario asserting DB/RLS/idempotency/contract — not
the matrix.

- **Onboarding default persistence** — tenant provisioned → defaults persisted, read back
- **Setup readiness transition** — configure gates → readiness MISSING→READY
- **Limit-blocked sell** — configured limit blocks preview/sell, **no ticket persisted** *(done)*
- **Maryaj gratis sell** — active campaign → promotional line persisted *(done)*
- **Print / reprint** — endpoint persists, preserves promo lines + copy markers
- **Terminal bind + admin-as-terminal override** — bind + X-Tenant-Id override persist correctly
- **Draw result apply idempotent** — simulate→apply, **one settlement only** on replay, stats persisted
- **Idempotency-store** — same key + same payload → same ticket at store level
- **RLS** — cross-tenant fetch → 404, not a leak
- **Auth / identity provider** — Firebase Emulator, isolated from business tests

Owner change: `spring-integration-business-flows-v1`.
Assert **once** here what Unit already proved is composed correctly — never the
per-rule permutations.

---

## 3. E2E — Python flows (the happy path, full stack)

Full deployed stack, multiple roles, client-like. One representative per flow.

- **POS happy path** — onboard → configure → cashier sells → print → send → close
- **Idempotent sell** — same key → same ticket (replay)
- **Multi-tenant isolation under concurrency** — interleaved A/B, no RLS leak
- **Public runtime (anonymous)** — draw results, ticket verify, page-models
- **Login/context per role** — super-admin / tenant-admin / cashier
- **Draw result end-to-end** — result applied, visible on public + stats

Owner change: `e2e-business-runtime-v1`.
E2E does **not** re-assert ProblemDetail shapes, idempotency-store internals, or
enumerate stake/limit permutations — those are Integration/Unit.

---

## 4. LOAD (Python Locust) & WEB (Playwright) — adjacent, not in the pyramid

- **Load** (`perf-load-testing-locust-v1`): drives the real sell flow for
  capacity/latency; **no** business assertions; reuses the E2E client.
- **Web** (`web-e2e`, to spec separately): browser flows — login, admin config
  screens, POS sell + print feedback. UI-observable only.

---

## 5. Anti-duplication checklist (apply on every new test PR)

1. Is it a pure decision/calc? → Unit. Stop.
2. Does it need DB/RLS/idempotency/contract only? → Integration, **one** case.
3. Does it need the whole stack + roles? → E2E, **happy path** only.
4. Is the same assertion already proven one layer down? → **delete it**.
