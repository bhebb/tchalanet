# Runtime notes — Promotion V1

## Why no full rule engine now

Promotion V1 must remain predictable and fast. The runtime accepts only three effects and a small set of eligibility conditions.

A future rules engine may replace or extend the evaluator, but V1 must not hide configuration in untyped JSON blobs.

## Runtime cache

Runtime cache is the only promotion cache that matters for hot path Sales.

```text
PromotionRuntimeCache[tenantId] -> active campaigns parsed
```

The cache must be invalidated after activate, pause, deactivate, archive, update campaign, add/update/delete rule, update rule eligibility, and update rule effects.

## Resolve vs Apply

Promotion resolves. Sales applies.

```text
ResolvePromotionDecision
  input: sale preparation context
  output: promotion decision with effects

SalePromotionEffectApplier
  input: prepared sale draft + promotion decision
  output: updated lines/money
```

Promotion must not create TicketLine directly.
