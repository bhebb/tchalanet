# Theme Preset Specification

## Purpose

This specification defines the read-only Theme Preset catalog used to expose stable, global UI theme presets. Tenant-specific application and lifecycle management are explicitly out of scope and handled in `core/tenanttheme`.

## Requirements

### Requirement: R1 — Lecture des presets

The `ThemeCatalog` MUST expose the following read operations:

- `List<ThemePresetView> listActive()` — MUST return presets where `deleted_at IS NULL AND active = true`.
- `Optional<ThemePresetView> findById(ThemePresetId id)` — Intended for admin/internal usage; MUST return the preset if present and not soft-deleted.
- `Optional<ThemePresetView> findByCode(String code)` — Preferred functional lookup; MUST return the preset if present and not soft-deleted.

#### Scenario: lecture des presets actifs

- Given: en base 3 presets (2 actifs, 1 soft-deleted)
- When: appel de `listActive()`
- Then: la réponse contient précisément les 2 presets actifs

#### Scenario: recherche par id

- Given: un preset existant avec un id connu
- When: appel de `findById(id)`
- Then: l'option contient le preset

#### Scenario: recherche par code

- Given: un preset avec code `modern-light`
- When: appel de `findByCode("modern-light")`
- Then: le preset correspondant est renvoyé

---

### Requirement: R2 — Admin writes (internal)

The catalog MUST provide an internal admin service for management operations (create / update / soft-delete).

- Service: `catalog/theme/internal/write/ThemePresetAdminService`
- Return type: `ThemePresetView` (mapping via MapStruct)
- Cache invalidation MUST evict:
  - `catalog.theme.cache.ACTIVE_PRESETS`
  - `catalog.theme.cache.PRESET_BY_CODE`

#### Scenario: création et visibilité

- Given: aucun preset `new-theme` en base
- When: admin appelle `create(new-theme)`
- Then: `findByCode("new-theme")` renvoie le preset et les caches sont invalidés

#### Scenario: soft-delete

- Given: preset existant
- When: admin appelle `softDelete(id)`
- Then: `listActive()` ne renvoie plus le preset et `deleted_at` est non-null

---

### Requirement: R3 — Contrainte d'unicité

The `code` property MUST be globally unique.

- Uniqueness SHOULD be enforced at the DB level.
- On violation, the admin service MUST return a readable error (recommended: HTTP 409 Conflict with ProblemDetail).

#### Scenario: duplication de code

- Given: un preset avec `code = 'light-v1'`
- When: création d’un second preset avec le même code
- Then: l’opération échoue avec une erreur explicite

---

### Requirement: R4 — Soft-delete

Presets MUST be soft-deletable via `deleted_at`.

- Read operations MUST always filter out soft-deleted entries.

---

### Requirement: R5 — Mapping et frontières API

- Public APIs in `catalog/*/api` MUST return immutable Views.
- Mapping MUST occur exclusively in `internal/mapper`.
- Controllers MUST NOT expose JPA entities.

---
