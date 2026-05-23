# Design — core.promotion

## Architecture

`core.promotion` follows the core Clean Architecture / CQRS archetype.

```text
core/promotion/api/
  query/
  model/

core/promotion/internal/
  domain/model/
  application/query/handler/
  application/engine/
  application/port/out/
  infra/persistence/
  infra/web/admin/
```

## Public API

```java
EvaluatePromotionsQuery(PromotionEvaluationContext context) -> PromotionDecision
```

Consumers use `QueryBus.ask(new EvaluatePromotionsQuery(context))`.

## Internal engine boundary

```java
interface PromotionRuleEngine {
  PromotionDecision evaluate(List<PromotionRuleDefinition> rules, PromotionEvaluationContext ctx);
}
```

V1 implementation: `SimplePromotionRuleEngine`.

Future implementation: DMN/Drools/JSON rules adapter. Only `core.promotion.internal.engine` changes.

## Tenant-specific multipliers

All promotion rules are tenant-scoped. `PAYOUT_MULTIPLIER_OVERRIDE` and `PAYOUT_MULTIPLIER_BOOST` effects carry the tenant-specific value in `effect_json` and return a typed `PayoutModifier`.

Example effect JSON:

```json
{
  "effectType": "PAYOUT_MULTIPLIER_OVERRIDE",
  "gameCode": "BOLET",
  "prizeRank": "FIRST",
  "appliedMultiplier": 60
}
```

At sale confirmation, sales stores a canonical snapshot:

```json
{
  "ruleCode": "CHRISTMAS_FIRST_PRIZE_60X",
  "ruleVersion": 3,
  "effectType": "PAYOUT_MULTIPLIER_OVERRIDE",
  "gameCode": "BOLET",
  "prizeRank": "FIRST",
  "appliedMultiplier": 60
}
```

Settlement uses the snapshot, not the current promotion rule.

## Persistence

Main tables:

```text
promotion_rule
promotion_rule_audit optional / Envers
promotion_rule_publication optional later
ticket_line_applied_rule
```

`promotion_rule` is tenant-scoped and RLS protected.

`ticket_line_applied_rule` belongs with sales persistence but references `promotion_rule` for traceability.

## Rule lifecycle

V1 lifecycle:

```text
DRAFT optional later
ACTIVE
INACTIVE
ARCHIVED
```

The simplest implementation may use `active boolean` + `archived_at`.

## Stacking/conflicts

Each rule has:

```text
priority
stackable
exclusive_group
```

Conflict rule:

```text
- Same target payout override cannot stack.
- Highest priority wins.
- Equal priority conflict throws configuration error in preview/admin validation.
```

## Offline

Default `offline_allowed = false`.

Offline promotion support is deferred to a signed rule snapshot model.
