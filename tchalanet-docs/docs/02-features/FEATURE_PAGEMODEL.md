\*\*# Feature : Page Model

> Analyse du code existant — 2026-04-24. Aucune modification n'a été faite.

---

## Rôle métier

Le **Page Model** est un système de CMS headless intégré à Tchalanet.

Il permet de définir la **structure complète d'une page** (layout, navigation shell, widgets, thème, langues) sous forme d'un document JSON stocké en base de données. Chaque opérateur de borlette (tenant) peut ainsi avoir une page d'accueil publique et des dashboards personnalisés **sans modification de code**.

Concrètement :

- La page d'accueil `tchalanet.ht` est pilotée par le pagemodel `public.home`
- Le dashboard du caissier POS est piloté par `private.dashboard.cashier`
- Un opérateur abonné au plan Premium peut avoir un thème et une nav personnalisés

**5 pages pilotées par pagemodel** (logicalIds actifs) :

| `logicalId`                      | Scope   | Consommé par          |
| -------------------------------- | ------- | --------------------- |
| `public.home`                    | public  | Visiteur non connecté |
| `private.dashboard.superadmin`   | private | SUPER_ADMIN           |
| `private.dashboard.tenant_admin` | private | TENANT_ADMIN          |
| `private.dashboard.operator`     | private | Opérateur             |
| `private.dashboard.cashier`      | private | Caissier POS          |

---

## Comment ça marche (flow simplifié)

```
1. Angular app bootstrap — deux chargements au démarrage
   → MergedTranslateLoader.getTranslation(lang)
       a) GET /assets/i18n/{lang}.json           ← bundle statique ✅
       b) GET /v1/i18n-overrides?lang={lang}     ← overrides GLOBAL+TENANT ⚠️ ENDPOINT ABSENT
          (si backendPath configuré — fusionne b sur a, tenant override gagne)
   → PageApi.getPage()
      → GET /api/v1/public/pagemodel/{logicalId}?lang=fr
      ⚠ Route effective = features/pagemodelruntime/PageModelController
        (3 controllers déclarent la même route — voir § Incomplet #1)

2. Backend : ResolveEffectivePageModelQuery (3-level fallback)
   ├── A. PageModel PUBLISHED pour ce tenant (customisé)
   ├── B. PageModel PUBLISHED pour le tenant "default" (plateforme)
   └── C. JSON embarqué dans resources/ (fallback garanti)

3. Résolution de langue (LangResolver)
   → currentLang résolu par priorité (URL → préférence user → défaut tenant → meta → fallback)
   → langs = liste des langues proposées par ce tenant

4. Dynamic payload
   Pour chaque widget avec binding.mode = "dynamic" uniquement
   (binding.mode = "static" → données déjà dans le JSON, aucun appel supplémentaire)
   → PageModelDynamicResolver (features/pagemodel/) cherche le provider par source key
      ⚠ Pont inter-packages : PageModelRuntimeService (pagemodelruntime/) appelle
        PageModelDynamicResolver (features/pagemodel/) via conversion toShared() de 150 lignes
   → provider.load() appelle les services métier (news, KPIs, tirages…)
   → result injecté dans PageDynamicPayload.widgets

5. Réponse au client
   { currentLang, langs, pageModel: PageModelDoc, dynamic: { widgets, errors } }
   ⚠ Le pagemodel NE porte PAS de i18nOverrides — les surcharges tenant passent par /v1/i18n-overrides (catalog/i18n/)

6. Angular rendu
   → I18nEffects.initFromPage$ → translate.use(currentLang) → retrigger MergedTranslateLoader
   → GridLayoutComponent itère sur rows → columns → widgets
   → WidgetRendererComponent instancie le composant Angular par nom (type)
      → props passées au composant (clés i18n — ex: "hero.welcome.title")
      → | translate résout la clé : assets bundle + overrides GLOBAL + overrides TENANT
   → Theme appliqué via ThemeService
```

