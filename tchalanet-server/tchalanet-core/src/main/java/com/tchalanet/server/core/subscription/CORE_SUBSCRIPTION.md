# CORE_SUBSCRIPTION — Tenant Subscription V1

## Status

Normative for the `core.subscription` fixes and integration with plan catalog and entitlements.

## Decision

`core.subscription` remains the owner of the tenant subscription lifecycle.
It must not become the runtime entitlement engine.

```text
core.subscription = lifecycle of a tenant subscription
catalog.plan = commercial plan definition
platform.entitlement = runtime capability snapshot
```

## Responsibilities

`core.subscription` owns:

- apply tenant plan;
- change tenant plan;
- cancel subscription;
- suspend subscription;
- resume subscription;
- renew subscription;
- subscription status and validity periods;
- subscription events for downstream cache invalidation.

It does not own:

- parsing plan features in every business path;
- checking whether a sales/offline/promotion action is enabled;
- quota usage counting;
- UI feature availability payloads.

## Dependencies

Allowed:

```text
core.subscription -> catalog.plan.api.PlanCatalog
core.subscription -> common
core.subscription -> platform.audit.api if needed
```

Not allowed:

```text
core.subscription -> catalog.plan.internal
core.subscription -> platform.entitlement.internal
catalog.plan -> core.subscription
```

## Use PlanCatalog for plan validation

Handlers that apply/change plans must validate the target plan through `PlanCatalog`.

Example:

```java
var plan = planCatalog.findByCode(cmd.planCode())
    .orElseThrow(() -> ProblemRest.notFound("plan", cmd.planCode()));

if (!plan.active()) {
  throw ProblemRest.badRequest("subscription.plan_inactive");
}
```

The domain model receives only a validated plan code.

## Controller fixes

The current controller accepts command objects directly from the request body. Replace this with request DTOs.

### Tenant/admin current subscription routes

Recommended tenant/admin routes:

```http
GET  /admin/subscription/current
POST /admin/subscription/change
POST /admin/subscription/cancel
POST /admin/subscription/resume
```

The tenant id comes from `@CurrentContext`, not from request body.

### Platform tenant subscription routes

Recommended platform routes:

```http
GET  /platform/tenants/{tenantId}/subscription
POST /platform/tenants/{tenantId}/subscription/apply
POST /platform/tenants/{tenantId}/subscription/change
POST /platform/tenants/{tenantId}/subscription/suspend
POST /platform/tenants/{tenantId}/subscription/resume
POST /platform/tenants/{tenantId}/subscription/cancel
POST /platform/tenants/{tenantId}/subscription/renew
```

These routes are SUPER_ADMIN/platform scoped and may carry a tenant id path variable.

### Example request DTO style

```java
public record ChangePlanRequest(
    @NotBlank String planCode
) {}
```

```java
@PostMapping("/change")
public ResponseEntity<ApiResponse<ChangePlanResult>> change(
    @CurrentContext TchRequestContext ctx,
    @Valid @RequestBody ChangePlanRequest req
) {
  var cmd = new ChangePlanCommand(
      ctx.effectiveTenantIdRequired(),
      req.planCode()
  );
  var result = commandBus.execute(cmd);
  return ResponseEntity.ok(ApiResponse.success(result));
}
```

## Domain model fixes

The `Subscription` domain model must not call `Instant.now()`.
All time is passed from application handlers through an injected `Clock`.

Bad:

```java
Instant.now()
```

Good:

```java
public Subscription cancelNow(Instant now) {
  if (now == null) throw new IllegalArgumentException("now is required");
  ... updatedAt = now ...
}
```

Apply to:

```text
cancelNow(now)
resume(now)
suspend(now)
changePlan(newPlanCode, now)
renew(newEndsAt, now)
withMetadata(newMetadata, now)
withStatus(newStatus, now)
```

## Events

`core.subscription` should publish events after commit when subscription changes affect entitlement snapshots.

Recommended public events:

```java
TenantSubscriptionAppliedEvent
TenantSubscriptionPlanChangedEvent
TenantSubscriptionSuspendedEvent
TenantSubscriptionResumedEvent
TenantSubscriptionCanceledEvent
TenantSubscriptionRenewedEvent
```

Event metadata:

```text
eventId
occurredAt
tenantId
subscriptionId
planCode or oldPlanCode/newPlanCode
status
actorUserId if available
```

`platform.entitlement` listens to these events to evict tenant capability snapshot cache.

## Query API

Keep or expose a stable query:

```java
ResolveTenantSubscriptionQuery(TenantId tenantId)
```

Result should include:

```java
SubscriptionView(
  SubscriptionId id,
  TenantId tenantId,
  String planCode,
  SubscriptionStatus status,
  Instant startedAt,
  Instant endsAt,
  Instant trialEndsAt,
  Instant canceledAt
)
```

For tenant-scoped HTTP reads, prefer context-based query or controller mapping that supplies tenant id from context.

## Subscription validity rules V1

A subscription is usable for entitlements only when:

```text
status in ACTIVE, TRIAL
and startedAt <= now
and (endsAt is null or endsAt > now)
and canceledAt is null
```

Suspended subscriptions should return no premium features, but the UI can still show plan and reason.

Canceled/expired subscriptions should resolve to a safe fallback:

```text
features = core/base only or none, based on product decision
limits = strict minimum
```

Recommended V1 fallback:

```text
No premium feature. Admin can see subscription screen. POS selling should be blocked if subscription inactive.
```

## Demo subscription

DEMO is represented as a normal plan code with special plan metadata/features.

Do not implement:

```java
if (tenantMode == DEMO) return true;
```

Instead:

```text
subscription.planCode = DEMO
catalog.plan DEMO has demo-eligible features
platform.entitlement resolves the normal snapshot
```

## Integration with entitlement

`core.subscription` should not call `platform.entitlement` during ordinary changes.
It publishes events. `platform.entitlement` evicts/rebuilds snapshots.

Allowed exception:

- tests may call entitlement after subscription change to verify integration.

## Required fixes from current code

- [ ] Replace body command binding with request DTOs.
- [ ] Tenant-scoped endpoints must derive tenant from `@CurrentContext`.
- [ ] Move cross-tenant `resolve/{tenantId}` to platform route or restrict it.
- [ ] Remove `Instant.now()` from domain model.
- [ ] Inject `Clock` in handlers and pass `now` to domain methods.
- [ ] Validate target plan through `PlanCatalog` before apply/change.
- [ ] Publish after-commit subscription events for entitlement cache invalidation.
- [ ] Keep command/query outputs as result/view records, not domain entities.
