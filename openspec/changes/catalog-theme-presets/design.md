# Design - catalog-theme-presets

## But

Séparer la responsabilité des ThemePreset (catalogue) de la logique d'application par tenant (core/tenanttheme). Fournir un design technique clair pour `catalog/theme` et indiquer les points d'interaction avec `core/tenanttheme`.

## Modèle de données recommandé

Table: `theme_preset`

Champs principaux :

- `id` UUID PRIMARY KEY
- `code` VARCHAR NOT NULL
- `vendor` VARCHAR NULL
- `config` JSONB NOT NULL
- `label_key` VARCHAR NULL
- `active` BOOLEAN NOT NULL DEFAULT TRUE
- `created_at` TIMESTAMP
- `updated_at` TIMESTAMP
- `deleted_at` TIMESTAMP NULL (soft delete)

Contraintes & index

- UNIQUE(code) avec option partial index WHERE deleted_at IS NULL si supporté par le SGBD (recommandé).
- Index sur `code`, index sur `active`.

Entités & mapping

- `ThemePresetJpaEntity` dans `catalog/theme/internal/persistence` extend `BaseEntity`.
- `ThemePresetMapper` (MapStruct) pour transformer `ThemePresetJpaEntity` → `ThemePresetView`.
- Les controllers et services ne doivent pas exposer JPA entities.

API publique (catalog.theme.api)

- `ThemePresetView` (DTO immuable) : id, code, vendor, config(JsonNode), labelKey, active, createdAt, updatedAt
- `ThemePresetCatalog` interface :
  - `List<ThemePresetView> listActive()`
  - `Optional<ThemePresetView> findById(ThemePresetId id)`
  - `Optional<ThemePresetView> findByCode(String code)`

Implémentation interne (conventions)

- `catalog/theme/internal/read/ThemePresetCatalogImpl` : utilise `ThemePresetJpaRepository` + `ThemePresetMapper` ; annoter `listActive` et `findByCode` avec `@Cacheable`.
- `catalog/theme/internal/write/ThemePresetAdminService` : opérations create/update/softDelete ; retourne `ThemePresetView` et invalide caches via `@CacheEvict`.
- `catalog/theme/internal/web/ThemeAdminController` : endpoints admin protégés. Controller thin.

## Cache

- Names:
  - `catalog.theme.cache.ACTIVE`
  - `catalog.theme.cache.BY_CODE`
- TTL : configurable (considérer long TTL pour presets stables)
- Invalidations : `@CacheEvict` en write service pour toutes les clés pertinentes

## Interopération avec `core/tenanttheme`

- `core/tenanttheme` lira les presets via `ThemePresetCatalog` (injection du bean interface) pour valider et appliquer des presets aux tenants.
- `core/tenanttheme` stockera les associations tenant -> theme_preset_id dans `tenant_theme` et gérera lifecycle, versioning et événements.
- `core/tenanttheme` doit résilier ou ignorer presets marqués `deleted_at != null` ou `active=false`.

## Sécurité & audit

- Admin controller protégé (ROLE_PLATFORM_ADMIN / ROLE_ADMIN selon conventions).
- `ThemePresetAdminService` journalise (audit) les create/update/softDelete (id, user, timestamp, delta).

## Tests & qualité

- Mapper unit tests
- Integration tests (H2) pour read impl et admin writes
- ArchUnit tests pour empêcher `catalog.*.api` d'importer `internal`

## Opérations et migrations

- Fournir script SQL pour créer la table `theme_preset` et l'index UNIQUE.
- Plan de déploiement en deux étapes (catalog puis core), sans suppression immédiate du module legacy.

## Notes d'implémentation pratiques

- Pour l'unicité, préférer contrainte DB plutôt que lock applicatif.
- Pour partial-unique index (deleted_at IS NULL) vérifier compatibilité SGBD (Postgres supporte).
- Mapper doit être spring component (componentModel = "spring").

---
