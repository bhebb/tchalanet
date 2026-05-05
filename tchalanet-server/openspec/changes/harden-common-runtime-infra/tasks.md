# Tasks

## 1. Batch hardening

### 1.1 Remove common -> catalog dependency

- [x] Create `BatchTenantBootstrapProvider` in `common.batch.context`.
- [x] Create a small DTO/record for batch tenant bootstrap data, for example:
  - [x] tenant id;
  - [x] tenant code;
  - [x] timezone;
  - [x] currency.
- [x] Update `BatchTchContextBinder` to depend on `BatchTenantBootstrapProvider`.
- [x] Move the `TenantCatalog` implementation outside `common`.
- [x] Ensure the adapter implementation lives in an appropriate catalog/infra package.
- [x] Remove imports from `common.batch` to:
  - [x] `catalog.tenant.api.TenantCatalog`;
  - [x] `catalog.tenant.api.model.TenantBootstrapView`.

### 1.2 Remove common -> catalog settings internal persistence dependency

- [x] Create `BatchGateFlagStore` in `common.batch.gate`.
- [x] Define methods:
  - [x] `Optional<Boolean> findTenantFlag(JobKey jobKey, TenantId tenantId)`;
  - [x] `Optional<Boolean> findGlobalFlag(JobKey jobKey)`.
- [x] Update `BatchGateCacheImpl` to depend on `BatchGateFlagStore`.
- [x] Move the settings-backed implementation outside `common`.
- [x] Remove imports from `common.batch` to:
  - [x] `catalog.settings.internal.persistence.SettingRepository`;
  - [x] catalog settings internal persistence classes.

### 1.3 Add batch gate cache eviction

- [x] Add to `BatchGateCache`:
  - [x] `void evictTenant(JobKey jobKey, TenantId tenantId)`;
  - [x] `void evictGlobal(JobKey jobKey)`;
  - [x] `void evictAll()`.
- [x] Implement these methods in the current cache implementation.
- [ ] Ensure flag updates call eviction/update after commit.
- [ ] Add tests for tenant and global eviction.

### 1.4 Improve invalid setting observability

- [x] Update boolean parsing to log invalid values.
- [x] Log at WARN with:
  - [x] namespace;
  - [x] setting key;
  - [x] level;
  - [x] tenant id if any;
  - [x] raw value.
- [x] Keep invalid values non-fatal.
- [ ] Add tests for invalid boolean setting.

### 1.5 Correct result job scopes

- [x] Update `JobRegistry`:
  - [x] `RESULTS_EXTERNAL_FETCH` = `GLOBAL`.
  - [x] `RESULTS_EXTERNAL_APPLY` = `TENANT`.
- [x] Clarify `RESULTS_EXTERNAL_REFRESH`:
  - [ ] choose global orchestration; or
  - [x] choose tenant apply orchestration without repeated global fetch.
- [x] Update required/optional params accordingly.
- [ ] Update `JobParamsValidator` rules if necessary.
- [ ] Update Ops endpoints if they assume tenant id for fetch.
- [ ] Update schedulers if they trigger fetch per tenant.
- [ ] Add tests for job registration and params.

### 1.6 Clarify platform/global batch context

- [x] Review `TchRequestContext` constructor fields.
- [x] Identify the boolean currently passed as `false` in platform scope.
- [ ] Confirm whether it represents `isSuperAdmin`, `isPlatform`, or another flag.
- [ ] Ensure GLOBAL jobs bind:
  - [x] `ApiScope.PLATFORM`;
  - [x] SYSTEM role/actor;
  - [ ] platform/super-admin-equivalent flag required by RLS.
- [ ] Add tests for RLS context values bound by global jobs.
- [x] Ensure MDC is cleared even if binding fails.

### 1.7 Batch tests

- [ ] Test tenant job requires `tenant_id`.
- [ ] Test global job does not require `tenant_id`.
- [ ] Test tenant gate override wins over global flag.
- [ ] Test global flag wins over default.
- [ ] Test default enabled behavior.
- [ ] Test invalid setting is logged and ignored.
- [ ] Test context binder clears MDC/context after job.
- [ ] Test platform context binds RLS-compatible values.

---

## 2. Cache hardening

### 2.1 Fix cache manager wiring

- [x] Update `CacheConfig.cacheManager(...)` to inject Redis cache manager explicitly:
  - [x] `@Qualifier("redisCacheManager")`; or
  - [x] `ObjectProvider<CacheManager>` with qualifier.
- [x] Ensure local `CaffeineCacheManager` is never injected as Redis manager.
- [ ] Add startup test with Redis enabled.
- [ ] Add startup test with Redis disabled.

### 2.2 Make Redis best-effort runtime

- [x] Update `CombinedCache.get(...)`.
- [x] Update `CombinedCache.get(key, type)`.
- [x] Update `CombinedCache.get(key, Callable)`.
- [x] Update `CombinedCache.put(...)`.
- [x] Update `CombinedCache.evict(...)`.
- [x] Update `CombinedCache.clear(...)`.
- [x] Remote/L2 exceptions must not break application paths.
- [x] Log remote failures.
- [x] Keep L1 operational.

### 2.3 Fix Spring Cache semantics

