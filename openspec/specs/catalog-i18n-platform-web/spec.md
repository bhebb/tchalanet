# catalog-i18n-platform-web Specification

## Purpose

TBD - created by archiving change migrate-platformadmin-slices-to-catalogs. Update Purpose after archive.

## Requirements

### Requirement: Endpoint GET /platform/i18n-overrides/overview dans PlatformI18nOverridesController

`PlatformI18nOverridesController` (existant dans `catalog/i18n/internal/web/`) SHALL exposer
`GET /platform/i18n-overrides/overview` avec `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au
niveau méthode. Appelle `I18nOverridesCatalog.keyStats()`. Retourne `ApiResponse<I18nGlobalOverviewView>`
(record inline : `generatedAt: Instant`, `summary: Summary(totalKeys, totalLocales, totalOverrides)`).

#### Scenario: Overview retourne les statistiques globales

- **WHEN** `GET /platform/i18n-overrides/overview` est appelé par un SUPER_ADMIN
- **THEN** la réponse contient `data.summary.totalKeys`, `data.summary.totalLocales`, `data.summary.totalOverrides`

#### Scenario: Overview interdit au TENANT_ADMIN

- **WHEN** `GET /platform/i18n-overrides/overview` est appelé par un utilisateur TENANT_ADMIN only
- **THEN** le serveur retourne `403 Forbidden`

### Requirement: Endpoint GET /platform/i18n-overrides/resolve (cross-tenant) dans PlatformI18nOverridesController

`PlatformI18nOverridesController` SHALL exposer `GET /platform/i18n-overrides/resolve` avec
`@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau méthode, paramètres `locale` (requis)
et `tenantId` (optionnel).

- Sans `tenantId` → `catalog.resolveLocale(locale)` (GLOBAL uniquement)
- Avec `tenantId` → `catalog.resolveLocaleForTenant(locale, tenantId)` (GLOBAL + TENANT)
- Retourne `ApiResponse<Map<String, String>>`

Note : L'endpoint existant `GET /resolve/{locale}` (résolution tenant par contexte) est conservé intact.

#### Scenario: Résolution globale sans tenant (SUPER_ADMIN)

- **WHEN** `GET /platform/i18n-overrides/resolve?locale=fr` est appelé par un SUPER_ADMIN sans `tenantId`
- **THEN** la réponse contient la map des overrides GLOBAL uniquement pour `fr`

#### Scenario: Résolution cross-tenant avec tenantId (SUPER_ADMIN)

- **WHEN** `GET /platform/i18n-overrides/resolve?locale=fr&tenantId=<id>` est appelé par un SUPER_ADMIN
- **THEN** la réponse contient la map fusionnée GLOBAL + TENANT pour `fr` et le tenant spécifié

### Requirement: Suppression de features/platformadmin/i18nglobal/

Le dossier `features/platformadmin/i18nglobal/` et sa classe `PlatformAdminI18nGlobalController` SHALL
être supprimés après enrichissement du controller catalog.

#### Scenario: Le package features/platformadmin/i18nglobal n'existe plus

- **WHEN** le build Maven est exécuté après migration
- **THEN** aucune classe dans `features/platformadmin/i18nglobal` n'existe dans le classpath
