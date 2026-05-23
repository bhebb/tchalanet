# PLATFORM_ENTITLEMENT — Runtime Capabilities V1

## Status

New normative design document for `platform.entitlement`.

## Decision

Create `platform.entitlement` as a small transversal application service that resolves tenant capabilities from existing `core.subscription` and `catalog.plan`.

It is intentionally simple for V1.

```text
No rule engine.
No plan inheritance runtime.
No billing integration.
No if(plan == PRO) in business code.
```

## Purpose

`platform.entitlement` answers one runtime question:

```text
What can this tenant do right now?
```

It returns a cached `TenantCapabilitySnapshot` containing:

- tenant id;
- plan code;
- subscription status;
- feature booleans;
- numeric limits;
- optional notices/reasons.

## Module shape

```text
platform/entitlement/
  api/
    EntitlementApi.java
    annotation/RequiredFeature.java
    annotation/RequiredQuota.java
    model/FeatureKey.java
    model/LimitKey.java
    model/TenantCapabilitySnapshot.java
    model/EntitlementDecision.java
  internal/
    service/TenantCapabilityResolver.java
    service/EntitlementService.java
    web/TenantCapabilitiesController.java
    event/SubscriptionEntitlementEventListener.java
    cache/EntitlementCacheSpecs.java
    config/EntitlementProperties.java
```

## Public API

```java
public interface EntitlementApi {

  TenantCapabilitySnapshot getSnapshot(TenantId tenantId);

  EntitlementDecision checkFeature(TenantId tenantId, String featureKey);

  void requireFeature(TenantId tenantId, String featureKey);

  int limitValue(TenantId tenantId, String limitKey, int defaultValue);

  void requireLimitAtMost(TenantId tenantId, String limitKey, int requestedValue);
}
```

## Snapshot model

```java
public record TenantCapabilitySnapshot(
    TenantId tenantId,
    String planCode,
    String subscriptionStatus,
    boolean subscriptionActive,
    Map<String, Boolean> features,
    Map<String, Integer> limits,
    List<String> notices
) {
  public boolean hasFeature(String key) {
    return features.getOrDefault(key, false);
  }

  public int limit(String key, int defaultValue) {
    return limits.getOrDefault(key, defaultValue);
  }
}
```

## Resolution flow

```text
EntitlementApi.getSnapshot(tenantId)
  -> cache lookup platform.entitlement.tenant_snapshot
  -> QueryBus.ask(ResolveTenantSubscriptionQuery(tenantId))
  -> PlanCatalog.findByCode(subscription.planCode)
  -> parse PlanView.featuresJson
  -> parse PlanView.limitsJson
  -> apply V1 fallback rules for inactive subscription
  -> return TenantCapabilitySnapshot
```

## Dependency rules

Allowed:

```text
platform.entitlement -> catalog.plan.api.PlanCatalog
platform.entitlement -> core.subscription.api.query via QueryBus or api query model
platform.entitlement -> common
```

Not allowed:

```text
platform.entitlement -> catalog.plan.internal
platform.entitlement -> core.subscription.internal
platform.entitlement -> core.sales/internal, core.terminal/internal, etc.
```

## Cache

Cache name:

```text
platform.entitlement.tenant_snapshot
```

Key:

```text
tenantId
```

Rules:

- Cache is an optimization, not source of truth.
- TTL must be declared with `CacheSpecProvider`.
- Subscription events evict tenant snapshot after commit.
- Plan admin changes may evict all snapshots for MVP, then optimize later.

## Annotations

### `@RequiredFeature`

For HTTP/application entry gates that are optional modules.

```java
@RequiredFeature("promotion.rules.basic")
@PostMapping("/admin/promotions")
public ApiResponse<CreatePromotionResponse> create(...) { ... }
```

Rules:

- Use on controllers for coarse feature gates.
- It reads tenant from `TchRequestContext`.
- It delegates to `EntitlementApi.requireFeature`.
- It must not call repositories directly.
- It is not a replacement for critical domain rules.

### `@RequiredQuota`

For simple quota gates when usage can be provided safely.

```java
@RequiredQuota(
  feature = "terminal.licensing",
  limit = "limits.terminals.max",
  usage = "usage.terminals.active"
)
```

V1 may defer generic usage providers. It is acceptable to implement terminal/user/outlet quota checks directly in the relevant handlers first.

## Enforcement strategy

Do not check entitlements everywhere.

Use three layers only:

```text
1. UI/page payloads use snapshot to hide/disable actions.
2. HTTP annotations block optional modules.
3. Handlers check only critical actions or non-HTTP-callable paths.
```

Critical checks for V1:

```text
terminal create -> limits.terminals.max
outlet create -> limits.outlets.max
user invite/create -> limits.users.max
mobile device bind -> limits.mobile_devices.max
offline grant/sell/sync -> offline.sales.basic + offline limits
promotion create -> promotion.rules.basic + limits.promotion_rules.max
payout approval flow -> payout.approval.workflow
email delivery -> notification.email
export Excel -> reporting.export.excel
```

## UI endpoint

Expose one endpoint for V1:

```http
GET /tenant/me/capabilities
```

Response:

```json
{
  "planCode": "PRO",
  "subscriptionStatus": "ACTIVE",
  "features": {
    "offline.sales.basic": true,
    "promotion.rules.basic": true,
    "promotion.rules.advanced": false
  },
  "limits": {
    "limits.terminals.max": 30,
    "limits.mobile_devices.max": 20
  }
}
```

Admin/platform variants can come later.

## Inactive subscription behavior

V1 decision:

- subscription inactive means premium features disabled;
- admin screens can still load to allow plan repair;
- POS/sales/offline should be blocked if subscription inactive;
- capability snapshot includes `subscriptionActive=false` and a notice.

## Demo behavior

DEMO is resolved as a normal plan.

`platform.entitlement` must not implement a universal bypass.

External delivery for demo should be controlled by feature flags or tenant mode, not entitlement bypass.

## Initial integration points

### Admin/PageModel

- `features.tenantadmin` uses `EntitlementApi.getSnapshot` to include action availability.
- POS/mobile bootstrap uses `/tenant/me/capabilities`.

### Core handlers

Add minimal explicit checks in:

```text
core.terminal create terminal
core.outlet create outlet
core.tenantuser invite/create user
core.offlinesync grant/sync
core.sales offline sell acceptance
core.promotion create rule
core.payout approval workflow
```

Do not add checks to simple reads or dashboards.

## Errors

Feature disabled:

```text
403 entitlement.feature_disabled
```

Quota exceeded:

```text
409 entitlement.quota_exceeded
```

Missing plan/subscription:

```text
403 entitlement.subscription_inactive
```

Errors use `ProblemDetail`, never wrapped in `ApiResponse`.

## PR checklist

- [ ] `platform.entitlement.api.EntitlementApi` exists.
- [ ] `TenantCapabilitySnapshot` resolves plan features/limits.
- [ ] `/tenant/me/capabilities` endpoint exists.
- [ ] Snapshot cache with TTL exists.
- [ ] Subscription events evict tenant snapshot.
- [ ] `@RequiredFeature` exists or is explicitly deferred with TODO.
- [ ] Critical integration checks added for terminal/user/outlet/offline/promotion.
- [ ] No business module parses `PlanView.featuresJson` directly.
