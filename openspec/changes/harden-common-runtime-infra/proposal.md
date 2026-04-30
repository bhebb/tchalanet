# Change: Harden common runtime infrastructure

## Status

Proposed

## Owner

Backend / Platform runtime

## Why

The current batch, cache, and event infrastructure is broadly aligned with the intended Tchalanet architecture, but recent audits found several production-risk inconsistencies.

### Batch

The batch foundation is good:

- jobs are launched through `BatchJobStarter`;
- jobs are allowlisted through `JobRegistry`;
- job scope is declared as `TENANT` or `GLOBAL`;
- job parameters are validated;
- Spring Batch 6 `JobOperator.start(Job, JobParameters)` is used;
- tenant-scoped jobs bind `TchRequestContext` through `BatchTchContextBinder`;
- `BatchGate` can enable or disable jobs by key.

However, the implementation still has several issues:

- `common.batch.context.BatchTchContextBinder` directly depends on `catalog.tenant.api.TenantCatalog`.
- `common.batch.gate.BatchGateCacheImpl` directly depends on `catalog.settings.internal.persistence.SettingRepository`.
- This violates the rule that `common/` must remain technical and métier-independent.
- `RESULTS_EXTERNAL_FETCH` is currently registered as tenant-scoped even though it writes global `draw_result` data by `result_slot`.
- `RESULTS_EXTERNAL_REFRESH` is ambiguous and may lead to global result fetch being repeated once per tenant.
- Platform/global batch context currently binds `ApiScope.PLATFORM`, but the RLS super-admin/system flag semantics are unclear.
- Batch gate cache does not expose explicit eviction methods.
- Invalid boolean settings are silently ignored.

### Cache

The cache foundation is good:

- L1 Caffeine + optional L2 Redis;
- `CacheSpecProvider` declares cache names and TTLs;
- `CacheSpec` enforces `ttlL1 <= ttlL2`;
- manual caches are possible where `@Cacheable` is not enough;
- Redis can be disabled at startup.

However:

- `CacheConfig.cacheManager(...)` injects `@Nullable CacheManager redisCacheManager` ambiguously. Since `CaffeineCacheManager` is also a `CacheManager`, Spring may inject the wrong manager or fail with ambiguity.
- `CombinedCache` does not treat Redis failures as best-effort at runtime.
- If Redis is enabled at startup but unavailable later, cache `get`, `put`, `evict`, or `clear` may break application paths.
- `CombinedCache.putIfAbsent` does not follow the expected Spring Cache contract.
- `get(key, Callable)` may cache `null` values by default.
- Some cache key builders rely on `TenantId.toString()` instead of `tenantId.value()`.
- `CacheKeyBuilder.appSettingsKey(...)` collides with outlet cache key format.
- Some cache declarations are owned by the wrong bounded context.
- Some `CacheSpec.of(name, ttlL1, ttlL2)` calls have reversed comments/arguments.
- Some cache names are misleading, for example `catalog.draw.*` for core draw lifecycle data.
- US Lottery raw provider cache is declared under `core.draw`, even though it belongs to `core.uslottery`.

### Events

The event primitives are good:

- `DomainEvent`;
- `DomainEventPublisher`;
- `SpringDomainEventPublisher`;
- dev/stg logging listener.

However:

- Some listeners are too generic and mix unrelated concerns.
- Some listeners consume events from another bounded context but inject infra/cache evictors from that producer context.
- `DrawDomainEventListener` in `core.draw` consumes `DrawResultIngestedEvent` and injects `DrawResultCacheEvictor`, crossing infra boundaries.
- Some listeners use `evictAll()` broadly.
- Some consumers use `alreadyProcessed(...)` followed by `markProcessed(...)`, which is race-prone in multi-instance deployments.
- `DrawResultIngestedEvent` and `DrawResultAppliedEvent` semantics need to be standardized.
- Event producers must be audited to ensure events are published only after state changes are persisted, preferably after commit for cross-domain side effects.

## What changes

This change hardens the common runtime infrastructure around batch, cache and events.

### Batch changes

