# Change: add-domain-promotion

## Summary

Introduce `core.promotion` to manage tenant-scoped commercial game rules such as free Maryaj, tenant-specific payout multiplier boosts, discounts, fixed bonuses and commission modifiers.

The domain exposes a stable contract:

```text
PromotionEvaluationContext -> PromotionDecision
```

Consumers (`core.sales`, `core.limitpolicy`, `core.settlement`, `core.payout`) must not depend on the internal rule engine format.

## Why

Client requirements already include rules beyond simple game activation:

```text
- Maryaj gratuit when cart paid total exceeds a threshold.
- Christmas morning first prize pays 60x instead of 50x.
- Multipliers differ per tenant.
- Discounts or fixed bonuses may be introduced later.
```

These rules affect sale preview, sale confirmation, limit exposure, settlement, payout, commissions, receipts and audit. They cannot live only in `catalog.game`.

## Scope

- Add `core.promotion` API and internal V1 engine.
- Add DB model for tenant-scoped promotion rules and applied snapshots.
- Integrate sales preview/confirmation with promotion evaluation.
- Integrate settlement with applied snapshots.
- Provide admin CRUD/read APIs for tenant admins.
- Keep offline promotions disabled by default.

## Non-goals

- No Drools/DMN in V1.
- No visual rule builder in V1.
- No unrestricted DSL in V1.
- No advanced offline promotions in V1.
