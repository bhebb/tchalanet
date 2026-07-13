# Tasks — unit-coverage-critical-domains-v1

Scope: fast, Spring-free unit tests (JUnit + AssertJ) on **logic carriers only**.
Order matters: **A first** — biggest gap, pure predicate, unblocks limits/promo
tasks in `e2e-business-runtime-v1` and Locust.

Focused command:

```bash
./mvnw -pl tchalanet-core -am -Dsurefire.failIfNoSpecifiedTests=false test
```

## A. Limits engine (create — highest priority)

- [ ] `LimitEvaluationEngine` — orchestration: below / at / above / limit-absent.
- [ ] `MaxStakeExposurePerSelectionPerDrawEvaluator`.
- [ ] `MaxSalesCountPerSelectionPerDrawEvaluator`.
- [ ] `MaxPotentialPayoutPerLineEvaluator`.
- [ ] `MaxPotentialPayoutPerTicketEvaluator`.
- [ ] `MaxPotentialPayoutExposurePerSelectionPerDrawEvaluator`.
- [ ] `BlockBetTypeEvaluator`.
- [ ] `BlockSelectionPerDrawEvaluator`.
- [ ] `EffectiveLimitRule` / `EffectiveLimits` / `LimitBreach` / `LimitEvaluationResult`
      construction + edge cases (empty rules, overlapping rules).

## B. Sale decision & charge (reinforce)

- [ ] `SaleAcceptanceEvaluator` — each decision branch (accept / block / warn).
- [ ] `SaleChargeCalculator` — charge with and without promotion effects.
- [ ] `PosSaleContextResolver` — context resolution edge cases.

## C. Promotion / Maryaj gratis at sell time (reinforce)

- [ ] `SalePromotionEffectApplier` — applies / no-op when no eligible campaign.
- [ ] `PromotionChargeApplier` — charge waived vs unchanged.
- [ ] `PromotionOddsBoostApplier` — boosted odds vs base odds untouched.
- [ ] `AppliedSalePromotionEffects` — snapshot correctness.
- [ ] `PromotionContextHasher` — stable hash for equal contexts, differs otherwise.
- [ ] Matrix: sell **with** maryaj gratis active vs **without** any campaign configured.

## D. Terminal odds override (reinforce)

- [ ] `SellerTerminalOddsOverride` — domain rules (active / inactive / expired).
- [ ] Override × promotion odds boost interaction (which wins, no double-apply).
- [ ] Matrix: sell **with** override vs **without** override.

## E. Draw result / settlement / payout (reinforce)

- [ ] `SettlementLifecycle` — state transitions (all 6 methods), illegal transitions.
- [ ] `TicketSettlementStatus` — status derivation.
- [ ] `ResultSlotScheduleCalculator` — slot schedule computation.
- [ ] `ResultSlotSourceConfigResolver` — source config resolution.
- [ ] `DrawProcessingDuePolicy` — due / not-due boundaries.

## F. Print / receipt (reinforce)

- [ ] `TicketPrintPolicyService` — print allowed / blocked policy.
- [ ] `DocumentPrintProfileResolver` — profile selection.
- [ ] `TicketReceiptI18nResolver` / `TicketReceiptLabelResolver` — label/locale resolution.

## G. Audit & access resolvers

- [ ] Audit `tchalanet-features` (7/251): list real logic carriers vs plumbing,
      then add tests for the logic carriers only (or record "plumbing only, no
      unit tests needed").
- [ ] Platform access resolvers: `ApiScopeResolver`, `ClientSurfacePolicy`,
      `ClientSurfaceResolver`, `BffSlicePolicy`.

## H. Wrap-up

- [ ] Update `docs/conventions/testing.md` with the "logic carriers only" rule
      and the unit-vs-integration-vs-e2e matrix boundary.
- [ ] Confirm the matrix permutations removed/kept in
      `spring-integration-business-flows-v1` and `e2e-business-runtime-v1`
      (avoid duplication now covered by unit).
