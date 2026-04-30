# common-cache Specification Delta

## MODIFIED Requirements

### Requirement: Cache manager SHALL compose L1 and optional L2 safely

The primary cache manager SHALL use Caffeine as local L1 and Redis as optional remote L2.

#### Scenario: Redis cache is enabled

- **GIVEN** Redis caching is enabled by configuration
- **WHEN** the primary cache manager is created
- **THEN** it SHALL compose Caffeine L1 with Redis L2
- **AND** the Redis cache manager SHALL be injected explicitly by name or qualifier
- **AND** the Caffeine cache manager SHALL NOT accidentally be injected as the Redis manager

#### Scenario: Redis cache is disabled

- **GIVEN** Redis caching is disabled by configuration
- **WHEN** the primary cache manager is created
- **THEN** it SHALL use Caffeine only
- **AND** no Redis connection SHALL be required

#### Scenario: Redis fails after startup

- **GIVEN** Redis caching was enabled at startup
- **AND** Redis becomes unavailable at runtime
- **WHEN** cache `get`, `put`, `evict`, or `clear` is called
- **THEN** remote/L2 failure SHALL NOT fail the application path
- **AND** L1 SHALL continue to function
- **AND** the failure SHALL be logged

---

### Requirement: Combined cache SHALL follow Spring Cache semantics

`CombinedCache` SHALL preserve expected Spring Cache behavior.

#### Scenario: Cache hit in L1

- **GIVEN** a value exists in L1
- **WHEN** `get(key)` is called
- **THEN** the value SHALL be returned
- **AND** L2 SHALL not be required

#### Scenario: Cache hit in L2

- **GIVEN** a value does not exist in L1
- **AND** a value exists in L2
- **WHEN** `get(key)` is called
- **THEN** the value SHALL be returned
- **AND** L1 SHALL be hydrated with the value

#### Scenario: Cache miss loads value

- **GIVEN** a value does not exist in L1 or L2
- **WHEN** `get(key, Callable)` loads a non-null value
- **THEN** the value SHALL be stored in cache
- **AND** the value SHALL be returned

#### Scenario: Loader returns null

- **GIVEN** a value does not exist in cache
- **WHEN** `get(key, Callable)` returns null
- **THEN** null SHALL NOT be cached by default
- **AND** negative caching SHALL require explicit per-cache decision and short TTL

#### Scenario: putIfAbsent inserts value

- **GIVEN** no value exists for the key
- **WHEN** `putIfAbsent(key, value)` is called
- **THEN** the value SHALL be stored
- **AND** the method SHALL return null

#### Scenario: putIfAbsent finds existing value

- **GIVEN** a value already exists for the key
- **WHEN** `putIfAbsent(key, value)` is called
- **THEN** the existing `ValueWrapper` SHALL be returned
- **AND** the existing value SHALL not be overwritten

---

### Requirement: Cache specs SHALL declare cache TTLs consistently

Caches SHALL be declared using `CacheSpecProvider` where possible.

#### Scenario: Cache spec is declared

- **WHEN** a cache spec uses `CacheSpec.of(name, ttlL1, ttlL2)`
- **THEN** the first duration SHALL be the L1 TTL
- **AND** the second duration SHALL be the L2 TTL
- **AND** `ttlL1` SHALL be less than or equal to `ttlL2`

#### Scenario: TTL arguments are reversed

- **GIVEN** a declaration comments the first TTL as L2 or second TTL as L1
- **WHEN** cache declarations are audited
- **THEN** the declaration SHALL be corrected
- **AND** tests or review SHALL verify the effective TTLs

#### Scenario: Cache has no specific spec

- **WHEN** a cache is used without a specific declaration
- **THEN** infrastructure defaults MAY apply
- **AND** production hot-path caches SHOULD be explicitly declared

---

### Requirement: Cache names SHALL follow bounded-context ownership

Cache names SHALL identify the owner bounded context or infrastructure area.

#### Scenario: Core draw declares caches

- **WHEN** core draw declares lifecycle/read-model caches
- **THEN** names SHALL start with `core.draw.`
- **AND** names SHALL NOT start with `catalog.draw.` unless the cache truly belongs to a catalog draw capability

#### Scenario: US Lottery declares provider caches

- **WHEN** US Lottery declares provider raw caches
- **THEN** names SHALL start with `infra.uslottery.`
- **AND** the declaration SHALL live under `core.uslottery.infra.cache`
- **AND** `core.draw` SHALL NOT declare US Lottery provider caches

#### Scenario: Catalog declares caches

- **WHEN** a catalog domain declares caches
- **THEN** names SHALL start with `catalog.<domain>.`

#### Scenario: Feature declares caches

- **WHEN** a feature/BFF declares caches
- **THEN** names SHALL start with `feature.<feature>.`

#### Scenario: Common declares caches

- **WHEN** common declares caches
- **THEN** they SHALL be generic runtime/infrastructure caches only
- **AND** common SHALL NOT own métier/domain caches

---

### Requirement: Cache keys SHALL be stable, tenant-safe and collision-free

Cache key builders SHALL produce deterministic keys that do not collide across resources, tenants or environments.

#### Scenario: TenantId is used in a key

- **WHEN** a key is built with `TenantId`
- **THEN** the key SHALL use `tenantId.value()`
- **AND** it SHALL NOT rely on `TenantId.toString()`

#### Scenario: UUID tenant id is used in a key

- **WHEN** a key is built with raw UUID tenant id
- **THEN** the key SHALL format the UUID consistently
- **AND** it SHALL include environment and tenant namespace

#### Scenario: App settings key is built

- **WHEN** an app settings key is built for tenant/outlet scope
- **THEN** it SHALL use a namespace distinct from outlet entity cache
- **AND** it SHALL NOT collide with `tenantOutletKey`

#### Scenario: Manual domain key builders exist

- **WHEN** a domain-specific key builder duplicates common key builder behavior
- **THEN** it SHOULD be removed or consolidated
- **AND** common key builder helpers SHOULD be preferred unless a strong domain-specific reason exists

---

### Requirement: Cache eviction SHALL happen after commit for writes

Caches backed by mutable state SHALL be invalidated only after the write transaction commits.

#### Scenario: Command changes cached data

- **WHEN** a command handler mutates data used by a cache
- **THEN** cache eviction SHALL happen after commit
- **AND** the eviction SHALL target the narrowest known key when practical

#### Scenario: Broad eviction remains

- **WHEN** `evictAll()` remains for MVP simplicity
- **THEN** it SHALL be documented as MVP-only
- **AND** a follow-up task SHALL exist to replace it with targeted eviction

#### Scenario: Write rolls back

- **WHEN** a command transaction rolls back
- **THEN** cache eviction for the failed state change SHALL NOT run as a committed side effect
