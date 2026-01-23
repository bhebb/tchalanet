# OpenSpec — Catalog Rules (75)

> **Scope**: Backend (`tchalanet-server`)
> **Applies to**: all modules under `com.tchalanet.server.catalog.*`
> **Status**: NORMATIVE
> **Purpose**: structural + technical rules (NOT functional)

---

## 1. What a Catalog IS (definition)

A **Catalog** is a **reference data module**.

It provides:

- stable identifiers
- lookup data
- configuration or registries
- read-mostly datasets

Catalogs are **not business domains**.

They do **not**:

- enforce business invariants
- manage lifecycles
- orchestrate flows
- emit domain events
- own transactional rules

👉 Functional meaning lives in `DOMAIN_*.md`, not here.

---

## 2. What a Catalog is NOT

A catalog is NOT:

- a core domain
- a workflow engine
- a state machine
- a transaction boundary
- an integration orchestrator

If a module:

- reacts to events
- drives lifecycle changes
- creates or mutates business entities

➡️ it belongs in **`core/`**, not in `catalog/`.

---

## 3. Mandatory Read / Write separation (KEY RULE)

Catalogs **MUST separate read and write responsibilities**.

### 3.1 Read side = Catalog API

**Read access** is exposed via a **Catalog API**.

**Location**:

- `catalog/<name>/api`

**Contains**:

- `XCatalog` (interface)
- `XView` (DTO)
- optional `XSearchCriteria`

**Rules**:

- read-only
- side-effect free
- cache-friendly
- consumed by `core/` and `features/`
- MUST NOT depend on `internal/*`

**Example**:

```java
public interface AddressCatalog {
  List<AddressView> listActive();
  Optional<AddressView> findById(AddressId id);
}
```

### 3.2 Read implementation = CatalogImpl

Implementation of the read contract.

**Location**:

- `catalog/<name>/internal/read`

**Rules**:

- implements `XCatalog`
- reads from persistence
- applies filtering (`active`, `deleted_at`)
- performs caching
- maps Entity → View
- NEVER performs writes
- NEVER emits events
- NEVER exposed as a controller

**Naming**:

- `XCatalogImpl` (MANDATORY)

### 3.3 Write side = Admin Service

All writes go through a write-only service.

**Location**:

- `catalog/<name>/internal/write`

**Rules**:

- create / update / delete
- validation
- soft-delete when applicable
- cache eviction
- mapping to View (if returning data)
- NOT used by `core/` or `features/`
- ONLY called by admin controllers

**Naming**:

- `XAdminService` (preferred) or `XWriteService`

---

## 4. Controllers (Admin CRUD only)

Catalogs MUST NOT expose repositories or entities directly.

**Controllers**:

- `catalog/<name>/internal/web`

**Rules**:

- admin only (`SUPER_ADMIN` / `TENANT_ADMIN`)
- thin controllers
- no persistence access
- no mapping logic
- no cache logic
- delegate to write services

**Controllers MUST**:

- call write service
- return `ApiResponse<T>`
- never return JPA entities

---

## 5. Mapping (STRICT)

Mapping is internal-only.

**Location**:

- `catalog/<name>/internal/mapper`

**Rules**:

- Entity → View mapping only
- API MUST NOT contain mapping code
- Controllers MUST NOT map entities
- Services MAY call mappers
- Prefer MapStruct
- Reuse `CommonIdMapper` for ID wrappers

**Required mapper methods**:

- `XView toView(XJpaEntity e);`
- `List<XView> toViews(List<XJpaEntity> entities);`

---

## 6. Persistence

Persistence is internal.

**Location**:

- `catalog/<name>/internal/persistence`

**Rules**:

- JPA entities only
- repositories internal-only
- soft-delete preferred
- no Spring Data REST

🚫 FORBIDDEN:

- `@RepositoryRestResource`
- exposing repositories as HTTP APIs

---

## 7. Cache rules (INTERNAL ONLY)

Cache is an implementation detail.

**Location**:

- `catalog/<name>/internal/cache`

**Rules**:

- cache names defined here
- read side uses `@Cacheable`
- write side performs `@CacheEvict`
- API MUST NOT reference cache

**Recommended cache names**:

- `catalog:<name>:active`
- `catalog:<name>:by_id`
- `catalog:<name>:by_key`

---

## 8. LIST vs PAGE (important)

**Default rule**:

Catalogs MUST expose LIST APIs by default.

Paging is NOT automatic.

**Paging is allowed ONLY if**:

- dataset can grow large
- search criteria exist
- justified in domain documentation

**Examples**:

- Address → PAGE allowed
- ResultSlot, Game, Pricing → LIST only

**Paging requires**:

- `TchPage<T>`
- `TchPageRequest`
- justification in `DOMAIN_<X>.md`

---

## 9. Dependency rules (STRICT)

- `catalog/*/api` MUST NOT depend on `internal/*`
- `core/` MAY read from catalog APIs
- `catalog/` MUST NOT emit domain events
- `catalog/` MUST NOT depend on `core/`
- `features/` MAY orchestrate catalog + core

**Allowed graph**:

```
common
↑
catalog     core
↑       ↑
└── features
```

---

## 10. Documentation split (VERY IMPORTANT)

This file (75):

- structure
- layering
- responsibilities
- technical constraints

`DOMAIN_<X>.md`:

- functional meaning
- business intent
- examples
- data semantics
- lifecycle explanations (if any)

Never mix both.

---

## 11. Enforcement (required)

Add ArchUnit rules to enforce:

- no `catalog.api` → `internal`
- no controller returning JPA entities
- no SDR annotations in `catalog.*`
- controllers cannot access repositories

**Violations require**:

- refactor OR
- explicit ADR

---

## 12. Mental model (TL;DR)

- `XCatalog` = read contract
- `XCatalogImpl` = read provider
- `XAdminService` = write handler
- `Controller` = HTTP boundary
- `Mapper` = Entity → View
- `Catalog` = reference data only

If a class does not clearly fit one of these roles → it is misplaced.