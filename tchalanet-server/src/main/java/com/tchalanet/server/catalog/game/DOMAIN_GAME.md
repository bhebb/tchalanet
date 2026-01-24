# Domaine Catalog Game

> Référentiel pur des jeux globaux. Fournit une API stable, cacheable pour les domaines `core.sales` (vente), `core.draw` (tirages), et `core.tenantgame` (configuration tenant). Aucune logique métier, aucun événement, aucun lifecycle tenant.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Maintenir le registre global des jeux (codes, catégories, limites de combinaisons) et exposer une API stable, cacheable et découplée.

**Ce que le domaine fait**

- Stocke et expose les définitions de jeux (code, name, category, minDigits, maxDigits, combination).
- Marque les jeux comme actifs/inactifs et soft-delete pour traçabilité.
- Fournit une API publique (GameCatalog) pour consultation.
- Gère les opérations admin (CRUD + cache eviction).

**Ce que le domaine ne fait pas**

- Aucune logique métier (calculs, validations complexes).
- Aucun événement domaine (référentiel pur).
- Aucune logique tenant-scoped (ça va dans core/tenantgame).
- Aucun orchestration de workflow.

---

## 2. Modèle métier (agrégats / entités)

### Entités principales

- **Game** (JPA Entity) — agrégat racine
  - `id` (UUID, PK)
  - `code` (String, UNIQUE, length 32) — clé fonctionnelle
  - `name` (String, length 128)
  - `category` (String, length 32) — ex: HAITI
  - `combination` (String, length 32) — type de jeu
  - `minDigits`, `maxDigits` (int) — règles de combinaisons
  - `description` (String)
  - `active` (boolean, default true) — publi cation
  - `sortOrder` (int, default 0) — tri admin
  - `deleted_at` (Instant, nullable) — soft-delete
  - audit columns (createdAt, updatedAt, createdBy, updatedBy)

### Projections / Views

- **GameView** (immutable record)
  - Exposée via API publique (GameCatalog)
  - Jamais exposée en JPA entity brut
  - Mapping via MapStruct (GameMapper)

### Invariants métier

- `code` globally unique (DB constraint + validation admin).
- `minDigits` ≤ `maxDigits`.
- `active=false` n'impacte pas la persistance (reste visible pour audit).
- `deleted_at != NULL` filtre partout (soft-delete strict).

---

## 3. Architecture (75-catalog-rules.md)

### Package structure

```
catalog/game/
├── api/                          # Contrat public
│   ├── GameCatalog.java         # Interface (read-only)
│   ├── GameView.java             # Immutable record
│   └── GameId.java              # Typed ID wrapper
├── internal/
│   ├── read/
│   │   └── GameCatalogImpl.java   # Implémentation (cacheable)
│   ├── write/
│   │   └── GameAdminService.java # CRUD admin
│   ├── mapper/
│   │   └── GameMapper.java       # MapStruct (entity → view)
│   ├── cache/
│   │   └── GameCacheNames.java   # Constantes cache
│   └── infra/
│       ├── persistence/
│       │   ├── GameJpaEntity.java
│       │   └── GameJpaRepository.java
│       └── web/
│           └── GameAdminController.java # REST CRUD
```

### Règles d'isolation

- ✅ `api/` = contrat public (GameCatalog, GameView)
- ✅ `internal/` = implémentation cachée
- ✅ **Aucune dépendance from `core/tenantgame` vers `internal/**`\*\*
- ✅ Validation via `GameCatalog` API publique only

---

## 4. API Publique (GameCatalog)

### Opérations de lecture

```java
List<GameView> listActive()
  // Retourne jeux où active=true AND deleted_at IS NULL
  // Cacheable, rapide, pour bootstrap/UI

Optional<GameView> findByCode(String code)
  // Lookup fonctionnel par code
  // Retourne inactifs si actifs=false (pas soft-deleted)
  // Cacheable

Optional<GameView> findById(GameId id)
  // Lookup technique par ID
  // Filtre deleted_at IS NULL
  // Cacheable
```

**Spec compliance**: Spec G1 (read operations)

### Opérations d'admin (GameAdminService)