---

## Architecture technique

### Composants backend

Le système a **3 implémentations coexistantes** (migration incomplète).

#### A — `core/pagemodel/` — Nouvelle implémentation hexagonale (cible)

| Composant                                     | Rôle                                                                    |
| --------------------------------------------- | ----------------------------------------------------------------------- |
| `PageModelInstance`                           | Aggregate domain : DRAFT → PUBLISHED → ARCHIVED → (deleted)             |
| `PageModelDoc`                                | Record Java structurant le JSON de pagemodel (meta/theme/shell/content) |
| `PageModelType`                               | Enum des 5 logicalIds connus                                            |
| `UpsertPageModelHandler`                      | Commande upsert (create draft ou update)                                |
| `PublishPageModelHandler`                     | Commande publish (DRAFT → PUBLISHED)                                    |
| `ResolveEffectivePageModelHandler`            | Query : résout la version effective avec fallback 3 niveaux             |
| `PageModelReadPort` / `PageModelWritePort`    | Ports de sortie                                                         |
| `PageModelPersistenceAdapter`                 | Adaptateur JPA implémentant les ports                                   |
| `PageModelJpaEntity`                          | Entité JPA → table `page_model` (extend `BaseTenantEntity`)             |
| `PageModelAdminController` _(core/infra/web)_ | CRUD admin via CommandBus/QueryBus, `ApiResponse<T>`                    |
| `ClasspathPageModelTemplateLoader`            | Charge les JSON depuis `resources/pagemodel/*.json`                     |
| `PageModelBootstrapService`                   | Seed des pagemodels par défaut à l'onboarding d'un tenant               |
| `PageModelStartupInitializer`                 | Runner Spring qui appelle `seedDefaultsForTenant()`                     |

#### B — `features/pagemodel/` — Implémentation intermédiaire (en cours de remplacement)

| Composant                                               | Rôle                                                                           |
| ------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `PageModel`                                             | Record Java (structure identique à `PageModelDoc` mais dans features/)         |
| `PageModelService`                                      | Service avec 3-level fallback sur `PageModelRepository` (old)                  |
| `PageModelOrchestrator`                                 | Orchestre résolution selon scope (public / tenant / platform)                  |
| `PageModelController`                                   | Controller public + tenant + platform (sans `/api/v1` préfixe)                 |
| `PageModelDynamicResolver`                              | Dispatche vers les providers par `binding.source`                              |
| `PageModelDynamicProvider`                              | Interface : `supports()` + `load()`                                            |
| `PublicNewsProvider`                                    | Provider `source: "news"` → liste les news publiques                           |
| `CashierOverviewProvider`                               | Provider `source: "overview"` → KPIs caissier (stub en dur)                    |
| `PageModelAdminController` _(features/pagemodel/admin)_ | CRUD admin via `PageModelAdminService` (ResponseEntity)                        |
| `LangResolver`                                          | Résolution de langue par priorité (URL → user pref → tenant → meta → fallback) |
| `PageModelRepository`                                   | `@RepositoryRestResource` (VIOLATION) → JpaRepository direct                   |
| `PageModelEntity`                                       | Entité JPA propre à features/ (doublon de `PageModelJpaEntity`)                |

#### C — `features/pagemodel_backup/` — Implémentation legacy (à supprimer)

| Composant                   | Rôle actuel                                                                        |
| --------------------------- | ---------------------------------------------------------------------------------- |
| `PublicPageModelController` | Sert `GET /api/v1/public/pagemodel/{logicalId}` (même route que pagemodelruntime!) |
| `PageModelRuntimeService`   | Appelle `PageModelService` + `LangResolver` + i18n overrides                       |
| `Block*` classes            | Types de blocs (Hero, News, Plans, ResultsByGame, Tchala, Features) — héritage     |
| `*Provider` classes         | Fournisseurs dynamiques legacy                                                     |

