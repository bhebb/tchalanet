## 1. Corriger TenantAdminSettingsService — IDs null dans search()

- [ ] 1.1 Dans `TenantAdminSettingsService.search()`, remplacer l'appel `settingsCatalog.resolve(criteria)` par `settingsAdmin.search(new SearchSettingsCriteria(namespace, settingKey, SettingLevel.TENANT, ctx.tenantIdSafe(), active), pageRequest)` — `settingsAdmin` est déjà injecté
- [ ] 1.2 Importer `com.tchalanet.server.catalog.settings.internal.web.model.SearchSettingsCriteria` et `com.tchalanet.server.catalog.settings.api.model.SettingLevel`
- [ ] 1.3 Adapter la signature `search()` pour accepter `TchPageRequest pageRequest` si besoin (ou utiliser une page fixe `TchPageRequest.ofSize(200)` pour compatibilité ascendante)
- [ ] 1.4 Réécrire le mapper : `new AdminSettingRow(settingView.id().value().toString(), settingView.namespace(), settingView.settingKey(), ...)` — id non-null
- [ ] 1.5 Vérifier que `upsert()` et `delete()` restent inchangés (ils fonctionnent déjà)

## 2. Corriger TenantAdminI18nService — stubs UnsupportedOperationException

- [ ] 2.1 Ajouter `I18nOverridesCatalog i18nOverridesCatalog` dans les dépendances injectées (en plus de `I18nOverridesAdminService` déjà présent)
- [ ] 2.2 Réécrire `search()` : construire `new SearchI18nOverridesCriteria(I18nOverrideLevel.TENANT, locale, q, active, ctx.tenantIdSafe().value(), "active")` → appeler `i18nOverridesCatalog.search(criteria, pageReq)` → mapper `I18nOverrideView → AdminI18nRow` avec `id = view.id().value().toString()`
- [ ] 2.3 Réécrire `resolvePreview()` : `return i18nOverridesCatalog.resolveLocale(locale, ctx)` — supprimer le `throw UnsupportedOperationException`
- [ ] 2.4 Vérifier que `upsert()` et `delete()` restent inchangés (ils fonctionnent déjà via `I18nOverridesAdminService`)

## 3. Documentation

- [ ] 3.1 Créer ou mettre à jour `features/tenantadmin/config/FEATURE_TENANT_CONFIG.md`
  - Documenter les 3 slices : settings/, i18n/, identity/
  - Indiquer que settings/ et i18n/ utilisent `catalog.*.internal.write` pour les mutations et `I18nOverridesCatalog` (api) pour les lectures i18n

## 4. Validation

- [ ] 4.1 Exécuter `./mvnw verify` — build vert
- [ ] 4.2 Tester `GET /config/settings` → vérifier que `id` != null dans chaque row
- [ ] 4.3 Tester `GET /config/i18n` → 200 OK, pas d'exception
- [ ] 4.4 Tester `GET /config/i18n/resolve?locale=fr` → Map<String,String> valide
- [ ] 4.5 Tester `DELETE /config/settings/{id}` avec un id retourné par le search → 200 OK
