# Change: unit-coverage-critical-domains-v1

## Why

The test pyramid is inverted on the business-critical sell flows. The
sell-time decision logic — limit evaluators, promotion effect appliers, sale
acceptance, odds override rules — has **no unit tests**, while the surrounding
config/state layer (campaign state machines, money calculators, validators)
does. As a result the combinatorial matrix
`{maryaj gratis ON/OFF} × {limit configured ON/OFF} × {terminal override ON/OFF}`
falls onto the slow Spring integration and Python E2E layers instead of fast,
deterministic unit tests.

Coverage ratios (server, `*Test` files vs `main` sources):

- `tchalanet-core` — 48 / 1138 (~4%)
- `tchalanet-features` — 7 / 251 (~3%)
- `tchalanet-platform` — 50 / 771 (~6%)

This change builds the **base of the pyramid** so that:

- the combinatorial matrix lives in unit tests,
- Spring integration only asserts the wired composition (one pass),
- E2E asserts a single full-stack happy path.

It is the level-0 companion to `test-strategy-separation-v1` (cross-project
test-layer contract) and directly unblocks the still-`[ ]` limits/promotions
tasks in `e2e-business-runtime-v1` and `perf-load-testing-locust-v1`.

## What Changes

- Add fast, Spring-free unit tests (JUnit + AssertJ) for the pure business-logic
  classes of the critical domains, targeting **logic carriers only** —
  evaluators, appliers, calculators, resolvers, state machines — **not**
  commands, handlers, DTOs, entities, or adapters.
- Cover the sell-time decision matrix at the unit level:
  - **Limits** — the untested `LimitEvaluationEngine` and its per-selection /
    per-line / per-ticket evaluators (below / at / above / limit-absent).
  - **Sale decision & charge** — `SaleAcceptanceEvaluator`, `SaleChargeCalculator`.
  - **Promotion / Maryaj gratis at sell time** — the effect appliers
    (`SalePromotionEffectApplier`, `PromotionChargeApplier`,
    `PromotionOddsBoostApplier`) that were untested while campaign config was tested.
  - **Terminal odds override** — `SellerTerminalOddsOverride` domain rules and
    its interaction with the promotion odds boost.
  - **Draw result / settlement / payout** — `SettlementLifecycle`,
    `TicketSettlementStatus`, `ResultSlotScheduleCalculator`,
    `ResultSlotSourceConfigResolver`, `DrawProcessingDuePolicy`.
  - **Print / receipt** — `TicketPrintPolicyService`,
    `DocumentPrintProfileResolver`, receipt label/i18n resolvers.
- Audit `tchalanet-features` (7/251) to separate real logic from plumbing before
  writing, so effort is spent on branch-bearing code.
- Keep the reinforcement list **explicit** (named classes), not a raw coverage
  percentage, to avoid inflating counts with plumbing tests.

## Impact

- **No production runtime change.** Test-only additions under each module's
  `src/test/java`, next to the class under test.
- Fast feedback: runs inside the normal `./mvnw test` build, no Testcontainers,
  no Spring context — measured in milliseconds, not seconds.
- Pushes the combinatorial matrix down: `spring-integration-business-flows-v1`
  and `e2e-business-runtime-v1` can drop redundant matrix permutations and keep
  one composition / happy-path assertion each.

## Non-goals

- No coverage of plumbing (commands, handlers, requests/responses, JPA
  entities/adapters, controllers) unless a class carries branching logic.
- No Spring-context or Testcontainers tests — those stay in
  `spring-integration-business-flows-v1`.
- No blocking CI coverage gate in v1 (may follow once the critical packages are
  covered).
- No web/mobile unit tests in this change (backend-first; web/mobile tracked
  separately).
