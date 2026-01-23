# Spec Delta — ThemePreset catalog (catalog/theme)

## Objectif

Fournir un catalogue `catalog/theme` read-only contenant des ThemePreset stables, et définir les exigences formelles, scénarios et critères d'acceptation pour l'implémentation et la migration vers `core/tenanttheme`.

## ADDED Requirements

### Requirement: R1 — Lecture des presets

The `ThemePresetCatalog` MUST expose the following read operations:

- `List<ThemePresetView> listActive()` — MUST return presets where `deleted_at IS NULL AND active = true`.
- `Optional<ThemePresetView> findById(ThemePresetId id)` — MUST return the preset if present and not soft-deleted.
- `Optional<ThemePresetView> findByCode(String code)` — MUST return the preset if present and not soft-deleted.

#### Scenario: lecture des presets actifs

- Given : en base 3 presets (2 actifs, 1 soft-deleted)
- When : appel de `listActive()`
- Then : la réponse contient précisément les 2 presets actifs (le soft-deleted est exclu)

#### Scenario: recherche par id

- Given : un preset existant avec un id connu
- When : appel de `findById(id)`
- Then : l'option contient le preset

#### Scenario: recherche par code

- Given : un preset avec code `modern-light`
- When : appel de `findByCode("modern-light")`
- Then : le preset correspondant est renvoyé

---

### Requirement: R2 — Admin writes (internal)

The catalog MUST provide an internal admin service for management operations (create / update / soft-delete).

- Service expected: `catalog/theme/internal/write/ThemePresetAdminService` — MUST return `ThemePresetView` (mapping via MapStruct).
- Behavior: writes MUST invalidate caches `catalog.theme.cache.ACTIVE` and `catalog.theme.cache.BY_CODE` (for example via `@CacheEvict`).

#### Scenario: création et visibilité

- Given : aucun preset `new-theme` en base
- When : admin appelle `create(new-theme)`
- Then : `findByCode("new-theme")` renvoie le preset et les caches pertinents ont été invalidés

#### Scenario: soft-delete

- Given : preset existant
- When : admin appelle `softDelete(id)`
- Then : `listActive()` ne renvoie plus le preset et `deleted_at` est non-null

---

### Requirement: R3 — Contrainte d'unicité

The `code` property MUST be unique globally. The uniqueness constraint SHOULD be enforced at the DB level (recommended) and the admin service MUST return a readable error when a duplicate is attempted.

#### Scenario: duplication de code

- Given : un preset en base avec `code = 'light-v1'`
- When : tentative de création d'un second preset avec `code = 'light-v1'`
- Then : l'insertion échoue (violation d'unicité) et le service admin renvoie une erreur lisible (400/409 selon conventions)

---

### Requirement: R4 — Soft-delete

Presets MUST be soft-deletable via a `deleted_at` timestamp. Read operations MUST filter out soft-deleted entries (i.e., `deleted_at IS NULL`).

#### Scenario: soft-delete et filtration

- Given : preset marqué `deleted_at != NULL`
- When : appel de `listActive()` ou `findByCode(code)`
- Then : le preset n'apparaît pas

---

### Requirement: R5 — Mapping et frontières API

Public APIs in `catalog/*/api` MUST return immutable Views; mapping from JPA entities to Views MUST occur exclusively in `internal/mapper`. Controllers MUST NOT expose JPA entities.

#### Scenario: mapper isolation

- Given : `ThemePresetJpaEntity` en base
- When : `ThemePresetCatalogImpl` appelle le mapper
- Then : le consumer ne voit que `ThemePresetView` (aucune entité JPA exposée)

---

## Non-Fonctionnelles (NF)

### NF1 — Caching

- `listActive()` and `findByCode()` MUST be `@Cacheable`.
- TTL configurable; invalidation MUST be immediate after writes.

#### Scenario: cache invalidation

- Given : un preset est mis à jour via `ThemePresetAdminService.update`
- When : appel `findByCode(code)` après la mise à jour
- Then : la réponse reflète la nouvelle valeur (cache invalidé)

### NF2 — Performance

- Lecture optimisée ; pagination non requise pour un petit nombre de presets mais prévoir dimensionnement.

### NF3 — Sécurité

- Les endpoints admin doivent être protégés (ROLE_PLATFORM_ADMIN / ROLE_ADMIN). Les lecteurs internes peuvent être utilisés par d'autres services.

---

## Critères d'acceptation

- Tests unitaires et d'intégration couvrant R1..R5 passent (utiliser H2 pour integration tests).
- Migration SQL fournie et testée (création table + index unique ou partial unique selon SGBD).
- ArchUnit tests vérifient l'isolation `catalog/*/api` vs `catalog/*/internal`.
- Documentation `DOMAIN_THEME.md` incluse et relie `catalog/theme` à `core/tenanttheme`.

---

## Déploiement & migration

- Déployer `catalog/theme` (lecture-only) en premier.
- Déployer `core/tenanttheme` ensuite et basculer handlers pour utiliser `ThemePresetCatalog`.
- Supprimer le module legacy `theme` après validation complète.

---

<!-- EOF -->
