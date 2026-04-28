## ADDED Requirements

### Requirement: Endpoint GET /platform/settings/overview dans PlatformSettingsController

`PlatformSettingsController` (existant dans `catalog/settings/internal/web/`) SHALL exposer
`GET /platform/settings/overview`. `SettingsCatalog` SHALL être injecté en plus de
`SettingsAdminService`. L'endpoint appelle `SettingsCatalog.stats()` et retourne
`ApiResponse<SettingsCatalogStatsView>` (existant : `totalGlobalSettings`, `totalTenantSettings`, `totalActiveSettings`).
La sécurité `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau classe est déjà en place.

#### Scenario: Overview retourne les statistiques de settings

- **WHEN** `GET /platform/settings/overview` est appelé par un SUPER_ADMIN
- **THEN** la réponse contient les champs `totalGlobalSettings`, `totalTenantSettings`, `totalActiveSettings`

#### Scenario: Overview interdit à un non-SUPER_ADMIN

- **WHEN** `GET /platform/settings/overview` est appelé sans `SUPER_ADMIN`
- **THEN** le serveur retourne `403 Forbidden`

### Requirement: Suppression de features/platformadmin/settingsglobal/

Le dossier `features/platformadmin/settingsglobal/` (controller, orchestrateur, view) SHALL être
supprimé après enrichissement de `PlatformSettingsController`. `PlatformAdminSettingsGlobalOrchestrator`
est absorbé via injection directe de `SettingsCatalog` dans le controller existant.

#### Scenario: Le package features/platformadmin/settingsglobal n'existe plus

- **WHEN** le build Maven est exécuté après migration
- **THEN** aucune classe dans `features/platformadmin/settingsglobal` n'existe dans le classpath
