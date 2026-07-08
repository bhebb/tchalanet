# cache Spec

## ADDED Requirements

### Requirement: Every cache declares a TTL via CacheSpecProvider

Any cache used through `@Cacheable` SHALL be declared in a `CacheSpecProvider` with explicit
`ttlL1` and `ttlL2` (`ttlL1 ≤ ttlL2`). No cache may rely on the generic runtime defaults.

#### Scenario: Cache without a declared spec

- **GIVEN** a `@Cacheable` method whose cache name has no `CacheSpecProvider` entry
- **WHEN** the application context starts
- **THEN** a startup check fails, listing the undeclared cache name

### Requirement: Cached reads reflect committed writes

A write that changes cached data SHALL evict every cache that exposes that data, after commit.

#### Scenario: Draw channel edit invalidates calendar rows

- **GIVEN** `catalog:drawchannel:calendar_rows` holds a cached calendar for a tenant
- **WHEN** a draw channel or draw-channel-game write for that tenant commits
- **THEN** the `calendar_rows` cache is evicted and the next read returns the new calendar

#### Scenario: Rolled-back write does not corrupt the cache

- **GIVEN** a cached entry exists for an entity
- **WHEN** a write transaction evicts it but then rolls back
- **THEN** the cache is not left populated with stale/uncommitted state

### Requirement: Caches never make money, limit or POS-security decisions

Caches SHALL only hold descriptive data. Consumed limit counters, ticket lists/lines, audit data,
and seller-terminal sale/binding validation SHALL NOT be cached.

#### Scenario: Consumed limit counter is not cached

- **GIVEN** a sale decrements a tenant's remaining sellable amount
- **WHEN** a subsequent sale reads the remaining amount
- **THEN** the value comes from the source of truth, not a cache

#### Scenario: Limit rule definition may be cached

- **GIVEN** a limit policy definition (thresholds) rarely changes
- **WHEN** it is read on the sale path
- **THEN** it may be served from `core:limit:policy_by_scope`, evicted on definition write

### Requirement: Tenant-scoped caches key by tenant

A cache holding per-tenant data SHALL include the tenant id in the cache key, never in the name.

#### Scenario: Two tenants read the same cache name

- **GIVEN** tenants A and B read `platform:tenantgame:runtime`
- **WHEN** both are cached
- **THEN** each key contains its tenant id and neither tenant sees the other's value

### Requirement: L2 values round-trip through the Redis serializer

The Redis (L2) value serializer SHALL correctly serialize and deserialize the value shapes used by
caches: records with typed-id wrappers, root-level collections, and Spring `NullValue`.

#### Scenario: List of typed-id-bearing views

- **GIVEN** a cache value `List<ResultSlotView>` (typed-id `id`, `JsonNode` config fields)
- **WHEN** it is written to and read back from Redis
- **THEN** the deserialized value equals the original element-wise

#### Scenario: Negative caching

- **GIVEN** a `@Cacheable` read returns empty and Spring caches a `NullValue`
- **WHEN** the entry is read back from Redis
- **THEN** deserialization succeeds and yields a `NullValue`

### Requirement: Per-cache runtime kill-switch

Operations SHALL be able to disable an individual cache at runtime without redeploy; a disabled
cache is bypassed on read and write.

#### Scenario: Disabling a problematic cache

- **GIVEN** a cache is producing bad responses
- **WHEN** an operator disables it via the Ops toggle
- **THEN** subsequent reads bypass the cache and hit the source of truth, and the action is audited
