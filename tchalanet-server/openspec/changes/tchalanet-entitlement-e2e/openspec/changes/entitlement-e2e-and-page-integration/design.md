# Design — Entitlement E2E & Integration

## 1. Runtime model

```text
catalog.plan
  features_json
  limits_json

core.subscription
  tenant current plan + status

platform.entitlement
  TenantPlanSnapshotProvider
  PlanCatalog
  TenantCapabilitySnapshot
  cache platform.entitlement.tenant_snapshot
```

## 2. Testing order

Run tests in this order:

```text
1. Plan seed / plan catalog read
2. Tenant onboarding creates subscription
3. Tenant capabilities snapshot
4. Plan change invalidates capabilities cache
5. Quota providers: users, outlets, terminals
6. Feature gates: offline, promotion, payout approval
7. Suspended/canceled subscription behavior
8. Multitenant isolation
9. Page generation uses capabilities
10. Dashboard usage counts
```

## 3. E2E testing style

Python E2E tests should be black-box HTTP tests where possible.

They should not reach into DB except for controlled setup/cleanup helpers already used in the existing E2E framework.

## 4. Cache invalidation

Subscription lifecycle changes must evict tenant snapshot after commit:

```text
apply plan
change plan
suspend
resume
cancel
renew
```

Plan definition changes should evict all entitlement snapshots through a platform admin orchestration path or a platform ops endpoint.

## 5. Page generation

After entitlement E2E passes:

- Public pages can show active plans.
- Public pages can show a small curated list of features per plan.
- Tenant/admin pages can use capabilities to hide/disable unavailable actions.
- POS/mobile seller pages should hide unavailable features.

## 6. Dashboard counts

Admin dashboards should include usage counts for plan limits:

```text
users.active / limits.users.max
outlets.active / limits.outlets.max
terminals.active / limits.terminals.max
mobile_devices.active / limits.mobile_devices.max
promotion_rules.active / limits.promotion_rules.max
```

These counts are informational in dashboards. Enforcement remains in entitlement gates and handlers.

## 7. Review rule

Before developing a new API or handler, ask:

```text
Does this action depend on a paid capability or quota?
```

If yes:

- add `@RequiredFeature` or `@RequiredQuota` at HTTP boundary when appropriate;
- add handler/application-level validation for critical actions or non-HTTP entrypoints;
- add E2E coverage for denied and allowed cases.
