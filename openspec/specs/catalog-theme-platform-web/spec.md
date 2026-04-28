# catalog-theme-platform-web Specification

## Purpose

TBD - created by archiving change migrate-platformadmin-slices-to-catalogs. Update Purpose after archive.

## Requirements

### Requirement: Endpoint GET /platform/theme-presets/overview dans ThemeAdminController

`ThemeAdminController` SHALL exposer `GET /platform/theme-presets/overview` depuis
`catalog/theme/internal/web/` (path `/platform/theme-presets`). `ThemeCatalog` SHALL être injecté en plus
de `ThemePresetAdminService`. L'endpoint appelle `ThemeCatalog.stats()` et retourne
`ApiResponse<ThemePresetStatsView>` (existant : `total`, `active`).
La sécurité `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau classe est déjà en place.

#### Scenario: Overview retourne les statistiques de themes

- **WHEN** `GET /platform/theme-presets/overview` est appelé par un SUPER_ADMIN
- **THEN** la réponse contient les champs `total` et `active`

#### Scenario: Overview interdit à un non-SUPER_ADMIN

- **WHEN** `GET /platform/theme-presets/overview` est appelé sans `SUPER_ADMIN`
- **THEN** le serveur retourne `403 Forbidden`

### Requirement: Endpoint GET /platform/theme-presets (listing actif) dans ThemeAdminController

`ThemeAdminController` SHALL exposer `GET /platform/theme-presets` (listing) via
`ThemeCatalog.listActive()`, retournant `ApiResponse<List<ThemePresetView>>`.

#### Scenario: Listing des themes actifs

- **WHEN** `GET /platform/theme-presets` est appelé par un SUPER_ADMIN
- **THEN** la réponse contient la liste des `ThemePresetView` actifs

### Requirement: Suppression de features/platformadmin/theme/

Le dossier `features/platformadmin/theme/` et la classe `PlatformAdminThemeController` SHALL être supprimés
après enrichissement de `ThemeAdminController`.

#### Scenario: Le package features/platformadmin/theme n'existe plus

- **WHEN** le build Maven est exécuté après migration
- **THEN** aucune classe dans `features/platformadmin/theme` n'existe dans le classpath