#### D — `features/pagemodelruntime/` — Bridge (cible proche)

| Composant                  | Rôle                                                                                          |
| -------------------------- | --------------------------------------------------------------------------------------------- |
| `PageModelController`      | `GET /api/v1/public/pagemodel/{logicalId}` — utilise la nouvelle implémentation               |
| `PageModelRuntimeService`  | Appelle `ResolveEffectivePageModelHandler` (nouveau) + `PageModelDynamicResolver` (features/) |
| `PageModelRuntimeResponse` | Response record avec `pageModel: PageModelDoc` (nouveau type)                                 |

#### E — `catalog/pagemodeltemplate/` — Catalogue des templates

| Composant                             | Rôle                                                                       |
| ------------------------------------- | -------------------------------------------------------------------------- |
| `PageModelTemplateEntity`             | Entité (table `page_model_template`) — extends `BaseEntity` (non-tenantée) |
| `PageModelTemplateCatalog`            | Interface API publique du catalog                                          |
| `PageModelTemplateLevel`              | Enum : `GLOBAL` (défaut plateforme) / `TENANT` (surcharge opérateur)       |
| `PageModelTemplateSeedRunner`         | Seed des templates au démarrage depuis `resources/pagemodel/*.json`        |
| `PlatformPageModelTemplateController` | CRUD `SUPER_ADMIN` uniquement (`@PreAuthorize`)                            |
| `PageModelTemplateAdminService`       | Logique CRUD catalog                                                       |

---

### Composants frontend

| Composant                 | Localisation                   | Rôle                                                                      |
| ------------------------- | ------------------------------ | ------------------------------------------------------------------------- |
| `PageEffects`             | `libs/shared/data-access/page` | Charge le pagemodel via `PageApi.getPage()`, applique thème + i18n        |
| `PageApi`                 | `libs/shared/api`              | `getPage(context, tenantId)` → `GET /api/v1/public/pagemodel/{logicalId}` |
| `GridLayoutComponent`     | `libs/ui/layout`               | Rend `rows → columns → widgets` en CSS Grid 12 colonnes                   |
| `WidgetRendererComponent` | `libs/ui/widget-renderer`      | Lookup dynamique dans `TCH_WIDGET_REGISTRY` + instantiation               |
| `TCH_WIDGET_REGISTRY`     | `libs/web/widgets`             | Multi-provider Angular : map `nom → factory`                              |
| `provideBuiltinWidgets()` | `libs/web/widgets`             | Enregistre les 7 widgets built-in                                         |

**Widgets Angular enregistrés** (via `provideBuiltinWidgets()`) :

| Nom (type JSON)      | Composant                     | Binding attendu        |
| -------------------- | ----------------------------- | ---------------------- |
| `HeroWidget`         | `HeroWidgetComponent`         | static ou dynamic      |
| `NewsBannerWidget`   | `NewsBannerWidget`            | dynamic (source: news) |
| `DrawSwitcherWidget` | `DrawSwitcherWidget`          | dynamic                |
| `FeatureCardWidget`  | `FeatureCardWidgetComponent`  | dynamic                |
| `KpiWidget`          | `KpiWidgetComponent` (lazy)   | dynamic                |
| `InfoWidget`         | `InfoWidgetComponent` (lazy)  | dynamic                |
| `QuickActionsWidget` | `QuickActionsWidgetComponent` | static                 |

**Widgets référencés dans les JSON templates mais SANS composant Angular** :

| Nom JSON                | Manquant            |
| ----------------------- | ------------------- |
| `DrawsWidget`           | ❌ pas de composant |
| `CheckTicketWidget`     | ❌ pas de composant |
| `FeatureGridWidget`     | ❌ pas de composant |
| `NewsListWidget`        | ❌ pas de composant |
| `TchalaWidget`          | ❌ pas de composant |
| `TestimonialsWidget`    | ❌ pas de composant |
| `PlansWidget`           | ❌ pas de composant |
| `GenericInfoWidget`     | ❌ pas de composant |
| `LotteryCarouselWidget` | ❌ pas de composant |

