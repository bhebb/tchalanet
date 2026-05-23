# Specification: domain-promotion

## ADDED Requirements

### Requirement: Promotion domain exists as a core bounded context

The system SHALL introduce `core.promotion` as the owner of tenant-scoped commercial game rules.

#### Scenario: Sales evaluates promotions without knowing engine details

- GIVEN sales has a cart and operational context
- WHEN sales needs promotion decisions
- THEN sales SHALL call `EvaluatePromotionsQuery`
- AND sales SHALL receive a `PromotionDecision`
- AND sales SHALL NOT import `core.promotion.internal.*`.

### Requirement: Promotion rules are tenant-scoped

Promotion rules SHALL belong to one tenant and be protected by RLS.

#### Scenario: Same code exists for two tenants

- GIVEN tenant A and tenant B each define `CHRISTMAS_FIRST_PRIZE_BOOST`
- WHEN tenant A evaluates promotions
- THEN only tenant A rules SHALL be considered.

### Requirement: Tenant-specific payout multipliers are supported

Promotion rules SHALL support tenant-specific payout multiplier effects.

#### Scenario: Tenant A pays first prize 60x before noon

- GIVEN tenant A has an active `PAYOUT_MULTIPLIER_OVERRIDE` rule
- AND `effect_json.appliedMultiplier = 60`
- AND sale time is within the active window
- WHEN promotions are evaluated for a matching line
- THEN the decision SHALL include a `PayoutModifier` with `appliedMultiplier = 60`.

### Requirement: Effects are canonical and typed

Promotion decisions SHALL use canonical effect types, not engine-specific rule syntax.

#### Scenario: Future DMN engine returns a decision

- GIVEN an internal DMN engine evaluates a rule
- WHEN it returns a result
- THEN the adapter SHALL map it to `PromotionDecision`
- AND no consumer SHALL receive DMN-specific objects.

### Requirement: Rule engine implementation is replaceable

The V1 implementation SHALL use a `PromotionRuleEngine` internal interface.

#### Scenario: Replace engine later

- GIVEN `SimplePromotionRuleEngine` is replaced by `DmnPromotionRuleEngine`
- WHEN sales evaluates promotions
- THEN sales code SHALL remain unchanged.

### Requirement: Promotions are online-only by default

Rules SHALL be online-only unless explicitly configured otherwise.

#### Scenario: Offline context evaluates online-only rule

- GIVEN a rule has `offline_allowed = false`
- AND the context source is offline
- WHEN promotions are evaluated
- THEN the rule SHALL not apply.
