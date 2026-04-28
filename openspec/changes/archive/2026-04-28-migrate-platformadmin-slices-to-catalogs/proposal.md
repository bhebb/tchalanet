## Why

Les slices `i18nglobal`, `settingsglobal` et `theme` de `features/platformadmin/` sont des CRUD
mono-domaines qui n'orchestrent qu'un seul catalog. Par la règle architecturale **Feature = BFF
orchestrant ≥ 2 cores**, ces slices n'ont pas leur place dans `features/`. Chaque catalog doit
exposer ses propres controllers HTTP platform dans `catalog/<bc>/infra/web/`, au lieu de forcer un
détour via `features/platformadmin`.

## What Changes

- **Enrichir** `catalog/i18n/internal/web/PlatformI18nOverridesController` (existant, `/platform/i18n-overrides`)

  - Ajouter `GET /platform/i18n-overrides/overview` → `I18nOverridesCatalog.keyStats()`, SUPER_ADMIN only
  - Ajouter `GET /platform/i18n-overrides/resolve?locale=&tenantId=` (SUPER_ADMIN, résolution cross-tenant)
  - Supprimer le dossier source `features/platformadmin/i18nglobal/`

- **Enrichir** `catalog/settings/internal/web/PlatformSettingsController` (existant, `/platform/settings`)

  - Ajouter `GET /platform/settings/overview` → `SettingsCatalog.stats()`, retour `ApiResponse<SettingsCatalogStatsView>`
  - Injecter `SettingsCatalog` en plus de `SettingsAdminService`
  - Supprimer l'orchestrateur intermédiaire `PlatformAdminSettingsGlobalOrchestrator`
  - Supprimer le dossier source `features/platformadmin/settingsglobal/`

- **Enrichir** `catalog/theme/internal/web/ThemeAdminController` (existant, `/platform/theme-presets`)

  - Ajouter `GET /platform/theme-presets/overview` → `ThemeCatalog.stats()`
  - Ajouter `GET /platform/theme-presets` → `ThemeCatalog.listActive()`
  - Injecter `ThemeCatalog` en plus de `ThemePresetAdminService`
  - Supprimer le dossier source `features/platformadmin/theme/`

- ⚠️ Les packages `catalog/<bc>/internal/web/` **existent déjà** — aucune création de package requise

- **Mettre à jour** `features/platformadmin/FEATURE_PLATFORM_ADMIN.md` (créer s'il n'existe pas)

  - Retirer les 3 slices migrées ; noter que le CRUD catalog est dans `catalog/<bc>/infra/web/`
  - Garder `overview/` (composite cross-catalog)

- `features/platformadmin/overview/` : **NE PAS TOUCHER** (composite cross-catalog)

## Capabilities

### New Capabilities

- `catalog-i18n-platform-web` : Enrichissement de `PlatformI18nOverridesController` — ajout `/overview` (keyStats) et `/resolve` cross-tenant SUPER_ADMIN sur `/platform/i18n-overrides`
- `catalog-settings-platform-web` : Enrichissement de `PlatformSettingsController` — ajout `/overview` (stats) sur `/platform/settings`
- `catalog-theme-platform-web` : Enrichissement de `ThemeAdminController` — ajout `/overview` (stats) et listing actif sur `/platform/theme-presets`

### Modified Capabilities

_(aucun changement de spec d'exigences — refactoring de placement uniquement)_

## Impact

- **3 controllers existants enrichis** dans `catalog/<bc>/internal/web/` (aucune création de fichier)
- **Suppression** de 3 dossiers sous `features/platformadmin/` (i18nglobal, settingsglobal, theme)
- **Aucun breaking path** — les controllers existants gardent leurs paths actuels ; les endpoints `/overview` et `/resolve` sont **ajoutés**
- Pas d'impact sur `features/platformadmin/overview/`
- Pas d'impact sur `features/tenantadmin/` (scope séparé : `spec-fix-core-tenantconfig-p0-p1`)