---

### Flux de données

```
CRÉATION / MISE À JOUR (admin)
  SUPER_ADMIN
  → PUT /platform/page-model-templates/{id}       ← catalog
    → PageModelTemplateAdminService.updateFromView()
    → PageModelTemplateSeedRunner (startup seed)

  Platform admin
  → POST /admin/pagemodels                          ← core (nouvelle implémentation)
    → CommandBus → UpsertPageModelHandler
    → PageModelPersistenceAdapter.save()
    → table page_model (status: DRAFT)

  → POST /admin/pagemodels/{id}/publish
    → CommandBus → PublishPageModelHandler
    → status: PUBLISHED

ONBOARDING TENANT
  → PageModelStartupInitializer
    → PageModelBootstrapService.seedDefaultsForTenant()
      → pour chaque PageModelType (5 types)
         → PageModelTemplateCatalog.findByLogicalId()
         → PageModelService.createFromTemplate()  ⚠ bypass hexagonal
         → table page_model (status: PUBLISHED)

RÉSOLUTION À L'USAGE (runtime)
  GET /api/v1/public/pagemodel/public.home?lang=fr
  → PageModelRuntimeService (pagemodelruntime)
    → QueryBus → ResolveEffectivePageModelHandler
      → PageModelReadPort.findPublishedByLogicalId(logicalId)
        1. tenant courant → DB via RLS
        2. DEFAULT_TENANT_UUID → DB
        3. ClasspathPageModelTemplateLoader → resources/pagemodel/public.home.json
    → PageModelDynamicResolver.resolve(doc, lang, ctx)
      → pour chaque widget binding.mode == "dynamic"
        → providers.stream().filter(p.supports(...)).findFirst()
        → provider.load() → donnée temps-réel
    → PageModelRuntimeResponse { currentLang, langs, pageModel, dynamic }
```

---

## Entités et relations

```
page_model_template (catalog — non tenantée)
  id, code, logical_id, name, label, description
  schema (jsonb), model (jsonb), schema_version
  is_default, level (GLOBAL | TENANT), tenant_id (null si GLOBAL)

page_model (core — tenantée via BaseTenantEntity)
  id, tenant_id, code, logical_id, name
  schema (jsonb), model (jsonb)     ← le document PageModelDoc complet
  scope, slug, schema_version
  status (DRAFT | PUBLISHED | ARCHIVED)
  template_id → page_model_template.id
  published_at, created_at, updated_at, deleted_at
  created_by, updated_by
```

**Relations** :

- Un `page_model_template` → N `page_model` (un par tenant onboardé)
- Un `page_model` appartient à un tenant (RLS via `BaseTenantEntity`)
- Le tenant DEFAULT_TENANT_UUID porte les pagemodels de la plateforme

**RLS** : la table `page_model` est tenantée mais **aucune policy RLS n'a été identifiée** dans les migrations Flyway existantes pour cette table — les migrations V40 et V41 couvrent d'autres tables. À confirmer via `grep -r "page_model" tchalanet-server/src/main/resources/db/migration/`.

---

## Endpoints exposés

