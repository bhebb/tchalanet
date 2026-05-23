# Design Notes

## Runtime flow

```text
GET /tenant/me/capabilities
  -> TchRequestContext.tenantId
  -> EntitlementApi.getSnapshot(tenantId)
  -> cache lookup
  -> ResolveTenantSubscriptionQuery
  -> PlanCatalog.findByCode(planCode)
  -> parse features/limits
  -> TenantCapabilitySnapshot
```

## Enforcement flow

```text
UI/page payload
  uses snapshot to hide/disable actions

HTTP optional module
  uses @RequiredFeature when simple

Critical business action
  handler/application service calls EntitlementApi only when needed
```

## No repeated business prelude

Do not add entitlement checks to every handler. Add them only to feature boundaries and critical actions.

## Tenant safety

Tenant-scoped HTTP controllers must not accept tenant id as request body truth. Use `@CurrentContext`.

Platform routes may use `{tenantId}` path variables with SUPER_ADMIN authorization and audit.

## Demo

DEMO is a plan code and plan data. It is not a universal `return true` bypass.

## Cache invalidation

Subscription lifecycle events evict one tenant snapshot. Plan admin changes can evict all snapshots in V1.
