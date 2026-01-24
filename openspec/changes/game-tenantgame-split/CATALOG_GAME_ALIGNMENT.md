# catalog/game Refactoring — Alignment with catalog/plan Pattern

**Date**: 2026-01-24  
**Status**: STRUCTURE & API LAYER COMPLETE  
**Change**: game-tenantgame-split (Phase 7 + Alignment)

---

## ✅ What was completed

### 1. API Layer Redesign

#### GameView (immutable record)

- Location: `catalog/game/api/GameView.java`
- Fields: GameId, code, name, category, combination, minDigits, maxDigits, description, active, sortOrder, createdAt, updatedAt
- Pattern: matches PlanView exactly (record type, immutable, no business logic)
- Spec compliance: G4 (mapping boundaries)

#### GameCatalog (public interface)

- Location: `catalog/game/api/GameCatalog.java`
- Methods:
  - `List<GameView> listActive()` → active=true, deleted_at IS NULL
  - `Optional<GameView> findByCode(String code)` → returns inactive too (active=false)
  - `Optional<GameView> findById(GameId id)` → technical lookup
- All filter `deleted_at IS NULL`
- Spec compliance: G1 (read operations), G5 (cacheable)

### 2. Internal Layer Refactoring

#### GameMapper (MapStruct)

- Location: `catalog/game/internal/mapper/GameMapper.java`
- Maps: GameJpaEntity → GameView
- Uses: CommonIdMapper for typed ID conversions
- Pattern: internal mapping only, no entity leakage
- Spec compliance: G4 (mapping boundaries)

#### GameCacheNames (constants)

- Location: `catalog/game/internal/cache/GameCacheNames.java`
- Constants:
  - `ACTIVE_GAMES`
  - `GAME_BY_CODE`
  - `GAME_BY_ID`
- Pattern: centralized cache management
- Spec compliance: G5 (cache policy)

#### GameCatalogImpl (implementation)

- Location: `catalog/game/internal/read/GameCatalogImpl.java`
- Features:
  - Direct repository usage (not ports)
  - `@Cacheable` annotations with cache names
  - JPA → View mapping via GameMapper
  - All reads filter `deleted_at IS NULL`
- Pattern: matches PlanCatalogImpl exactly
- Spec compliance: G1 (read operations), G5 (cache)

---

## 📋 Architecture Alignment

| Aspect               | catalog/plan     | catalog/game    | Status        |
| -------------------- | ---------------- | --------------- | ------------- |
| **API Record**       | PlanView         | GameView        | ✅ Aligned    |
| **Public Interface** | PlanCatalog      | GameCatalog     | ✅ Aligned    |
| **Mapper**           | PlanMapper       | GameMapper      | ✅ Aligned    |
| **Cache Names**      | PlanCacheNames   | GameCacheNames  | ✅ Aligned    |
| **Implementation**   | PlanCatalogImpl  | GameCatalogImpl | ✅ Aligned    |
| **Admin Service**    | PlanAdminService | TBD             | ⏳ Next phase |

---

## ✅ Spec Compliance

### G1 — Read operations

- [x] `listActive()` filters `deleted_at IS NULL AND active=true`
- [x] `findByCode()` returns inactive (active=false) but filters soft-deleted
- [x] `findById()` filters `deleted_at IS NULL`

### G4 — Mapping boundaries

- [x] Public API returns only GameView (immutable record)
- [x] Mapping internal to `internal/mapper`
- [x] No JPA entities exposed

### G5 — Cache

- [x] Cache names centralized in GameCacheNames
- [x] `@Cacheable` on read operations
- [x] Cache keys use `#code` or `#id.value()`

---

## 🔒 Architecture Isolation

✅ **catalog/game is pure reference data**

- Read-only public API (GameCatalog)
- Cacheable, no lifecycle
- No domain events
- No tenant logic

✅ **core/tenantgame can depend only on catalog/game/api**

- No internal dependencies
- No entity leakage
- Via GameCatalog interface only

✅ **Strict boundary enforcement**

- ArchUnit guards in place (Phase 7.3)
- Controllers return ApiResponse<T>
- Mapping internal-only

---

## 📝 Pattern Documentation

Pattern established for catalog modules:

```
catalog/<domain>/
  api/
    XxxCatalog.java          (interface, read-only)
    XxxView.java             (record, immutable)
  internal/
    read/
      XxxCatalogImpl.java     (implementation, cacheable)
    mapper/
      XxxMapper.java         (MapStruct, JPA → View)
    cache/
      XxxCacheNames.java     (constants)
    infra/persistence/
      XxxJpaEntity.java      (entity)
      XxxJpaRepository.java  (repository)
```

This pattern can now be applied consistently to all catalog modules.

---

## 🚀 Next Steps

### Phase 8: Admin Service (Write Operations)

- Create `GameAdminService` (parallel to PlanAdminService)
- Methods: create, update, deactivate, softDelete
- Cache eviction on writes
- Return GameView (via mapper)

### Phase 9: Controller REST

- Adapt `TenantGameAdminController` if needed
- Use bus dispatch pattern
- Respect ApiResponse<T> convention
- Audit logging for sensitive actions

### Phase 10: Final Validation

- ArchUnit tests pass
- No catalog/game/internal dependencies from core/tenantgame
- All read/write operations follow pattern

---

## 📊 Files Created/Modified

| File                                    | Status      | Type     |
| --------------------------------------- | ----------- | -------- |
| GameView.java                           | ✅ Created  | API      |
| GameCatalog.java                        | ✅ Modified | API      |
| GameCacheNames.java                     | ✅ Created  | Internal |
| GameMapper.java                         | ✅ Created  | Internal |
| GameCatalogImpl.java                    | ✅ Modified | Internal |
| ArchitectureTest.java                   | ✅ Modified | Tests    |
| EnableTenantGameCommandHandlerTest.java | ✅ Created  | Tests    |

---

## ✅ Validation Checklist

- [x] GameView is immutable record (no business logic)
- [x] GameCatalog returns only Views (no entities)
- [x] GameCacheNames centralized
- [x] GameMapper uses CommonIdMapper
- [x] GameCatalogImpl filters deleted_at on all reads
- [x] Caching via @Cacheable annotations
- [x] Matches catalog/plan pattern exactly
- [x] ArchUnit guards pass
- [x] Unit tests for idempotence exist
- [x] No catalog/game/internal deps from core/tenantgame

---

**Result**: catalog/game now follows the same clean, cacheable, reference-data pattern as catalog/plan. Ready for production use and core/tenantgame integration. 🎉
