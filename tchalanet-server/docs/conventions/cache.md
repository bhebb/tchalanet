# Cache — Policy & Usage

> **Status**: NORMATIVE  
> **Scope**: tchalanet-server (common / core / catalog / features)  
> **Audience**: Backend developers, reviewers, ops  
> **Last reviewed**: 2026-07-07  
> **Related**:
>
> - `../../tchalanet-common/src/main/java/com/tchalanet/server/common/cache/CACHE.md` (architecture L1/L2)
> - `cache-gap-analysis.md` (audit des écarts & proposition de TTL par entité)
> - Ops cache : `tchalanet-features/.../ops/cache/CacheOpsController` (endpoints audités, SUPER_ADMIN)

---

## 1. Purpose

This document defines the **official cache usage rules** for Tchalanet.

It fixes:

- what **must** be done,
- what **must not** be done,
- and the **approved patterns**.

This document is **normative**.  
Implementation details belong to `cache-architecture.md`.

---

## 2. Architectural contract (reminder)

- Two-level cache:
  - **L1**: Caffeine (local, per instance)
  - **L2**: Redis (shared, business TTL)
- All cache operations apply to **L1 + L2**.
- Redis is optional; the application must work with L1 only.

---

## 3. Fundamental rule

### MUST

- Use cache only as an **infrastructure optimization**.
- Assume any cache entry can disappear at any time.

### MUST NOT

- Treat cache as a source of truth.
- Encode business rules that depend on cache presence.

---

## 4. Preferred usage: `@Cacheable`

### MUST

- Use `@Cacheable` whenever possible.
- Apply it only on **read-only / idempotent** operations.
- Declare a **stable functional `cacheName`**.
- Define the key using SpEL.

### MUST NOT

- Access `CacheManager` if `@Cacheable` is sufficient.
- Mix cache logic with business logic in the same method.

---

## 5. Manual cache (controlled exception)

Manual cache access is allowed **only when `@Cacheable` is not suitable**.

### Allowed cases

- Complex composite keys
- Stampede protection
- Raw external payloads (HTTP / JSON)
- Infra, batch or external provider clients

### MUST

- Encapsulate manual cache access in a dedicated helper (`XxxCache`)
- Use `CacheManager`
- Build keys via `CacheKeyBuilder`
- Tolerate cache absence

### MUST NOT

- Scatter `cache.get/put/evict` in domain or application code
- Manually craft Redis keys
- Override TTL decisions in code

---

## 6. Cache name conventions

### Rule

Cache names are **functional**, never technical.

### Format

- `<scope>:<domain>:<resource>[:<qualifier>]`
- Separator is a **colon** (`:`), matching the codebase convention.

### Examples

- `catalog:plan:active_plans`
- `catalog:drawchannel:calendar_rows`
- `core:pricing:tenant_odds_list`
- `platform.tenant.cache.REGISTRY_BY_CODE` *(legacy dot-named — do not replicate)*

> **Legacy note**: some older caches use dots (`core.drawresult.by_id`,
> `platform.tenant.cache.*`). New caches MUST use the colon format. Do not rename
> existing Redis-backed cache names without a migration (keys are persisted in L2).

### MUST NOT

- Use Redis key format as cache name
- Embed environment or tenant into `cacheName` (tenant belongs in the **key**, not the name)

---

## 7. Cache name vs cache key

### Cache name

- Stable
- Declared in annotations
- Declared in a `CacheSpecProvider`

### Cache key

- Runtime-computed
- May depend on tenant, date, hash, parameters
- Defined via:
  - SpEL in `@Cacheable`, or
  - `CacheKeyBuilder` for manual cache

### MUST NOT

- Use `CacheKeyBuilder` inside annotations
- Leak Redis key format to business code

---

## 8. TTL & `CacheSpecProvider`

### MUST

- Declare each cache in a `CacheSpecProvider`
- Provide:
  - `cacheName`
  - `ttlL1`
  - `ttlL2`
- Enforce: `ttlL1 ≤ ttlL2`

### MUST NOT

- Hardcode TTLs in business code
- Create a cache without a declared TTL

### Default TTL (fallback)

A cache used via `@Cacheable` **without** a `CacheSpecProvider` silently falls back to
the generic defaults **L1 = 5 min** (`CacheRuntimeConfig`) / **L2 = 60 min**
(`RedisCacheRuntimeConfig`). This is a code smell, not a feature: a rarely-mutated
referential then expires as fast as volatile session data. Every cache MUST declare a
`CacheSpecProvider`. Note: `CacheSpec.DEFAULT_TTL_L1` (10 min) differs from the runtime
default (5 min) — do not rely on either; declare explicitly.

---

## 9. Eviction rules

### MUST

- Evict **only after transaction commit**
- Use one of:
  - `@CacheEvict(beforeInvocation = false)`
  - `AfterCommit.run(...)`
  - an infra evictor called after commit

### MUST NOT

- Evict before commit
- Evict from domain models
- Evict directly from controllers

---

## 10. Spring Data REST (special case)

When write operations are not controlled:

### MUST

- Use persistence events (`AfterCreate`, `AfterSave`, `AfterDelete`)
- Perform eviction in infra layer only
- Keep eviction minimal and targeted

---

## 11. Ops cache administration

### MUST

- Provide Ops endpoints to:
  - list caches
  - clear one cache
  - clear all caches
- Restrict to `SUPER_ADMIN`
- Audit all Ops actions

### MUST NOT

- Depend on Ops for functional consistency
- Expose Ops cache endpoints to tenant scope

---

## 12. Summary

- `@Cacheable` is the default
- Manual cache is an exception
- TTLs are domain-declared
- Eviction is **after commit only**
- Two cache levels are transparent to business logic
- Ops is a last-resort, audited tool

---

**This policy is final and mandatory.**
