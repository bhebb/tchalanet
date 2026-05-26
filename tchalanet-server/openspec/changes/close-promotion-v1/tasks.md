# Tasks — close-promotion-v1

## 1. Promotion lifecycle

- [ ] Add `PromotionCampaignStateMachine`.
- [ ] Add transition enum `PromotionCampaignTransition`.
- [ ] Update lifecycle handlers to use state machine.
- [ ] Replace adapter-level `activate/pause/archive/deactivate` decisions with `changeStatus(...)`.
- [ ] Add tests for valid transitions.
- [ ] Add tests for invalid transitions: `ACTIVE -> ARCHIVED`, `ARCHIVED -> ACTIVE`, `ACTIVE -> INACTIVE`, `DRAFT -> PAUSED`.

## 2. Promotion rule persistence

- [x] Replace config JSON with typed `promotion_rule_effect`.
- [x] Add typed `promotion_rule_eligibility_line`.
- [x] Remove rule status, evaluation phase, quota key, and max uses from rule persistence.
- [x] Add/confirm `priority` on `promotion_rule`.
- [x] Ensure all promotion rule child tables extend `BaseTenantEntity` / include `tenant_id` for RLS.
- [x] Add repository methods: `findByIdAndCampaignId(...)`, `findByCampaignIdOrderByPriorityAscRuleKeyAsc(...)`, `existsByCampaignIdAndRuleKey(...)`.

## 3. Rule commands

- [x] Implement `AddPromotionRuleCommandHandler`.
- [x] Implement `UpdatePromotionRuleCommandHandler`.
- [x] Implement `DeletePromotionRuleCommandHandler`.
- [x] Implement `UpdatePromotionRuleEligibilityCommandHandler`.
- [x] Implement `UpdatePromotionRuleEffectsCommandHandler`.
- [ ] All rule mutations must evict promotion caches after commit.

## 4. Rule validation

- [x] Validate rule key is non-blank and unique within campaign.
- [x] Validate priority is non-negative.
- [x] Reject rule mutation unless campaign is `DRAFT`.
- [x] Remove quota metadata from V1.

## 5. Eligibility validation

- [x] Support only V1 conditions: `MIN_PAID_TOTAL`, `PAID_LINE_COUNT`, `BEFORE_LOCAL_TIME`.
- [x] Validate required fields: `MIN_PAID_TOTAL.amount`, `PAID_LINE_COUNT.gameCode`, `PAID_LINE_COUNT.minCount`, `BEFORE_LOCAL_TIME.time`.
- [x] Validate numeric values are positive.
- [x] Validate local time format.

## 6. Effect validation

- [x] Support only V1 effects: `FREE_GAME_LINE`, `BOOST_ODDS`, `WAIVE_CHARGE`.
- [x] Validate required fields: `FREE_GAME_LINE.gameCode`, `FREE_GAME_LINE.payoutBaseAmount`, `BOOST_ODDS.gameCode`, `BOOST_ODDS.oddsOverride`, `WAIVE_CHARGE.chargeCode`.
- [x] Validate positive money/odds values.
- [x] Store effect type on each typed effect row.

## 7. Campaign activation verification

- [ ] Add `PromotionCampaignActivationPolicy`.
- [ ] On activate: campaign must not be archived; dates coherent; at least one active rule; active rules have valid eligibility/effects JSON; effect types are V1-supported.
- [ ] Validate required game codes exist and are enabled for tenant if validator/API is available.
- [ ] If full tenant game validation is not available yet, add TODO and warning notice.

## 8. Campaign view

- [x] Update `PromotionCampaignView` to include `List<PromotionRuleView> rules`.
- [x] Add `PromotionRuleView`.
- [x] Add `PromotionEligibilityConfigView`.
- [x] Add `PromotionEffectConfigView`.
- [x] Mapper must assemble typed eligibility/effect rows into view items.
- [ ] Admin list may return empty `rules`; detail must return full rules.

## 9. Cache

