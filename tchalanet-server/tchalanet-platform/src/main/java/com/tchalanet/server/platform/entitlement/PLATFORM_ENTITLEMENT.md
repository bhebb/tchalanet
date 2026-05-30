
# Platform Capability `platform.entitlement` — Runtime Capabilities

## Status

New normative design document for `platform.entitlement`.

## Rôle

`platform.entitlement` expose les capacités runtime d’un tenant (feature flags, limites, statut plan, etc.)

**Ce module fait** :
- Résout les capacités d’un tenant à partir de core.subscription et catalog.plan
- Expose un snapshot runtime (`TenantCapabilitySnapshot`)
- API Java : `EntitlementApi`, `TenantPlanSnapshotProvider`, `UsageService`

**Ce module ne fait pas** :
- Pas de moteur de règles
- Pas d’héritage de plan runtime
- Pas d’intégration billing
- Pas de if(plan == PRO) dans le métier

## Surface API

- `EntitlementApi` (Java) : snapshot des capacités
- `TenantPlanSnapshotProvider`, `UsageService` (Java)

## Intégration

- Les modules platform/core interrogent ce module pour connaître les droits/features d’un tenant

## Règles et limitations

- Les snapshots sont mis en cache
- Les décisions sont purement techniques, pas métier

## Public API

```java
public interface EntitlementApi {

  TenantCapabilitySnapshot getSnapshot(TenantId tenantId);

  boolean checkFeature(TenantId tenantId, String featureKey);

  void requireFeature(TenantId tenantId, String featureKey);

  OptionalInt limitValue(TenantId tenantId, String limitKey);

  void requireLimitAtMost(TenantId tenantId, String limitKey, int currentUsage); // Renamed requestedValue to currentUsage
}
```

## Snapshot model

```java
public record TenantCapabilitySnapshot(
    TenantId tenantId,
    String planCode,
    boolean subscriptionActive, // Simplified from subscriptionStatus and subscriptionActive
    Map<String, Boolean> features,
    Map<String, Integer> limits,
    Instant resolvedAt // Added resolvedAt, removed notices
) {
  public boolean hasFeature(String key) {
    return features.getOrDefault(key, false);
  }

  public OptionalInt getLimit(String key) {
    return limits.containsKey(key) ? OptionalInt.of(limits.get(key)) : OptionalInt.empty();
  }
}
```

## Resolution flow

```text
EntitlementApi.getSnapshot(tenantId)
  -> cache lookup platform.entitlement.tenant_snapshot
  -> TenantPlanSnapshotProvider.findCurrentPlan(tenantId) // Changed from QueryBus
  -> PlanCatalog.findByCode(tenantPlanSnapshot.planCode)
  -> parse PlanView.featuresJson
  -> parse PlanView.limitsJson
  -> apply V1 fallback rules for inactive subscription
  -> return TenantCapabilitySnapshot
```

## Dependency rules

Allowed:

```text
platform.entitlement -> catalog.plan.api.PlanCatalog
platform.entitlement -> platform.identity.internal.service.TenantMembershipService (for UserUsageService)
platform.entitlement -> core.terminal.internal.application.port.out.TerminalReaderPort (for TerminalUsageService)
platform.entitlement -> core.outlet.internal.application.port.out.OutletReaderPort (for OutletUsageService)
platform.entitlement -> common
```

Not allowed:

```text
platform.entitlement -> catalog.plan.internal
platform.entitlement -> core.subscription.internal
platform.entitlement -> core.sales/internal, core.terminal/internal, etc. (except for specific UsageService implementations)
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
@RequiredFeature(FeatureKeys.PROMOTION_RULES_BASIC) // Using new FeatureKeys constant
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
  limit = LimitKeys.TERMINALS_MAX,
  usage = UsageKeys.TERMINALS_ACTIVE
)
```

Rules:

- Uses `LimitKeys` and `UsageKeys` for clarity.
- `usage` value is resolved by `UsageService.getCurrentUsage`.

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
terminal create -> limits.terminals.max (Implemented with @RequiredQuota)
outlet create -> limits.outlets.max (Implemented with @RequiredQuota)
user invite/create -> limits.users.max (Implemented with @RequiredQuota)
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
  "subscriptionActive": true,
  "features": {
    "offline.sales.basic": true,
    "promotion.rules.basic": true,
    "promotion.rules.advanced": false
  },
  "limits": {
    "limits.terminals.max": 30,
    "limits.mobile_devices.max": 20
  },
  "resolvedAt": "2023-10-27T10:00:00Z"
}
```

Rules:

- Absent limits stay absent in the REST `limits` object.
- Java callers use `OptionalInt` to distinguish absent from `0`.
- Do not use sentinel values such as `-1` for unlimited or missing limits.

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
core.terminal create terminal (Implemented with @RequiredQuota)
core.outlet create outlet (Implemented with @RequiredQuota)
core.tenantuser invite/create user (Implemented with @RequiredQuota)
core.offlinesync grant/sync
core.sales offline sell acceptance
core.promotion create rule
core.payout approval workflow
```

Do not add checks to simple reads or dashboards.

## Errors

Feature disabled:

```text
403 entitlement.feature_required // Changed from feature_disabled for consistency
```

Quota exceeded:

```text
403 entitlement.limit_exceeded // Changed from 409 quota_exceeded for consistency
```

Missing plan/subscription:

```text
403 entitlement.subscription_inactive
```

Errors use `ProblemDetail`, never wrapped in `ApiResponse`.

## PR checklist

- [x] `platform.entitlement.api.EntitlementApi` exists.
- [x] `TenantPlanSnapshotProvider` and related models exist.
- [x] `UsageService` and `UsageKeys`/`LimitKeys` exist.
- [x] `TenantCapabilitySnapshot` resolves plan features/limits.
- [ ] `/tenant/me/capabilities` endpoint exists.
- [x] Snapshot cache with TTL exists (`EntitlementCacheSpecProvider`).
- [x] Subscription events evict tenant snapshot (`EntitlementCacheEvictor`).
- [x] `@RequiredFeature` exists.
- [x] `@RequiredQuota` exists.
- [x] Critical integration checks added for terminal/user/outlet.
- [ ] Critical integration checks added for offline/promotion/payout.
- [x] No business module parses `PlanView.featuresJson` directly.
