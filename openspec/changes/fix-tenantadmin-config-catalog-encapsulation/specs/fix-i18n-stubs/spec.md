## ADDED Requirements

### Requirement: Search i18n fonctionne

TenantAdminI18nService SHALL injecter I18nOverridesCatalog (api public).
search() SHALL appeler i18nOverridesCatalog.search() avec SearchI18nOverridesCriteria filtre TENANT.
Mapper I18nOverrideView vers AdminI18nRow avec id non-null.
Le throw UnsupportedOperationException SHALL etre supprime.

#### Scenario: Search i18n retourne une page valide

- **WHEN** GET /config/i18n est appele par un TENANT_ADMIN
- **THEN** la reponse est 200 OK avec TchPage AdminI18nRow sans exception

#### Scenario: AdminI18nRow contient un id non-null

- **WHEN** GET /config/i18n est appele
- **THEN** chaque AdminI18nRow a un champ id non-null

### Requirement: resolvePreview est fonctionnel

TenantAdminI18nService.resolvePreview() SHALL appeler i18nOverridesCatalog.resolveLocale(locale, ctx).
Le throw UnsupportedOperationException SHALL etre supprime.

#### Scenario: Resolve preview retourne une map

- **WHEN** GET /config/i18n/resolve?locale=fr est appele
- **THEN** la reponse est 200 OK avec Map non-null, sans exception

#### Scenario: Locale sans override retourne map vide

- **WHEN** GET /config/i18n/resolve?locale=xx est appele
- **THEN** la reponse est 200 OK avec map vide

### Requirement: upsert et delete restent inchanges

Les methodes upsert() et delete() utilisant I18nOverridesAdminService SHALL rester inchangees.

#### Scenario: Upsert cree un override

- **WHEN** PUT /config/i18n est appele avec body valide
- **THEN** l'override est cree avec id non-null