- Introduce `BatchTenantBootstrapProvider` in `common.batch.context`.
- Introduce `BatchGateFlagStore` in `common.batch.gate`.
- Move catalog-backed implementations of those ports outside `common`.
- Remove direct `common -> catalog/internal persistence` dependencies.
- Correct results job scopes:
  - `RESULTS_EXTERNAL_FETCH` must be `GLOBAL`.
  - `RESULTS_EXTERNAL_APPLY` must remain `TENANT`.
  - `RESULTS_EXTERNAL_REFRESH` must be explicitly clarified.
- Clarify platform/global batch context for RLS:
  - `ApiScope.PLATFORM`;
  - `SYSTEM` role/actor;
  - explicit platform/super-admin-equivalent flag if required by RLS.
- Add explicit cache invalidation methods to `BatchGateCache`:
  - `evictTenant`;
  - `evictGlobal`;
  - `evictAll`.
- Ensure settings changes evict/update batch gate cache after commit.
- Log invalid batch gate boolean settings.

### Cache changes

- Fix `CacheConfig.cacheManager(...)` to inject Redis cache manager explicitly by qualifier or object provider.
- Make Redis/L2 best-effort at runtime inside `CombinedCache`.
- Fix `CombinedCache.putIfAbsent`.
- Avoid caching `null` by default.
- Fix `CacheKeyBuilder.appSettingsKey(...)`.
- Normalize all tenant keys to use `tenantId.value()`.
- Move fallback TTL/max-size defaults to properties where practical.
- Review all declared caches:
  - cache ownership;
  - cache names;
  - TTL order;
  - manual cache names;
  - `@Cacheable`;
  - `@CacheEvict`;
  - eviction strategy.
- Move US Lottery raw provider cache declaration to `core.uslottery`.
- Replace misleading cache names such as `catalog.draw.*` for core draw lifecycle data.

### Event changes

- Audit all event producers and event handlers/listeners.
- Enforce producer/consumer ownership:
  - event class lives in producer bounded context;
  - listener lives in consumer bounded context.
- Prevent listeners from injecting infra/internal components of another bounded context.
- Ensure cross-domain side effects run after commit.
- Split generic listeners by concern:
  - cache invalidation;
  - notifications;
  - settlement;
  - stats/projections;
  - audit.
- Add or standardize atomic event consumer idempotency:
  - `markProcessedIfAbsent(...)`;
  - or unique constraint equivalent.
- Replace broad `evictAll()` with targeted eviction where possible.
- Document any remaining `evictAll()` as MVP-only debt.
- Standardize draw result event semantics:
  - `DrawResultIngestedEvent` = global drawresult/provider ingestion;
  - `DrawResultAppliedEvent` = result attached to tenant draw.

## Non-goals

- Do not redesign the entire draw/drawresult/uslottery pipeline in this change.
- Do not add an external message broker.
- Do not introduce distributed Redis locks.
- Do not change sales, payout, pricing or public ticket verification business rules.
- Do not refactor all domain events into an outbox pattern yet.
- Do not introduce a new cache framework.
- Do not change the public API response envelope.

## Expected impact

- Cleaner dependency boundaries.
- Safer batch execution with RLS.
- Fewer accidental cross-tenant/global writes.
- Cache remains functional if Redis fails at runtime.
- Better cache key safety.
- Easier cache ownership and eviction audits.
- More reliable event handlers in multi-instance deployments.
- Less coupling between draw, drawresult, uslottery and feature listeners.

## Risks

- Refactoring `common.batch` ports may require moving existing beans and updating imports.
- Changing batch job scopes may require adjusting schedulers and Ops endpoints.
- Tightening event idempotency may require migration or new unique constraints.
- Renaming cache names may invalidate old Redis entries.
- Some `@Cacheable` declarations may need test coverage if names change.

## Rollout

1. Add new ports and adapters without removing old code where needed.
2. Add tests around batch gate and cache manager composition.
3. Rename cache constants and keep compatibility only if necessary.
4. Update event listeners one bounded context at a time.
5. Run `openspec validate`.
6. Run backend tests.
7. Validate local with Redis disabled and enabled.
8. Validate stg with Redis temporarily unavailable.
