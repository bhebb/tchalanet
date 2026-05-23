# Change: plan-subscription-entitlement

## Summary

Refactor the existing plan/subscription flow and introduce `platform.entitlement` as the runtime capability resolver.

The implementation builds on the existing modules:

- `catalog.plan` remains the plan definition catalog.
- `core.subscription` remains the tenant subscription lifecycle domain.
- `platform.entitlement` is added to resolve cached tenant capability snapshots and provide lightweight feature/quota gates.

## Why

The current direction risks spreading plan/feature checks across business code and causing repeated calls before real business operations.

This change creates one runtime snapshot per tenant:

```text
active subscription + plan features/limits = TenantCapabilitySnapshot
```

Business modules consume this only at strategic boundaries:

- UI/page payload availability;
- optional module HTTP gates;
- critical quota/feature enforcement paths.

## Non-goals for V1

- No visual theme builder.
- No custom role builder.
- No visual rule editor or generic rule engine.
- No public ENTERPRISE plan.
- No billing automation.
- No plan inheritance engine in DB.
- No wildcard demo bypass.

## Affected modules

- `catalog.plan`
- `core.subscription`
- `platform.entitlement` new module
- selected integration points: terminal, outlet, tenantuser, offlinesync, sales, promotion, payout, admin BFF/bootstrap.

## V1 plan set

```text
STARTER
STANDARD
PRO
DEMO
```

## Main decisions

1. Plans are cumulative by data for V1. Each plan stores its effective `featuresJson` and `limitsJson`.
2. `core.subscription` validates plan existence via `PlanCatalog` but does not parse plan feature JSON for runtime gating.
3. `platform.entitlement` resolves and caches `TenantCapabilitySnapshot`.
4. Controllers must not bind command objects directly from request bodies for subscription operations.
5. Tenant-scoped subscription operations derive tenant from `TchRequestContext`.
6. DEMO is a normal plan with bounded quotas and demo flags, not a bypass.