| Méthode               | Route                                         | Scope                  | Implémentation                                                                          |
| --------------------- | --------------------------------------------- | ---------------------- | --------------------------------------------------------------------------------------- |
| `GET`                 | `/api/v1/public/pagemodel/{logicalId}`        | Public non authentifié | **3 controllers en conflit** (backup, features/pagemodel non préfixé, pagemodelruntime) |
| `GET`                 | `/public/pagemodel/{logicalId}`               | Public                 | features/pagemodel `PageModelController`                                                |
| `GET`                 | `/tenant/pagemodel/{logicalId}`               | Authentifié (tenant)   | features/pagemodel `PageModelController`                                                |
| `GET`                 | `/tenant/pagemodel/dashboard`                 | Authentifié (tenant)   | features/pagemodel `PageModelController`                                                |
| `GET`                 | `/platform/pagemodel/{logicalId}`             | SUPER_ADMIN            | features/pagemodel `PageModelController`                                                |
| `GET`                 | `/platform/pagemodel/dashboard`               | SUPER_ADMIN            | features/pagemodel `PageModelController`                                                |
| `GET/POST/PUT/DELETE` | `/admin/pagemodels`                           | Admin plateforme       | **2 controllers** (core + features/admin)                                               |
| `POST`                | `/admin/pagemodels/{id}/publish`              | Admin plateforme       | core `PageModelAdminController`                                                         |
| `GET`                 | `/admin/pagemodels/{id}/duplicate`            | Admin plateforme       | features `PageModelAdminController`                                                     |
| `GET`                 | `/platform/page-model-templates`              | SUPER_ADMIN            | `PlatformPageModelTemplateController`                                                   |
| `GET/POST/PUT/DELETE` | `/platform/page-model-templates/{id}`         | SUPER_ADMIN            | `PlatformPageModelTemplateController`                                                   |
| `POST`                | `/platform/page-model-templates/{id}/default` | SUPER_ADMIN            | `PlatformPageModelTemplateController`                                                   |

---

## État actuel

### Ce qui fonctionne

- **Catalog `pagemodeltemplate/`** : complet, clean, `PlatformPageModelTemplateController` avec `ApiResponse<T>` et `@PreAuthorize("SUPER_ADMIN")`
- **3-level fallback** : logique de résolution tenant → default → classpath implémentée dans les deux générations
- **`LangResolver`** : résolution de langue avec priorité complète (URL → user pref → tenant → meta → fallback)
- **`PageModelDynamicProvider`** : pattern extensible, les providers sont auto-découverts via Spring
- **Classpath templates** : 4 fichiers JSON complets dans `resources/pagemodel/` (public.home, 3 dashboards)
- **`GridLayoutComponent`** : rendu grid 12 colonnes, mobile responsive
- **`WidgetRendererComponent`** : lookup dynamique dans `TCH_WIDGET_REGISTRY`, affiche un message d'erreur gracieux si le composant est absent
- **`PageEffects`** + store NgRx : load, fallback sur `page-default.json`, application du thème et de l'i18n
- **`PageModelInstance`** domain (nouvelle implémentation) : cycle de vie DRAFT → PUBLISHED → ARCHIVED avec protections (`applyUpsert` refuse les deleted/archived)

### Ce qui est incomplet

1. **Route `/api/v1/public/pagemodel/{logicalId}` en conflit** : 3 classes déclarent le même `@RequestMapping`. Spring ne peut pas avoir deux beans sur la même route — l'une d'elles sera ignorée ou la startup échouera. La route effective est imprévisible.

2. **`PageModelAdminController` dupliqué** : deux controllers sur `/admin/pagemodels` — l'un (core/) utilise le CommandBus et `ApiResponse`, l'autre (features/admin/) utilise `PageModelAdminService` et `ResponseEntity`. Comportement de démarrage Spring indéterminé.

3. **Widgets Angular manquants** : 9 types de widgets référencés dans les JSON templates n'ont pas de composant Angular. `WidgetRendererComponent` affiche une boîte d'erreur à la place.

4. **`CashierOverviewProvider`** : retourne des données hardcodées (`ticketsToday: 0`, `sessionOpen: true`). Les vrais KPIs ne sont pas branchés.

5. **Providers manquants** : `public.home` déclare des widgets avec `source: "hero"`, `"results_by_game"`, `"features"`, `"tchala"`, `"testimonials"`, `"plans"` — aucun provider backend ne les implémente. Les widgets correspondants recevront une erreur `NO_PROVIDER`.

