# Catalog Plan Specification

## Purpose

This specification defines the `catalog/plan` module, a pure catalog providing stable, read-mostly reference data for billing plans (tiers/offers).

It MUST NOT implement any tenant subscription lifecycle logic.

---

---

## ADDED Requirements

### Requirement: P1 — Read operations

`PlanCatalog` MUST expose the following read operations:

- `List<PlanView> listActive()` — MUST return plans where `deleted_at IS NULL AND active = true`.
- `Optional<PlanView> findById(PlanId id)` — MUST return plan if present and not soft-deleted.
- `Optional<PlanView> findByCode(String code)` — MUST return plan if present and not soft-deleted.  
  It MUST return inactive plans too (with `active=false`), allowing consumers to distinguish inactive vs missing.

All read operations MUST filter `deleted_at IS NULL`.

#### Scenario: listActive filters soft-deleted and inactive

- Given: 4 plans in DB
  - 2 active (active=true, deleted_at=NULL)
  - 1 inactive (active=false, deleted_at=NULL)
  - 1 soft-deleted (deleted_at!=NULL)
- When: `PlanCatalog.listActive()` is called
- Then: exactly the 2 active plans are returned

#### Scenario: findByCode returns inactive plan

- Given: plan exists with `code=pro-v1` and `active=false`
- When: `PlanCatalog.findByCode("pro-v1")`
- Then: returns `Optional.of(plan)` and `plan.active=false`

#### Scenario: findByCode filters soft-deleted plan

- Given: plan exists with `code=old-v0` and `deleted_at!=NULL`
- When: `PlanCatalog.findByCode("old-v0")`
- Then: returns `Optional.empty()`

---

### Requirement: P2 — Admin writes (internal)

The catalog MUST provide an internal admin service for management operations:

- Service: `catalog/plan/internal/write/PlanAdminService`
- Return type: `PlanView` (mapping via MapStruct; controllers MUST NOT map entities)

Operations:

- `create(...)`
- `update(...)`
- `deactivate(PlanId)` → set `active=false`
- `softDelete(PlanId)` → set `deleted_at=now()` AND `active=false`

Hard delete MUST NOT exist.

#### Scenario: deactivate plan

- Given: plan exists with `active=true`
- When: admin calls `deactivate(id)`
- Then: plan has `active=false` and `deleted_at` remains NULL

#### Scenario: soft-delete plan

- Given: plan exists
- When: admin calls `softDelete(id)`
- Then: plan has `deleted_at!=NULL` AND `active=false`
- And: plan is filtered from all reads

---

### Requirement: P3 — Uniqueness constraint

The `code` property MUST be globally unique.

- DB uniqueness constraint SHOULD be enforced.
- On violation, admin service MUST return a readable error (recommended: HTTP 409 Conflict).

#### Scenario: duplicate code rejected

- Given: plan exists with `code=basic-v1`
- When: admin attempts to create another plan with `code=basic-v1`
- Then: operation fails with a readable error and no duplicate is created

---

### Requirement: P4 — Mapping & API boundaries

- Public APIs in `catalog/*/api` MUST return immutable Views.
- Mapping from JPA entities to Views MUST occur exclusively in `internal/mapper`.
- Controllers MUST NOT expose JPA entities.

#### Scenario: no entity leakage

- Given: `PlanJpaEntity` exists in DB
- When: controller returns plan data
- Then: consumer only receives `PlanView`

---

### Requirement: P5 — Cache

Catalog read methods SHOULD be cached. Cache names MUST live in `internal/cache/PlanCacheNames`.

Writes MUST evict relevant caches (e.g. ACTIVE list + BY_CODE lookups).

#### Scenario: cache eviction on write

- Given: `listActive()` is cached
- When: admin updates or deactivates a plan
- Then: caches are evicted and next `listActive()` reflects changes

---

## Non-Functional Requirements

### NF1 — Performance

- `listActive()` SHOULD be O(n) with small dataset or backed by DB query + cache.
- Additional latency due to cache misses should remain acceptable.

### NF2 — Observability

- Admin operations SHOULD log create/update/deactivate/softDelete with structured metadata.

---

## Acceptance Criteria

- [ ] No hard delete exists in `catalog/plan`
- [ ] `listActive()` filters `deleted_at IS NULL AND active=true`
- [ ] `findByCode()` returns inactive plans but filters soft-deleted
- [ ] Mapping is internal-only; controllers never expose entities
- [ ] Cache is evicted on writes
