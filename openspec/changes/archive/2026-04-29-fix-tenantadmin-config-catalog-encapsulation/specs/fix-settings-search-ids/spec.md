## ADDED Requirements

### Requirement: GET /config/settings retourne des rows avec id non-null

`TenantAdminSettingsService.search()` SHALL utiliser `settingsAdmin.search()` avec un
`SearchSettingsCriteria(namespace, settingKey, SettingLevel.TENANT, tenantId, active)` afin
d'obtenir des `SettingView` portant un `id` réel.
Le mapper SHALL produire `AdminSettingRow.id = settingView.id().value().toString()` (non-null).
`SettingsAdminService` est déjà injecté — aucune nouvelle dépendance requise.

#### Scenario: Search settings retourne des IDs non-null

- **WHEN** `GET /config/settings` est appelé par un TENANT_ADMIN
- **THEN** chaque `AdminSettingRow` de la réponse a un champ `id` non-null

#### Scenario: DELETE /config/settings/{id} fonctionne avec l'id retourné

- **WHEN** `DELETE /config/settings/{id}` est appelé avec un `id` obtenu depuis le search
- **THEN** le setting est soft-deleted sans erreur `404` ni `IllegalArgumentException`

### Requirement: TenantAdminSettingsService.search() n'uses plus SettingsCatalog.resolve()

Le service SHALL remplacer l'appel `settingsCatalog.resolve(criteria)` par
`settingsAdmin.search(SearchSettingsCriteria(...), pageRequest)`.
`SettingsCatalog.resolve()` est réservé à la résolution effective hiérarchique — pas à l'inventaire admin.

#### Scenario: Search retourne uniquement les settings de niveau TENANT du tenant courant

- **WHEN** `GET /config/settings?namespace=checkout` est appelé
- **THEN** la réponse contient uniquement des `AdminSettingRow` avec `level = "TENANT"` appartenant au tenant du contexte