- [ ] Add `PromotionCacheEvictorPort` in application port/out.
- [ ] Keep `PromotionCacheEvictor` in infra/cache only.
- [ ] Handler imports port, never infra implementation.
- [ ] Add cache specs: `core.promotion.runtime.active`, `core.promotion.campaign.by_id`, `core.promotion.campaign.admin_list`.
- [ ] Use `tenantId:campaignId` for detail cache.
- [ ] Use `tenantId:page:size:sort` for admin list cache.
- [ ] Clear admin list cache after mutations.
- [ ] Evict runtime tenant cache after activate/pause/archive/rule mutations.

## 10. Runtime resolve/apply

- [x] Use pure runtime query `EvaluatePromotionQuery`.
- [ ] Input must include sale preparation context: tenant, paid lines, paid total, game codes, local time / tenant zone, charges.
- [x] Load only ACTIVE campaigns.
- [x] Evaluate only V1 eligibility/effects.
- [x] Return decision object: decision id, matched rule ids, effects to apply, warnings/notices.
- [x] Do not mutate Sales in Promotion runtime.

## 11. Sales integration

- [ ] Add fields to `TicketLine`: `origin`, `pricingSource`, `selectionSource`, `payoutBaseAmount`, `promotionDecisionId`.
- [ ] Ensure normal lines: `origin=CUSTOMER`, `pricingSource=STANDARD`, `payoutBaseAmount=stakeAmount`.
- [x] Implement `FREE_GAME_LINE`: add promotional line, `stakeAmount=0`, `origin=PROMOTION`, `pricingSource=PROMOTION`, `payoutBaseAmount` from effect, odds from tenant `pricing_odds`.
- [x] Implement `BOOST_ODDS`: update line odds snapshot, do not update `pricing_odds`.
- [x] Implement `WAIVE_CHARGE`: update `MoneyBreakdown`, do not update pricing odds.
- [x] Persist applied promotion snapshot from Sales inside the sale transaction.
- [ ] Sale preparation order: normal lines -> charges -> money -> resolve promotion -> apply effects -> final money -> limits/autonomy -> PreparedSale.

## 12. Settlement integration

- [ ] Settlement must not call Promotion runtime.
- [ ] Settlement reads `TicketLine.payoutBaseAmount`.
- [ ] Settlement reads `TicketLine.oddsSnapshot`.
- [ ] Settlement treats `HT_MARYAJ_GRATUIT` as supported game family if required.
- [ ] Settlement must be idempotent.

## 13. Payout integration

- [ ] Payout pays settled result only.
- [ ] Payout must not call Promotion.
- [ ] Payout receipt may include promotion-origin lines for display only.
- [ ] No bonus payout logic in Promotion V1.

## 14. Ledger / Stats integration

- [ ] Ticket events should expose line origin, pricing source, promotion decision id, payout base amount, odds snapshot.
- [ ] Stats should distinguish paid customer lines, promotional lines, waived charges, boosted odds lines.
- [ ] Ledger cost allocation is not required V1 but events must not block future implementation.

## 15. Print / Preview

- [ ] Cashier preview displays promotion effects before confirmation.
- [ ] Admin detail displays campaign/rules/effects/eligibility.
- [ ] Ticket print displays `HT_MARYAJ_GRATUIT` as a normal line with promotional marker if desired.
- [ ] SMS waived charge should be visible in money breakdown/receipt if relevant.

## 16. Tests

- [ ] Unit tests for state machine.
- [ ] Unit tests for eligibility validation.
- [ ] Unit tests for effect validation.
- [ ] Unit tests for campaign activation policy.
- [ ] Integration tests for rule add/update/delete.
- [ ] Integration tests for cache eviction after commit.
- [ ] Sales tests: `FREE_GAME_LINE`, `BOOST_ODDS`, `WAIVE_CHARGE`.
- [ ] Settlement tests: no promotion re-evaluation.
- [ ] Payout tests: pays settled amount only.
