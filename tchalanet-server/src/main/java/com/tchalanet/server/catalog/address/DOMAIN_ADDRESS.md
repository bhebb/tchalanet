# Domaine Address

> Ce fichier est un **template** pour documenter le domaine backend.
> Copie/complète les sections ci-dessous (voir `docs/DOMAIN_TEMPLATE.md`).

---

# Domaine core.address — Adresses (tenant/outlet/user)

> Centralise la gestion d’adresses postales et de contact pour tenants, outlets, et (optionnel) utilisateurs.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/catalog.md` (address reference)

---

## 1. Rôle du domaine — Catalog « Address »

**Scope**: `catalog.address`  
**Type**: Catalog (reference data)  
**Status**: STABLE  
**Layer**: `catalog/` (read-mostly, no business invariants)

---

## 1) Purpose

The **Address catalog** provides a centralized, normalized repository of physical addresses used across the platform.

It serves as **reference data** for:

- tenants
- outlets / points of sale
- reporting and exports
- integrations requiring a canonical address format

Addresses are **not transactional entities** and do not participate in financial workflows.

---

## 2) Responsibilities

### What this catalog DOES

- Store and expose normalized addresses
- Support **read access** for other layers (features, core read-only needs)
- Support **platform-level CRUD** for administrators
- Provide **search and pagination** (address datasets can grow large)
- Support **deduplication** workflows

### What this catalog DOES NOT do

- No financial logic
- No tenant lifecycle logic
- No authorization decisions beyond platform-level access
- No orchestration with other domains
- No domain events emission

---

## 3) Data characteristics

- Dataset size: **potentially large**
- Mutation frequency: **low**
- Read frequency: **moderate**
- Stability: **high**
- Soft-delete supported (`deleted_at`)

> À cause de la taille et des besoins de recherche, **la pagination est explicitement autorisée** pour Address.

---

## 4) Public Read API (`catalog.address.api`)

### `AddressCatalog`

```java
public interface AddressCatalog {

  List<AddressView> listActive();

  Optional<AddressView> findById(AddressId id);

  TchPage<AddressView> search(
      AddressSearchCriteria criteria,
      TchPageRequest pageRequest
  );
}
```

**Rules**

- All methods are read-only
- Side-effect free
- Cacheable
- API does not expose persistence details
- No domain events

---

## 5) DTOs (Views)

**AddressView**

```java
public record AddressView(
  AddressId id,
  String line1,
  String line2,
  String city,
  String postalCode,
  String country,
  String outletCode,
  boolean active
) {}
```

**Rules**

- Immutable
- Flat
- No logic
- Safe for caching and transport

---

## 6) Search model

**AddressSearchCriteria**

```java
public record AddressSearchCriteria(
  String line1,
  String city,
  String postalCode,
  String country,
  String outletCode,
  Boolean active
) {}
```

**Rules**

- All fields optional
- Case-insensitive matching where relevant
- Combined using AND semantics
- Always excludes `deleted_at IS NOT NULL`

---

## 7) Internal implementation — Persistence

**Location**

- `catalog.address.internal.infra.persistence`

**Key points**

- JPA entity `AddressJpaEntity`
- Soft delete (`deleted_at`)
- No `tenant_id` (global catalog)
- Repository remains internal-only

---

## 8) Mapping (INTERNAL)

**AddressMapper**

**Location**

- `catalog.address.internal.mapper`

**Responsibilities**

- Map `AddressJpaEntity` → `AddressView`
- Map lists of entities to lists of views
- Use `CommonIdMapper` for ID wrappers

**Example**

```java
@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface AddressMapper {
  AddressView toView(AddressJpaEntity entity);
  List<AddressView> toViews(List<AddressJpaEntity> entities);
}
```

> ❗ Mapping MUST NOT live in controllers or API.

---

## 9) Cache policy

**Cache names**

- `catalog:address:active`
- `catalog:address:by_id`

**Rules**

- `listActive()` → cached
- `findById()` → cached
- `search()` → **NOT** cached (criteria-based)

**Eviction**

- Eviction occurs in write services only: create, update, soft-delete, hard-delete (maintenance)

---

## 10) Write side (Admin)

**Scope**

- Platform-level only
- Requires `SUPER_ADMIN`

**Entry points**

- `catalog.address.internal.infra.web` (controllers)

**Service**

- `catalog.address.internal.admin.AddressAdminService`

**Responsibilities**

- Validation
- Persistence
- Cache eviction
- Mapping (entity → view)
- Deduplication logic

Controllers remain thin.

---

## 11) Deduplication

Address catalog supports deduplication based on:

- `postalCode`
- `line1`
- `city`
- `country`

**Rules**

- If multiple matches found: one address is kept
- Duplicates are hard-deleted
- Operation is explicit and admin-triggered
- No automatic background deduplication

---

## 12) Security

- Catalog is global
- No RLS
- All write endpoints require `SUPER_ADMIN`
- Read endpoints exposed only where explicitly allowed

---

## 13) Testing expectations

Minimum coverage:

- `listActive()` filters `active=true` and `deleted_at IS NULL`
- `search()` respects criteria combinations
- Mapping correctness
- Cache eviction on write

**ArchUnit**:

- no `api` → `internal` dependency
- no controller returning JPA entities

---

## 14) Summary (table)

| Aspect | Decision |
|---|---|
| Catalog type | Reference / lookup |
| Paging | ✅ Allowed |
| Caching | ✅ Read-only |
| Events | ❌ None |
| Business logic | ❌ None |
| Mapping | Internal only |
| SDR | ❌ Forbidden |

---

## 15) Related documents

- Conventions — Catalogs (CRUD, Cache & Exposure)
- `AGENTS.md`
- `openspec/context/75-catalog-rules.md`
- `ARCHITECTURE.md`
