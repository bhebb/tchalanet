## Context

### État actuel — bugs fonctionnels

```
TenantAdminSettingsService.search()
  ✗ utilise SettingsCatalog.resolve() → ResolvedSettingView sans ID
  ✗ AdminSettingRow.id = null (hardcodé) → DELETE /config/settings/{id} inutilisable

TenantAdminI18nService.search()
  ✗ throw UnsupportedOperationException → GET /config/i18n crash en prod

TenantAdminI18nService.resolvePreview()
  ✗ throw UnsupportedOperationException → GET /config/i18n/resolve crash en prod
```

### Ce qui existe et fonctionne

| Composant                                            | Package                           | Disponible                                        |
| ---------------------------------------------------- | --------------------------------- | ------------------------------------------------- |
| `SettingsAdminService.search(criteria, pageRequest)` | `catalog.settings.internal.write` | ✅ déjà injecté dans `TenantAdminSettingsService` |
| `SettingsAdminService.create()` / `delete()`         | idem                              | ✅ déjà utilisés pour upsert/delete               |
| `I18nOverridesCatalog.search(criteria, pageRequest)` | `catalog.i18n.api`                | ✅ api publique                                   |
| `I18nOverridesCatalog.resolveLocale(locale, ctx)`    | `catalog.i18n.api`                | ✅ api publique                                   |
| `I18nOverridesAdminService.create()` / `delete()`    | `catalog.i18n.internal.write`     | ✅ déjà injecté dans `TenantAdminI18nService`     |

**Conclusion** : toutes les briques nécessaires existent. Les corrections sont des appels au bon composant.

## Goals / Non-Goals

**Goals:**

- Corriger `TenantAdminSettingsService.search()` : utiliser `settingsAdmin.search()` (IDs réels)
- Corriger `TenantAdminI18nService.search()` : brancher sur `I18nOverridesCatalog.search()`
- Corriger `TenantAdminI18nService.resolvePreview()` : brancher sur `I18nOverridesCatalog.resolveLocale()`

**Non-Goals:**

- Ne pas créer de CommandBus, commands, ou query handlers — les catalog internal/write services sont le bon niveau
- Ne pas modifier les controllers (paths inchangés)
- Ne pas modifier les interfaces catalog api
- Ne pas paginer `GET /config/settings` si le controller retourne déjà `List<>`

## Decisions

### Décision 1 — `settingsAdmin.search()` avec filtre `level=TENANT` pour corriger les IDs

**Choix** : Remplacer `settingsCatalog.resolve()` par `settingsAdmin.search(new SearchSettingsCriteria(namespace, settingKey, SettingLevel.TENANT, tenantId, active), page)` dans `TenantAdminSettingsService.search()`.

**Rationale** : `SettingsAdminService` est déjà injecté dans le service. `search()` retourne `TchPage<SettingView>` avec des `id` réels. `resolve()` est la mauvaise méthode — elle fait une résolution hiérarchique GLOBAL→TENANT pour la configuration effective, pas un inventaire admin avec IDs.

**Note** : `SearchSettingsCriteria` vit dans `internal.web.model` mais est utilisé dans un contexte admin (écriture/gestion) — acceptable puisque le service tenantadmin est déjà dans ce contexte.

---

### Décision 2 — `I18nOverridesCatalog.search()` pour l'implémentation du listing i18n

**Choix** : Injecter `I18nOverridesCatalog` (api publique) dans `TenantAdminI18nService` pour `search()`.

**Rationale** : `I18nOverridesCatalog` est l'API publique du catalog i18n et expose déjà `search(SearchI18nOverridesCriteria, TchPageRequest)` retournant `TchPage<I18nOverrideView>` avec IDs réels. Pas de duplication nécessaire. Les writes (`upsert`, `delete`) continuent à passer par `I18nOverridesAdminService` (internal write) — inchangé.

---

### Décision 3 — `I18nOverridesCatalog.resolveLocale(locale, ctx)` pour le resolve preview

**Choix** : Appeler directement `i18nOverridesCatalog.resolveLocale(locale, ctx)`.

**Rationale** : Méthode publique existante dans `I18nOverridesCatalog.api`. Retourne `Map<String,String>` avec les overrides effectifs du tenant depuis le contexte. Implémentation triviale — 1 ligne.

## Risks / Trade-offs

| Risque                                                                             | Mitigation                                                                              |
| ---------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `settingsAdmin.search()` avec fenêtre de page fixe si controller retourne `List<>` | Utiliser une page large (ex: 200) ou ajouter pagination au controller dans un PR séparé |
| `SearchI18nOverridesCriteria.tenantId` est `UUID` (pas `TenantId`)                 | Appeler `ctx.tenantIdSafe().value()` pour extraire le `UUID`                            |

## Migration Plan

1. Corriger `TenantAdminSettingsService.search()`
2. Injecter `I18nOverridesCatalog` dans `TenantAdminI18nService` + corriger `search()` et `resolvePreview()`
3. Build + tests

**Rollback** : revert simple (pas de migration Flyway).

## Open Questions

- _(aucune)_
