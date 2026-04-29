## Why

`features/tenantadmin/config/settings/` et `features/tenantadmin/config/i18n/` présentent trois
bugs fonctionnels identifiés à l'audit :

1. **`AdminSettingRow.id = null`** (P2-4) : `TenantAdminSettingsService.search()` utilise
   `SettingsCatalog.resolve()` qui retourne des `ResolvedSettingView` sans ID — l'opération
   `DELETE /config/settings/{id}` est inutilisable depuis le frontend.

2. **`UnsupportedOperationException` dans `TenantAdminI18nService.search()`** (P1-4) :
   stub non implémenté — `GET /config/i18n` lance une exception en production.

3. **`UnsupportedOperationException` dans `TenantAdminI18nService.resolvePreview()`** (P1-4) :
   stub non implémenté — `GET /config/i18n/resolve` lance une exception en production.

Ces bugs sont des corrections d'implémentation pures — aucun changement d'architecture.
Les services appropriés existent déjà : `SettingsAdminService.search()` retourne
`TchPage<SettingView>` avec IDs réels ; `I18nOverridesCatalog.search()` et
`I18nOverridesCatalog.resolveLocale()` sont publics et opérationnels.

## What Changes

### Slice settings/

- **Corriger** `TenantAdminSettingsService.search()` :
  - Remplacer `settingsCatalog.resolve(criteria)` par `settingsAdmin.search(SearchSettingsCriteria(namespace, settingKey, TENANT, tenantId, active), pageRequest)`
  - `settingsAdmin` est **déjà injecté** — aucune nouvelle dépendance
  - Mapper `SettingView.id()` → `AdminSettingRow.id` non-null
  - Ajuster la signature pour accepter `TchPageRequest` si le controller le nécessite

### Slice i18n/

- **Corriger** `TenantAdminI18nService.search()` :

  - Injecter `I18nOverridesCatalog` (api publique — légal)
  - Appeler `i18nOverridesCatalog.search(SearchI18nOverridesCriteria(TENANT, locale, q, active, tenantId.value(), "active"), pageReq)`
  - Mapper `I18nOverrideView → AdminI18nRow` avec `id` non-null
  - Supprimer le `throw UnsupportedOperationException`

- **Corriger** `TenantAdminI18nService.resolvePreview()` :

  - Appeler `i18nOverridesCatalog.resolveLocale(locale, ctx)`
  - Supprimer le `throw UnsupportedOperationException`

- `TenantAdminI18nService.upsert()` et `delete()` → **inchangés** (fonctionnels)

## Capabilities

### New Capabilities

_(aucune — corrections d'implémentation uniquement)_

### Modified Capabilities

_(aucun changement de spec d'exigences — correction de bugs)_

## Impact

- **Correction functional** : `GET /config/settings` retourne des rows avec `id` non-null
- **Correction functional** : `GET /config/i18n` et `GET /config/i18n/resolve` ne lèvent plus `UnsupportedOperationException`
- **Aucun breaking path** — les controllers `TenantAdminSettingsController` et `TenantAdminI18nController` restent inchangés
- **Aucune nouvelle classe** — modifications dans les services existants uniquement