6. **`PageModelPersistenceAdapter.list()`** : `return repo.findAll()` sans aucun filtre — renvoie tous les pagemodels de tous les tenants.

7. **`PageModelBootstrapService`** : service dans `core/infra/init/` qui importe `PageModelRepository` et `PageModelService` depuis `features/pagemodel` — violation de la règle "core/ ne dépend pas de features/".

8. **RLS sur `page_model`** : la table étend `BaseTenantEntity` mais aucune policy RLS n'a été identifiée dans les migrations Flyway existantes pour cette table. Si RLS n'est pas configuré, n'importe quel tenant peut lire les pagemodels d'un autre.

9. **`PageModelDynamicResolver.toShared()`** : méthode de conversion manuelle de `core.PageModelDoc` → `features.PageModel` (150 lignes de mapping champ par champ). Résultat d'avoir deux records Java identiques dans deux packages.

### Dette technique

| Dette                                                | Criticité    | Impact                                                |
| ---------------------------------------------------- | ------------ | ----------------------------------------------------- |
| 3 controllers sur la même route HTTP                 | 🔴 BLOQUANT  | La page publique est imprévisible en prod             |
| 2 `PageModelAdminController`                         | 🔴 BLOQUANT  | Conflict Spring au démarrage potentiel                |
| `PageModelBootstrapService` → features/              | 🟠 IMPORTANT | Violation hexagonale : core/ dépend de features/      |
| `@RepositoryRestResource` sur `PageModelRepository`  | 🔴 BLOQUANT  | Expose les entités JPA brutes (convention violée)     |
| `features.PageModel` ≡ `core.PageModelDoc`           | 🟠 IMPORTANT | 2 types identiques → code bridge de 150 lignes        |
| 9 widgets Angular sans composant                     | 🟠 IMPORTANT | Page d'accueil publique partiellement affichée        |
| Providers dynamiques manquants (hero, draws, plans…) | 🟠 IMPORTANT | Home page : boîte d'erreur pour la moitié des widgets |
| RLS `page_model` non vérifié                         | 🟠 IMPORTANT | Potentielle fuite de données cross-tenant             |
| `features/pagemodel_backup/` actif                   | 🟠 IMPORTANT | Dead code mais activement enregistré dans Spring      |
| `CashierOverviewProvider` stub                       | 🟡 MINEUR    | Dashboard caissier affiche des zéros                  |

---

## Questions ouvertes

1. **Qui peut customiser un pagemodel en prod ?** Le code actuel ne définit pas clairement si `TENANT_ADMIN` peut modifier son propre pagemodel ou si c'est réservé à la plateforme. `PageModelOrchestrator.resolveTenant()` charge la version effective mais aucun endpoint `TENANT_ADMIN` ne permet de l'éditer.

2. **La table `page_model` a-t-elle une policy RLS dans les migrations Flyway ?** Confirmer via `grep -r "page_model" src/main/resources/db/migration/` avant toute modification multi-tenant.

3. **Quelle est la "vraie" route public ?** Les 3 controllers en conflit doivent être résolus — lequel prend la main n'est pas déterminable par analyse statique. La route effective est `features/pagemodelruntime/PageModelController` selon ce que le frontend appelle (`GET /api/v1/public/pagemodel/{logicalId}`), mais cela reste à confirmer par boot test.

4. **`ListPageModelsQueryHandler` — présent ou absent ?** `ListPageModelsQuery` est déclaré dans `core/pagemodel/application/query/model/` mais aucun handler correspondant n'a été identifié dans l'analyse. Gap ou fichier non trouvé ?

5. **Les JSON templates dans `resources/pagemodel/`** sont-ils utilisés par le `PageModelTemplateSeedRunner` ou directement comme fallback ? Les deux chemins semblent actifs mais les rôles ne sont pas formellement séparés.\*\*