```java
GameView create(GameCreateRequest req)
  // Insert + default active=true, sortOrder=0

GameView update(GameId id, GameUpdateRequest req)
  // Patch fields (non-null only)

void deactivate(GameId id)
  // Set active=false (preserve soft-delete)

void softDelete(GameId id)
  // Set deleted_at=now() + active=false
```

**Spec compliance**: Spec G2 (admin writes)

### REST Controller (GameAdminController)

| Method | Endpoint                          | HTTP | Returns               |
| ------ | --------------------------------- | ---- | --------------------- |
| POST   | `/platform/games`                 | 201  | ApiResponse<GameView> |
| PUT    | `/platform/games/{id}`            | 200  | ApiResponse<GameView> |
| DELETE | `/platform/games/{id}`            | 200  | ApiResponse<Void>     |
| POST   | `/platform/games/{id}/deactivate` | 200  | ApiResponse<Void>     |

- ✅ @PreAuthorize('SUPER_ADMIN')
- ✅ Returns ApiResponse<T> (never raw entities)
- ✅ Error handling via ProblemDetail

**Spec compliance**: Spec G2 + web_api.md

---

## 5. Cache Strategy

### Cache names (GameCacheNames)

- `catalog.game.cache.ACTIVE_GAMES` — listActive()
- `catalog.game.cache.GAME_BY_CODE` — findByCode(code)
- `catalog.game.cache.GAME_BY_ID` — findById(id)

### Eviction

- ✅ All writes (@CacheEvict on create/update/deactivate/softDelete)
- ✅ Evicts all 3 caches (allEntries=true)

**Spec compliance**: Spec G5 (cache policy)

---

## 6. Dépendances

**Dépend de**:

- `common.types.id` (GameId typed wrapper)
- `common.types.BaseEntity` (audit columns)
- `common.mapper.CommonIdMapper` (ID conversions)

**Utilisé par**:

- `core.tenantgame` (valide via GameCatalog API only)
- `core.sales` (lookup game par code)
- `core.draw` (lookup game rules)
- Features (bootstrap, pagemodel)

---

## 7. Notes techniques

### Typed IDs (typed_ids.md)

- ✅ GameId wrapper (value = UUID)
- ✅ Controllers reçoivent UUID, convertis via Spring converter
- ✅ Service manipule GameId typé

### Mapping (api_response.md, web_api.md)

- ✅ GameView = immutable record (public API)
- ✅ GameMapper = MapStruct (internal only)
- ✅ Controllers retournent ApiResponse<GameView>
- ✅ Jamais d'entité JPA exposée

### RLS (Row-Level Security)

- catalog/game = global (pas de tenant_id)
- core/tenantgame = tenant-scoped (avec RLS)

---

## 8. Séparation des responsabilités

### catalog/game (référentiel pur)

✅ Lis/écrit les définitions globales de jeux  
✅ Expose une API stable et cacheable  
✅ Aucune logique tenant  
✅ Aucun événement

### core/tenantgame (lifecycle tenant)

✅ Gère activation/configuration par tenant  
✅ Valide via GameCatalog (API only)  
✅ Publie événements (TenantGameUpdated)  
✅ Applique RLS

---

## 9. Patterns appliqués

- ✅ **Pure Catalog Pattern** (75-catalog-rules.md)
- ✅ **Typed IDs** (typed_ids.md)
- ✅ **API Response** (api_response.md, web_api.md)
- ✅ **Cache Strategy** (conventions/cache.md)
- ✅ **Admin Service + Controller** (PlanAdminService parallel)

---

## 10. Conformité aux specs

| Spec    | Requirement        | Status                                    |
| ------- | ------------------ | ----------------------------------------- |
| G1      | Read operations    | ✅ listActive, findByCode, findById       |
| G2      | Admin writes       | ✅ create, update, deactivate, softDelete |
| G3      | Uniqueness         | ✅ code UNIQUE (DB + validation)          |
| G4      | Mapping            | ✅ Views only, no entity leakage          |
| G5      | Cache              | ✅ @Cacheable + eviction                  |
| web_api | REST + ApiResponse | ✅ Controllers, ProblemDetail             |
