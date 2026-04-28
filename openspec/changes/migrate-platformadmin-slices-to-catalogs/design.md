## Context

Le backend tchalanet-server est organisé en 4 couches : `common/` · `catalog/` · `core/` · `features/`.
La règle immuable stipule qu'une `feature` est un **BFF orchestrant ≥ 2 cores/catalogs**. Les slices
`i18nglobal`, `settingsglobal` et `theme` de `features/platformadmin/` violent cette règle car
chacune n'appelle qu'un seul catalog.

Les catalogs `i18n`, `settings` et `theme` exposent **déjà** leurs propres controllers HTTP dans
`catalog/<bc>/internal/web/`. Ces controllers doivent être **enrichis** avec les opérations manquantes
issues des slices features, puis les slices features supprimées.

État actuel des controllers cibles :
| Controller existant | Path | Ce qui manque |
|---|---|---|
| `catalog/i18n/internal/web/PlatformI18nOverridesController` | `/platform/i18n-overrides` | `GET /overview` (keyStats), `GET /resolve?locale&tenantId` SUPER_ADMIN |
| `catalog/settings/internal/web/PlatformSettingsController` | `/platform/settings` | `GET /overview` (stats) + injection `SettingsCatalog` |
| `catalog/theme/internal/web/ThemeAdminController` | `/platform/theme-presets` | `GET /overview` (stats), `GET /` (listActive) + injection `ThemeCatalog` |

Slices features à supprimer après enrichissement :
| Slice | Fichiers |
|---|---|
| `features/platformadmin/i18nglobal/` | `PlatformAdminI18nGlobalController` |
| `features/platformadmin/settingsglobal/` | `PlatformAdminSettingsGlobalController` + `PlatformAdminSettingsGlobalOrchestrator` + `SettingsGlobalOverviewView` |
| `features/platformadmin/theme/` | `PlatformAdminThemeController` |

## Goals / Non-Goals

**Goals:**

- Enrichir `PlatformI18nOverridesController` avec les opérations manquantes (`/overview`, `/resolve` SUPER_ADMIN cross-tenant)
- Enrichir `PlatformSettingsController` avec `GET /overview` (stats) via `SettingsCatalog`
- Enrichir `ThemeAdminController` avec `GET /overview` (stats) et `GET /` (listActive) via `ThemeCatalog`
- Supprimer les 3 dossiers `features/platformadmin/<slice>/`
- Créer/mettre à jour `features/platformadmin/FEATURE_PLATFORM_ADMIN.md`
- **Aucun breaking path** — les controllers gardent leurs paths actuels

**Non-Goals:**

- Ne pas toucher `features/platformadmin/overview/`
- Ne pas modifier les interfaces `catalog/<bc>/api/` (contrat public inchangé)
- Ne pas créer de nouveaux controllers — uniquement enrichir ceux qui existent
- Ne pas traiter `features/tenantadmin/config/settings/` (scope séparé : `spec-fix-core-tenantconfig-p0-p1`)

## Decisions

### Décision 1 — Enrichissement des controllers existants (pas de création)

**Choix** : Ajouter les endpoints manquants directement dans `catalog/<bc>/internal/web/` existant.

**Alternatif rejeté** : Créer un nouveau `catalog/<bc>/infra/web/` parallèle.

**Rationale** : Les controllers existent déjà dans `internal/web/` et sont correctement placés architecturalement. Créer un deuxième controller sur le même domaine introduirait de la duplication et de l'ambiguïté. L'enrichissement est la seule option cohérente.

---

### Décision 2 — Orchestrateur `PlatformAdminSettingsGlobalOrchestrator` est supprimé

**Choix** : Injecter `SettingsCatalog` directement dans `PlatformSettingsController`.

**Alternatif rejeté** : Conserver l'orchestrateur dans le catalog.

**Rationale** : L'orchestrateur n'apporte aucune logique métier — il propage simplement `settingsCatalog.stats()`. Sa suppression simplifie l'arbre. `PlatformSettingsController` injecte déjà `SettingsAdminService` ; ajouter `SettingsCatalog` est trivial.

---

### Décision 3 — Sécurité `/overview` et `/resolve` SUPER_ADMIN dans `PlatformI18nOverridesController`

**Choix** : Annotation `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau méthode pour les nouveaux endpoints, en complément de l'annotation classe existante (`hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')`).

**Rationale** : Les opérations de stats globales et de résolution cross-tenant sont strictement réservées aux SUPER_ADMIN. Les opérations CRUD existantes restent accessibles aux TENANT_ADMIN.

---

### Décision 4 — Aucun breaking path

**Choix** : Conserver les paths existants ; les endpoints `/overview` et listing sont **ajoutés** aux controllers existants.

**Rationale** : Les controllers existants ont déjà leurs paths en production (`/platform/i18n-overrides`, `/platform/settings`, `/platform/theme-presets`). Les slices features étaient des doublons non déployés — pas de clients à migrer.

## Risks / Trade-offs

| Risque                                                                                                 | Mitigation                                                                                                                            |
| ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| `PlatformSettingsController` n'expose que `stats()` en overview — fonctionnalités stats limitées       | Acceptable ; opérations supplémentaires dans une change dédiée                                                                        |
| `ThemeAdminController` stub overview si `ThemeCatalog.stats()` non implémenté                          | Vérifier l'implémentation avant enrichissement; si absent, retourner `ThemePresetStatsView` depuis `ThemeCatalog.listActive().size()` |
| Sécurité i18n : méthodes overview/resolve SUPER_ADMIN dans un controller `TENANT_ADMIN or SUPER_ADMIN` | Géré par annotation `@PreAuthorize` au niveau méthode — prioritaire sur la classe                                                     |

## Migration Plan

1. Enrichir `PlatformI18nOverridesController` (add `/overview`, `/resolve` cross-tenant)
2. Enrichir `PlatformSettingsController` (add `SettingsCatalog` + `/overview`)
3. Enrichir `ThemeAdminController` (add `ThemeCatalog` + `/overview`, `GET /`)
4. Supprimer les 3 dossiers `features/platformadmin/<slice>/`
5. Créer `FEATURE_PLATFORM_ADMIN.md`
6. Build + tests (`./mvnw verify`)

**Rollback** : revert du PR (pas de migration Flyway impliquée).

## Open Questions

- _(aucune)_ — tous les catalog API nécessaires sont déjà exposés via `I18nOverridesCatalog`, `SettingsCatalog`, `ThemeCatalog`.
