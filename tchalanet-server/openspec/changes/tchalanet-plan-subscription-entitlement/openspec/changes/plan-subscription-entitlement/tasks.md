# Tasks

## 1. catalog.plan fixes

- [ ] Fix `PlanMapper` JSON parsing so string JSON becomes object `JsonNode`.
- [ ] Add validation for `featuresJson` and `limitsJson` as JSON objects.
- [ ] Seed V1 plans: STARTER, STANDARD, PRO, DEMO.
- [ ] Ensure plan JSON is cumulative for V1.
- [ ] Keep `PlanCatalog` read-only and internal packages hidden.
- [ ] Keep admin writes platform/SUPER_ADMIN scoped.
- [ ] Ensure cache eviction after create/update/deactivate/soft-delete.

## 2. core.subscription fixes

- [ ] Replace request-body command binding with request DTOs.
- [ ] Derive tenant id from `@CurrentContext` for tenant/admin scoped endpoints.
- [ ] Move cross-tenant resolve/apply/change operations under platform routes.
- [ ] Remove `Instant.now()` from `Subscription` domain model.
- [ ] Inject `Clock` in handlers and pass `now` to domain methods.
- [ ] Validate target plans with `PlanCatalog.findByCode` before apply/change.
- [ ] Publish subscription lifecycle events after commit.
- [ ] Add tests for inactive/missing plan validation.

## 3. platform.entitlement creation

- [ ] Create `platform.entitlement.api.EntitlementApi`.
- [ ] Create `TenantCapabilitySnapshot`, `EntitlementDecision`, optional `FeatureKey`/`LimitKey` constants.
- [ ] Implement snapshot resolution from `ResolveTenantSubscriptionQuery` + `PlanCatalog.findByCode`.
- [ ] Parse plan `featuresJson` into `Map<String, Boolean>`.
- [ ] Parse plan `limitsJson` into `Map<String, Integer>`.
- [ ] Implement `getSnapshot`, `checkFeature`, `requireFeature`, `limitValue`, `requireLimitAtMost`.
- [ ] Add `platform.entitlement.tenant_snapshot` cache with declared TTL.
- [ ] Add listener for subscription events to evict snapshots.
- [ ] Add `/tenant/me/capabilities` endpoint.
- [ ] Add `@RequiredFeature` annotation/aspect or document it as deferred if not in first PR.

## 4. Initial integration

- [ ] Add terminal create quota check.
- [ ] Add outlet create quota check.
- [ ] Add user create/invite quota check.
- [ ] Add mobile device bind quota check.
- [ ] Add offline grant/sync/sell feature checks.
- [ ] Add promotion basic create feature/quota checks.
- [ ] Add payout approval workflow feature check.
- [ ] Add admin/page bootstrap capability payload.

## 5. Tests

- [ ] Unit tests for plan JSON parsing.
- [ ] Unit tests for subscription domain lifecycle time behavior.
- [ ] Handler tests for apply/change validating plans.
- [ ] Entitlement snapshot tests for STARTER/STANDARD/PRO/DEMO.
- [ ] Entitlement fallback tests for inactive subscription.
- [ ] Integration test for subscription change evicting/rebuilding entitlement snapshot.