- [x] Update `CombinedCache.putIfAbsent(...)`:
  - [x] return existing wrapper if present;
  - [x] return `null` after successful insert.
- [ ] Add unit test for `putIfAbsent`.

### 2.4 Clarify null caching

- [x] Update `get(key, Callable)` to not cache `null` by default.
- [ ] Document negative caching policy.
- [ ] If negative caching is needed later, require explicit per-cache opt-in and short TTL.
- [ ] Add unit test for null loader behavior.

### 2.5 Fix key builder

- [x] Fix `CacheKeyBuilder.appSettingsKey(...)` collision with outlet key.
- [x] Rename to a distinct namespace, for example:
  - [x] `settings:outlet`;
  - [ ] or `app-settings:outlet`.
- [x] Replace all `TenantId` formatting with `tenantId.value()`.
- [ ] Add helper methods for draw keys if `DrawCacheKeyBuilder` is removed.
- [ ] Remove duplicate domain key builders where possible.
- [ ] Add unit tests for key outputs.

### 2.6 Externalize default cache values

- [ ] Add properties if practical:
  - [ ] `tch.cache.caffeine.default-ttl`;
  - [ ] `tch.cache.caffeine.default-max-size`;
  - [ ] `tch.cache.redis.default-ttl`.
- [ ] Keep explicit `CacheSpecProvider` for production caches.
- [ ] Ensure current hardcoded defaults remain safe fallback.

### 2.7 Audit declared caches

- [x] List all `CacheSpecProvider`.
- [x] List all `cacheManager.getCache("...")`.
- [x] List all `@Cacheable`.
- [x] List all `@CacheEvict`.
- [x] Verify every cache name is declared or intentionally defaulted.
- [x] Verify ownership:
  - [x] `core.draw.*` for core draw lifecycle;
  - [x] `infra.uslottery.*` for provider raw/cache infra;
  - [x] `catalog.<domain>.*` for catalog referentials;
  - [x] `feature.<feature>.*` for feature/BFF caches.
- [x] Fix reversed TTL arguments/comments.
- [x] Remove US Lottery cache declaration from `core.draw`.
- [x] Replace `catalog.draw.*` when it refers to core draw lifecycle data.
- [x] Prefer simple `@Component` `CacheSpecProvider` classes over `@Configuration + @Bean` when no complex config is needed.

### 2.8 Audit eviction

- [x] List all write handlers that mutate cached data.
- [ ] Verify eviction happens after commit.
- [ ] Replace broad `evictAll()` with targeted eviction where possible.
- [x] Document remaining `evictAll()` as MVP debt.
- [ ] Add tests for key eviction where practical.

---

## 3. Event hardening

### 3.1 Inventory

- [x] List all `DomainEvent` implementations.
- [x] List all `DomainEventPublisher.publish(...)` calls.
- [x] List all `@EventListener`.
- [x] List all `@TransactionalEventListener`.
- [x] List all event listener classes by bounded context.

### 3.2 Enforce event ownership

- [x] For each event, identify producer bounded context.
- [ ] Ensure event class lives in producer bounded context.
- [x] For each listener, identify consumer bounded context.
- [ ] Ensure listener lives in consumer bounded context.
- [ ] Remove listeners that import producer infra/internal adapters directly.
- [ ] Replace cross-domain infra dependencies with:
  - [ ] consumer-owned evictor;
  - [ ] consumer-owned port;
  - [ ] command dispatch;
  - [ ] query/catalog API if allowed.

### 3.3 Enforce after-commit side effects

- [x] Ensure cross-domain listeners use `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- [x] Replace synchronous `@EventListener` when side effects depend on committed state.
- [x] Keep synchronous listeners only for local/in-memory/dev logging or explicitly safe internal behavior.
- [x] Review producers to ensure event publication occurs after state mutation is persisted.
- [x] Prefer `AfterCommit.run(...)` for cross-domain event publication where appropriate.

### 3.4 Split generic listeners

- [ ] Split listeners by concern:
  - [ ] cache invalidation listener;
  - [ ] notification listener;
  - [ ] stats/projection listener;
  - [ ] settlement listener;
  - [ ] audit listener.
- [ ] Rename generic listener classes.
- [ ] Keep listeners thin.
- [ ] Move métier logic to command handlers or application services.

### 3.5 Improve idempotency

- [x] Add or standardize atomic method:
  - [x] `markProcessedIfAbsent(consumerKey, eventId)`;
  - [x] or equivalent.
- [x] Back it with unique constraint where possible.
- [x] Replace `alreadyProcessed(...)` + `markProcessed(...)` in cross-domain listeners.
- [ ] Add tests for duplicate event handling.

### 3.6 Draw/drawresult event cleanup hooks

- [x] In common event audit, identify current misuse of `DrawResultIngestedEvent`.
- [x] Defer draw-specific refactor to `align-draw-uslottery-results-pipeline`.
- [x] Ensure common spec documents general rule:
  - [x] ingestion event != applied event;
  - [x] consumer owns listener and side effect.

### 3.7 Event tests

- [ ] Test listener runs after commit.
- [ ] Test listener does not run on rollback.
- [ ] Test duplicate event is ignored.
- [ ] Test listener does not import producer infra.
- [ ] Test event publication happens after successful write where applicable.
