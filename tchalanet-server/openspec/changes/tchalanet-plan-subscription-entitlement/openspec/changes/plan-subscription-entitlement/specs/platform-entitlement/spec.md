# platform.entitlement Requirements

## ADDED Requirements

### Requirement: Entitlement API exposes tenant capability snapshot

`platform.entitlement` SHALL expose `EntitlementApi.getSnapshot(TenantId)`.

#### Scenario: Resolve active PRO tenant

- GIVEN tenant has active subscription plan PRO
- AND catalog plan PRO has features and limits JSON
- WHEN `getSnapshot(tenantId)` is called
- THEN the snapshot includes planCode PRO
- AND features from the plan
- AND limits from the plan

### Requirement: Entitlement API checks features

`platform.entitlement` SHALL expose feature check and require methods.

#### Scenario: Feature enabled

- GIVEN snapshot has `promotion.rules.basic=true`
- WHEN `requireFeature(tenantId, "promotion.rules.basic")` is called
- THEN no exception is thrown

#### Scenario: Feature disabled

- GIVEN snapshot has no `promotion.rules.basic`
- WHEN `requireFeature(tenantId, "promotion.rules.basic")` is called
- THEN `ProblemDetail` 403 with code `entitlement.feature_disabled` is returned

### Requirement: Entitlement API checks limits

`platform.entitlement` SHALL expose numeric limit lookup and simple limit enforcement.

#### Scenario: Requested terminal count within limit

- GIVEN `limits.terminals.max=10`
- WHEN `requireLimitAtMost(tenantId, "limits.terminals.max", 8)` is called
- THEN no exception is thrown

#### Scenario: Requested terminal count exceeds limit

- GIVEN `limits.terminals.max=10`
- WHEN `requireLimitAtMost(tenantId, "limits.terminals.max", 11)` is called
- THEN `ProblemDetail` 409 with code `entitlement.quota_exceeded` is returned

### Requirement: Tenant capabilities endpoint

The backend SHALL expose a tenant-scoped capabilities endpoint.

#### Scenario: Current tenant capabilities

- WHEN authenticated tenant user calls `GET /tenant/me/capabilities`
- THEN the response contains planCode, subscriptionStatus, features, and limits
- AND tenant id is resolved from context

### Requirement: Snapshot cache

Tenant capability snapshots SHALL be cached with declared TTL.

#### Scenario: Cached snapshot reused

- GIVEN snapshot exists in cache
- WHEN `getSnapshot(tenantId)` is called again
- THEN plan/subscription resolution is not repeated until cache eviction/expiry

### Requirement: Subscription events evict snapshots

`platform.entitlement` SHALL evict tenant snapshots when subscription lifecycle events occur.

#### Scenario: Plan change evicts snapshot

- GIVEN tenant snapshot is cached
- WHEN `TenantSubscriptionPlanChangedEvent` is consumed
- THEN the tenant snapshot cache entry is evicted

## MODIFIED Requirements

None.

## REMOVED Requirements

None.
