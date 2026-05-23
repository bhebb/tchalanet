# Tasks — add-domain-promotion

## 1. Domain/API

- [x] Create `core.promotion.api.query.EvaluatePromotionsQuery`.
- [x] Create `core.promotion.api.model.PromotionEvaluationContext`.
- [x] Create `PromotionDecision`, `FreeLineGrant`, `PayoutModifier`, `DiscountModifier`, `CommissionModifier`, `PromotionNotice`, `AppliedPromotionSnapshot`.
- [x] Create enums `PromotionPhase`, `PromotionEffectType`, `PromotionConditionType`, `PrizeRank` if not already available.

## 2. Persistence

- [ ] Add Flyway migration for `promotion_rule`.
- [ ] Add Flyway migration or sales migration for `ticket_line_applied_rule`.
- [ ] Add RLS policies.
- [ ] Add JPA entity/repository/reader adapter.
- [ ] Add validation for `condition_json` and `effect_json`.

## 3. Engine V1

- [x] Add `PromotionRuleEngine` interface.
- [x] Add `SimplePromotionRuleEngine`.
- [x] Implement active window matching using rule timezone.
- [ ] Implement `CART_PAID_TOTAL`, `SALE_DATE`, `SALE_TIME_WINDOW`, `GAME_CODE`, `DRAW_CHANNEL`, `OUTLET`, `TERMINAL`, `PRIZE_RANK`, `ONLINE_ONLY` conditions.
- [ ] Implement `FREE_GAME_LINE`, `PAYOUT_MULTIPLIER_OVERRIDE`, `PAYOUT_MULTIPLIER_BOOST`, `PAYOUT_FIXED_BONUS`, `DISCOUNT_FIXED`, `DISCOUNT_PERCENT` effects.
- [ ] Implement stacking/conflict resolution.

## 4. Sales integration

- [ ] Preview calls `EvaluatePromotionsQuery`.
- [x] Confirmation revalidates promotions.
- [ ] Client-forced free line rejected if not in decision.
- [ ] Store applied snapshots on ticket/lines.
- [x] Include promotion notices in API response.

## 5. Settlement/payout integration

- [ ] Settlement reads applied snapshots.
- [ ] Payout calculator applies tenant-specific multiplier snapshots.
- [ ] Payout result includes promotion effect explanation for audit/receipt.

## 6. Admin APIs

- [ ] Tenant admin list/create/update/activate/archive promotion rules.
- [ ] Test rule endpoint returns matched rules/effects/snapshots.
- [ ] Audit all writes.
- [ ] Protect routes with permissions.

## 7. Tests

- [ ] Unit tests for engine matching/effects/conflicts.
- [ ] Integration tests with RLS.
- [ ] Sales preview/confirmation tests.
- [ ] Settlement tests for snapshot stability.
- [ ] Admin validation tests.
