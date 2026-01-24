## ADDED Requirements

### Requirement: G1 — Read operations

`GameCatalog` MUST expose:

- `List<GameView> listActive()` → `deleted_at IS NULL AND active=true`
- `Optional<GameView> findById(GameId id)` → `deleted_at IS NULL`
- `Optional<GameView> findByCode(String code)` → `deleted_at IS NULL`
  - MUST return inactive games too (`active=false`) if not soft-deleted

All reads MUST filter `deleted_at IS NULL`.

#### Scenario: listActive filters

- **WHEN** `listActive()` is called
- **THEN** it returns only games where `active=true` and `deleted_at` is null

#### Scenario: findByCode returns inactive

- **GIVEN** game code `HT_LOTO3` exists and `active=false`
- **WHEN** `findByCode("HT_LOTO3")` is called
- **THEN** it returns an Optional containing the game with `active=false`

### Requirement: G2 — Admin writes (internal)

`GameAdminService` MUST support:

- create/update/deactivate/softDelete
- hard delete MUST NOT exist
- returns `GameView`
- mapping via MapStruct only

#### Scenario: softDelete filters from reads

- **GIVEN** a game exists
- **WHEN** it is soft-deleted via `GameAdminService`
- **THEN** it no longer appears in `listActive` or `findByCode`

### Requirement: G3 — Uniqueness

`code` MUST be globally unique.
DB uniqueness constraint SHOULD be enforced.
Duplicate MUST return readable error (409 recommended).

#### Scenario: Duplicate code creation

- **GIVEN** a game with code `HT_BOLET` exists
- **WHEN** creating another game with code `HT_BOLET`
- **THEN** the operation fails with a uniqueness error

### Requirement: G4 — Mapping & boundaries

The system MUST enforce strict boundaries.
The API MUST return immutable `GameView` only.
Mapping MUST occur only in `internal/mapper`.
Controllers MUST NOT expose entities.

#### Scenario: API return type

- **WHEN** any `GameCatalog` method is called
- **THEN** it returns `GameView` or a collection of `GameView`, never an entity

### Requirement: G5 — Cache

The system MUST cache reads where appropriate.
Writes MUST evict caches.
Cache constants MUST live in `internal/cache`.

#### Scenario: Cache eviction

- **GIVEN** a game is cached
- **WHEN** the game is updated via `GameAdminService`
- **THEN** the cache entry is evicted or updated
