# PageModel — Architecture cible

> Analyse du 2026-04-24. Basé sur la lecture du code source + FEATURE_PAGEMODEL.md.
> Ce document définit **où chaque composant doit vivre**, avant toute implémentation.
> Valider avec l'équipe avant OpenSpec #90.

---

## Ce que pagemodel pilote (périmètre exact)

### Pages pilotées par pagemodel (logicalIds actifs)

| `logicalId`                      | Scope   | Page Angular               | Route                         | Statut                                   |
| -------------------------------- | ------- | -------------------------- | ----------------------------- | ---------------------------------------- |
| `public.home`                    | public  | `HomePublicPage`           | `/`                           | ✅ Piloté + JSON OK                      |
| `private.dashboard.superadmin`   | private | `SuperAdminDashboardPage`  | `/app/dashboard/super-admin`  | ✅ Piloté + JSON OK                      |
| `private.dashboard.tenant_admin` | private | `TenantAdminDashboardPage` | `/app/dashboard/tenant-admin` | ✅ Piloté + JSON OK                      |
| `private.dashboard.operator`     | private | _(aucune)_                 | _(non déclarée)_              | ⚠️ JSON présent, route Angular manquante |
| `private.dashboard.cashier`      | private | `CashierDashboardPage`     | `/app/dashboard/cashier`      | ✅ Piloté + JSON OK                      |

**Observation critique** : `private.dashboard.operator` est défini dans `PageModelType` (core) et dans les resources JSON, mais aucun composant Angular ni route `/app/dashboard/operator` n'existe. Le rôle `OPERATOR` n'a pas de dashboard propre — à clarifier avec le product.

### Pages Angular NON pilotées par pagemodel

Ces pages ont leur propre structure Angular et n'appellent pas `PageApi.getPage()`.

| Route                                          | Composant                 | Raison                            |
| ---------------------------------------------- | ------------------------- | --------------------------------- |
| `/pricing`                                     | `PlansPage`               | Page statique autonome            |
| `/features`                                    | `FeaturesPage`            | Page statique autonome            |
| `/verify`                                      | `PublicStubPageComponent` | Stub — intégration ticket à venir |
| `/ticket/:code`                                | `PublicStubPageComponent` | Lookup ticket individuel          |
| `/explanations`, `/official-reports`, etc.     | `MarkdownPageComponent`   | Contenu markdown statique         |
| `/support`, `/security`, `/tchala`, `/legal/*` | `MarkdownPageComponent`   | Contenu markdown statique         |
| `/app/tickets`                                 | `TicketsPage`             | Interface de vente POS            |
| `/app/tirages`                                 | `DrawsPage`               | Gestion des tirages               |
| `/app/rapports`                                | `ReportsPage`             | Reporting                         |
| `/app/gestion`                                 | `AdminPage`               | Administration tenant             |
| `/app/profile`                                 | `ProfilePage`             | Profil utilisateur                |

Le pagemodel pilote donc **4 pages actives + 1 à définir (operator)**, pas l'ensemble de l'application.

---

## Ce qui va dans catalog/

### Critères catalog/

- Données de référence, read-mostly, pas de side-effects
- Pas de logique métier, pas de domain events
- Jamais de dépendance vers `core/` ou `features/`
- Cache obligatoire sur les lectures

### Analyse des candidats

#### `catalog/pagemodeltemplate/` — déjà dans catalog ✅ CORRECT

La structure est déjà canonique :

```
api/PageModelTemplateCatalog      ← interface read-only ✅
api/model/PageModelTemplateView   ← projection complète ✅
api/model/PageModelTemplateLevel  ← enum GLOBAL / TENANT ✅
api/model/PageModelTemplateStatsView ← projection stats ✅
internal/read/PageModelTemplateCatalogImpl ✅
internal/write/PageModelTemplateAdminService ✅
internal/mapper/PageModelTemplateMapper ✅
internal/persistence/PageModelTemplateEntity ✅
internal/cache/PageModelTemplateCacheNames ✅
internal/web/PlatformPageModelTemplateController ✅
internal/init/PageModelTemplateSeedRunner ← ⚠️ import à corriger
```

**Seule violation à corriger** : `PageModelTemplateSeedRunner` importe `PageModelType` depuis `features/pagemodel/PageModelType` au lieu de `core/pagemodel/domain/model/PageModelType`. Un import catalog → features est interdit. Correction : changer l'import.

#### Les JSON dans `resources/pagemodel/` — PAS UN CATALOG ❌

Ces fichiers jouent deux rôles distincts :

1. **Seed du catalog** : `PageModelTemplateSeedRunner` les lit au démarrage et upserte `page_model_template`
2. **Fallback ultime** : `ClasspathPageModelTemplateLoader` (dans `core/`) les charge si aucun template n'est trouvé en base

Ces fichiers **restent** dans `src/main/resources/pagemodel/` — c'est le bon emplacement pour des ressources classpath. Ils ne constituent pas un catalog Java : ils sont les données sources qui alimentent le catalog.

#### Types de widgets (`DrawsWidget`, `HeroWidget`, etc.) — PAS CATALOG ❌

Les types de widgets sont une préoccupation **frontend uniquement**. Le backend stocke la clé de type (`type: "DrawsWidget"`) dans le JSON du pagemodel, mais la résolution du composant Angular est faite via `TCH_WIDGET_REGISTRY`. Il n'y a rien à cataloguer côté Java.

#### `PageModelType` (logicalIds enum) — CORE ✅ (pas catalog)

L'enum `PageModelType` appartient à `core/pagemodel/domain/model/` car :

- C'est un concept du domaine métier (les 5 identités de page que le système connaît)
- Il est utilisé par les handlers et la logique de seed de core/
- `catalog/` peut importer depuis `core/` via les interfaces publiques

> **Règle** : `catalog/` n'importe pas directement `PageModelType` — il itère sur les logicalIds fournis au démarrage, sans couplage à l'enum.

---

## Ce qui va dans core/

### Critères core/

- Cycle de vie avec invariants métier
- Domain events
- Identité forte (TypedId)
- Framework-free au niveau domaine

### Analyse des candidats

#### `PageModelInstance` — CORE ✅

Aggregate avec cycle de vie complet :

```
DRAFT → PUBLISHED → ARCHIVED → (deleted: soft-delete)
```

Invariants enforced : `applyUpsert()` refuse les instances deleted/archived. `markPublished()` vérifie l'état. Domain exceptions levées.

**Amélioration requise** : introduire `PageModelId` (TypedId) pour remplacer l'UUID brut dans le domain.

#### `UpsertPageModelHandler` / `PublishPageModelHandler` — CORE ✅

Handlers CQRS canoniques : record command + `@TchTx` + délégation aux ports. Restent dans `core/pagemodel/application/command/handler/`.

#### `ResolveEffectivePageModelHandler` — CORE ✅

Query handler (lecture seule, pas de side-effects) qui implémente la logique de fallback 3 niveaux. Dépend uniquement de `common/` (TchContextRunner, DEFAULT_TENANT_UUID) et de ses propres ports — le graphe de dépendances est respecté.

#### `PageModelBootstrapService` (seed tenant) — PAS CORE ❌

Actuellement dans `core/infra/init/` mais importe `PageModelRepository` et `PageModelService` depuis `features/pagemodel/` — **violation hexagonale grave** (core/ dépend de features/).

Cette logique est de l'orchestration : elle récupère un template depuis catalog/ et crée une instance via core/. Elle appartient dans `features/pagemodel/onboarding/`.

**Ce que core/ peut garder** : le `PageModelStartupInitializer` (Runner Spring) pourrait appeler un port d'onboarding, ou simplement être déplacé avec le service.

---

## Ce qui va dans features/

### Critères features/

- Orchestre core/ + catalog/ sans contenir d'invariants métier
- Compose la réponse pour un endpoint HTTP précis (BFF)
- Résout le contexte (scope, langue, tenant)
- Charge les données dynamiques via providers

### Analyse des candidats

#### `PageModelRuntimeService` (features/pagemodelruntime) — FEATURES ✅

Orchestre correctement : QueryBus → `ResolveEffectivePageModelHandler` + `LangResolver` + `PageModelDynamicResolver`. C'est le pattern idéal. À déplacer dans `features/pagemodel/public/app/` et renommer `PublicPageModelService`.

#### `PageModelDynamicResolver` + providers — FEATURES ✅

Le dispatch par `binding.source` est de l'orchestration runtime (pas d'invariants métier). Les providers appellent des services cross-domaines (news, draws, plans). Appartient à `features/pagemodel/dynamic/`.

**Problème à corriger** : la méthode `toShared(PageModelDoc → features.PageModel)` de 150 lignes n'existera plus une fois `features.PageModel` supprimé — les providers utiliseront directement `core.PageModelDoc`.

#### `LangResolver` — FEATURES ✅

Algorithme de résolution de langue basé sur le contexte de requête (URL, préférence user, défaut tenant, meta). Orchestre des données de contexte HTTP — aucun invariant métier. Reste dans `features/pagemodel/shared/`.

#### `PageModelOrchestrator` — FEATURES (à refactorer)

La logique de dispatch par scope (public/tenant/platform) est correcte pour features/. Mais son implémentation actuelle utilise l'ancien `PageModelService` (features/) au lieu du QueryBus. À remplacer par deux services de slice : `PublicPageModelService` et `DashboardPageModelService`.

#### `PageModelController` (features/pagemodel) — FEATURES ✅ (routes valides, impl à corriger)

Les routes définies (`/public/pagemodel/*`, `/tenant/pagemodel/*`, `/platform/pagemodel/*`) sont correctes et couvrent tous les scopes. Ce controller doit être conservé mais déléguer aux nouveaux slice services au lieu de `PageModelOrchestrator`.

---

## Architecture cible complète

### `catalog/pagemodeltemplate/` — inchangé

```
catalog/pagemodeltemplate/
├── api/
│   ├── PageModelTemplateCatalog.java          ← GARDER
│   └── model/
│       ├── PageModelTemplateView.java          ← GARDER
│       ├── PageModelTemplateLevel.java         ← GARDER
│       └── PageModelTemplateStatsView.java     ← GARDER
└── internal/
    ├── read/PageModelTemplateCatalogImpl.java  ← GARDER
    ├── write/PageModelTemplateAdminService.java ← GARDER
    ├── mapper/PageModelTemplateMapper.java     ← GARDER
    ├── persistence/PageModelTemplateEntity.java ← GARDER
    ├── persistence/PageModelTemplateRepository.java ← GARDER
    ├── cache/PageModelTemplateCacheNames.java  ← GARDER
    ├── web/PlatformPageModelTemplateController.java ← GARDER
    └── init/PageModelTemplateSeedRunner.java   ← GARDER + fix import
```

### `core/pagemodel/` — corrections mineures

```
core/pagemodel/
├── domain/
│   ├── model/
│   │   ├── PageModelInstance.java              ← GARDER + ajouter PageModelId TypedId
│   │   ├── PageModelDoc.java                   ← GARDER (type canonical partagé)
│   │   ├── PageModelStatus.java                ← GARDER
│   │   └── PageModelType.java                  ← GARDER (authoritative enum)
│   ├── policy/
│   │   └── PublishPolicy.java                  ← GARDER
│   └── exception/
│       ├── PageModelNotEditableException.java  ← GARDER
│       └── PageModelStateException.java        ← GARDER
├── application/
│   ├── command/
│   │   ├── model/UpsertPageModelCommand.java   ← GARDER
│   │   ├── model/PublishPageModelCommand.java  ← GARDER
│   │   ├── handler/UpsertPageModelHandler.java ← GARDER
│   │   └── handler/PublishPageModelHandler.java ← GARDER
│   ├── query/
│   │   ├── model/ResolveEffectivePageModelQuery.java ← GARDER
│   │   ├── model/ListPageModelsQuery.java      ← GARDER
│   │   └── handler/ResolveEffectivePageModelHandler.java ← GARDER
│   └── port/
│       ├── PageModelReadPort.java              ← SUPPRIMER (doublon de port/out/)
│       ├── PageModelWritePort.java             ← SUPPRIMER (doublon de port/out/)
│       └── out/
│           ├── PageModelReadPort.java          ← GARDER (canonique)
│           ├── PageModelWritePort.java         ← GARDER (canonique)
│           └── PageModelTemplateLoaderPort.java ← GARDER
├── infra/
│   ├── persistence/
│   │   ├── PageModelJpaEntity.java             ← GARDER
│   │   ├── PageModelJpaRepository.java         ← GARDER
│   │   ├── PageModelMapper.java                ← GARDER
│   │   └── PageModelPersistenceAdapter.java    ← GARDER + fix list() (findAll → filtre RLS)
│   ├── resources/
│   │   └── ClasspathPageModelTemplateLoader.java ← GARDER
│   ├── web/
│   │   └── PageModelAdminController.java       ← GARDER (seul controller admin valide)
│   └── init/
│       ├── PageModelBootstrapService.java      ← DÉPLACER → features/pagemodel/onboarding/
│       └── PageModelStartupInitializer.java    ← DÉPLACER → features/pagemodel/onboarding/
```

### `features/pagemodel/` — restructuration par slice

```
features/pagemodel/
│
├── public/                         ← slice : page publique (non authentifiée)
│   ├── web/
│   │   └── PublicPageModelController.java      ← CRÉER (remplace pagemodelruntime/PageModelController)
│   │       @GetMapping("/public/pagemodel/{logicalId}")
│   ├── app/
│   │   └── PublicPageModelService.java         ← CRÉER (= pagemodelruntime/PageModelRuntimeService)
│   └── model/
│       └── PublicPageModelResponse.java        ← CRÉER (= PageModelRuntimeResponse)
│
├── dashboard/                      ← slice : dashboards privés (par rôle)
│   ├── web/
│   │   └── DashboardPageModelController.java   ← CRÉER (consolide /tenant/* + /platform/*)
│   │       @GetMapping("/tenant/pagemodel/{logicalId}")
│   │       @GetMapping("/tenant/pagemodel/dashboard")
│   │       @GetMapping("/platform/pagemodel/{logicalId}")
│   │       @GetMapping("/platform/pagemodel/dashboard")
│   ├── app/
│   │   ├── DashboardPageModelService.java      ← CRÉER
│   │   └── PageModelTypeResolver.java          ← DÉPLACER depuis features/pagemodel/
│   └── model/
│       └── DashboardPageModelResponse.java     ← CRÉER
│
├── onboarding/                     ← slice : seed des pagemodels au démarrage du tenant
│   └── app/
│       ├── PageModelOnboardingService.java     ← CRÉER (= PageModelBootstrapService refactoré)
│       │   // Orchestre : catalog.findByLogicalId() + CommandBus.dispatch(UpsertPageModelCommand)
│       │   // Plus de dépendance features/PageModelService ou features/PageModelRepository
│       └── PageModelOnboardingRunner.java      ← CRÉER (= PageModelStartupInitializer)
│
├── dynamic/                        ← extension : providers de données widgets
│   ├── PageModelDynamicProvider.java           ← GARDER (interface)
│   ├── PageModelDynamicResolver.java           ← GARDER + fix (supprimer toShared() → utilise PageModelDoc)
│   └── providers/
│       ├── PublicNewsProvider.java             ← GARDER
│       ├── CashierOverviewProvider.java        ← GARDER + fix (supprimer stub, brancher core/sales)
│       ├── HeroProvider.java                   ← CRÉER (source: "hero")
│       ├── DrawsProvider.java                  ← CRÉER (source: "draws" / "results_by_game")
│       ├── PlansProvider.java                  ← CRÉER (source: "plans")
│       ├── TchalaProvider.java                 ← CRÉER (source: "tchala")
│       ├── TestimonialsProvider.java           ← CRÉER (source: "testimonials")
│       └── FeaturesProvider.java               ← CRÉER (source: "features")
│
└── shared/                         ← helpers transverses à la feature pagemodel
    ├── LangResolver.java                       ← GARDER (déjà dans features/pagemodel/)
    ├── PageDynamicPayload.java                 ← GARDER
    └── WidgetDynamicError.java                 ← GARDER
```

### À supprimer entièrement

| Package / Fichier                                         | Raison                                          |
| --------------------------------------------------------- | ----------------------------------------------- |
| `features/pagemodel_backup/` (17 fichiers)                | Dead code, controllers en conflit Spring        |
| `features/pagemodelruntime/` (3 fichiers)                 | Migré dans `features/pagemodel/public/`         |
| `features/pagemodel/PageModelController.java`             | Remplacé par public/ + dashboard/ controllers   |
| `features/pagemodel/PageModelOrchestrator.java`           | Remplacé par slice services                     |
| `features/pagemodel/PageModelService.java`                | Remplacé par QueryBus                           |
| `features/pagemodel/PageModelRepository.java`             | `@RepositoryRestResource` interdit              |
| `features/pagemodel/PageModelEntity.java`                 | Doublon de `core/.../PageModelJpaEntity`        |
| `features/pagemodel/PageModel.java` (record)              | Doublon de `core/.../PageModelDoc`              |
| `features/pagemodel/PageModelResponse.java`               | Remplacé par slice responses                    |
| `features/pagemodel/PageModelType.java`                   | Doublon de `core/.../PageModelType`             |
| `features/pagemodel/PageStatus.java`                      | Doublon de `core/.../PageModelStatus`           |
| `features/pagemodel/admin/PageModelAdminController.java`  | Controller admin doit être dans core/infra/web/ |
| `features/pagemodel/admin/PageModelAdminService.java`     | Remplacé par CommandBus                         |
| `features/pagemodel/admin/dto/*.java`                     | Remplacés par core/infra/web/ modèles           |
| `core/pagemodel/application/port/PageModelReadPort.java`  | Doublon de `port/out/`                          |
| `core/pagemodel/application/port/PageModelWritePort.java` | Doublon de `port/out/`                          |

---

## Mapping composant par composant

| Composant actuel                                     | Action        | Destination                                                      |
| ---------------------------------------------------- | ------------- | ---------------------------------------------------------------- |
| `catalog/pagemodeltemplate/**`                       | **GARDER**    | Fix import PageModelType (features→core)                         |
| `PageModelTemplateSeedRunner`                        | **GARDER**    | Fix: `import core.pagemodel.domain.model.PageModelType`          |
| `core/.../PageModelInstance`                         | **GARDER**    | + Ajouter `PageModelId` TypedId                                  |
| `core/.../PageModelDoc`                              | **GARDER**    | Type canonical — tous les layers l'utilisent                     |
| `core/.../PageModelType`                             | **GARDER**    | Authoritative — fix catalog import                               |
| `core/.../UpsertPageModelHandler`                    | **GARDER**    | —                                                                |
| `core/.../PublishPageModelHandler`                   | **GARDER**    | —                                                                |
| `core/.../ResolveEffectivePageModelHandler`          | **GARDER**    | —                                                                |
| `core/.../application/port/PageModelReadPort`        | **SUPPRIMER** | Doublon de `port/out/`                                           |
| `core/.../application/port/PageModelWritePort`       | **SUPPRIMER** | Doublon de `port/out/`                                           |
| `core/.../port/out/PageModelReadPort`                | **GARDER**    | Canonique                                                        |
| `core/.../port/out/PageModelWritePort`               | **GARDER**    | Canonique                                                        |
| `core/.../port/out/PageModelTemplateLoaderPort`      | **GARDER**    | —                                                                |
| `core/.../PageModelPersistenceAdapter`               | **GARDER**    | Fix: `findAll()` → scope RLS                                     |
| `core/.../ClasspathPageModelTemplateLoader`          | **GARDER**    | —                                                                |
| `core/.../PageModelAdminController`                  | **GARDER**    | Seul admin controller valide                                     |
| `core/.../PageModelBootstrapService`                 | **DÉPLACER**  | → `features/pagemodel/onboarding/app/PageModelOnboardingService` |
| `core/.../PageModelStartupInitializer`               | **DÉPLACER**  | → `features/pagemodel/onboarding/app/PageModelOnboardingRunner`  |
| `features/pagemodelruntime/PageModelController`      | **SUPPRIMER** | Remplacé par `features/pagemodel/public/web/`                    |
| `features/pagemodelruntime/PageModelRuntimeService`  | **DÉPLACER**  | → `features/pagemodel/public/app/PublicPageModelService`         |
| `features/pagemodelruntime/PageModelRuntimeResponse` | **DÉPLACER**  | → `features/pagemodel/public/model/PublicPageModelResponse`      |
| `features/pagemodel/PageModelController`             | **SUPPRIMER** | Remplacé par public/ + dashboard/                                |
| `features/pagemodel/PageModelOrchestrator`           | **SUPPRIMER** | Remplacé par slice services                                      |
| `features/pagemodel/PageModelService`                | **SUPPRIMER** | Remplacé par QueryBus                                            |
| `features/pagemodel/PageModelRepository`             | **SUPPRIMER** | `@RepositoryRestResource` interdit                               |
| `features/pagemodel/PageModelEntity`                 | **SUPPRIMER** | Doublon                                                          |
| `features/pagemodel/PageModel`                       | **SUPPRIMER** | Doublon de `PageModelDoc`                                        |
| `features/pagemodel/PageModelResponse`               | **SUPPRIMER** | Doublon                                                          |
| `features/pagemodel/PageModelType`                   | **SUPPRIMER** | Doublon de `core/PageModelType`                                  |
| `features/pagemodel/PageStatus`                      | **SUPPRIMER** | Doublon de `core/PageModelStatus`                                |
| `features/pagemodel/PageModelTypeResolver`           | **DÉPLACER**  | → `features/pagemodel/dashboard/app/`                            |
| `features/pagemodel/admin/**`                        | **SUPPRIMER** | Remplacé par `core/infra/web/`                                   |
| `features/pagemodel/LangResolver`                    | **GARDER**    | → `features/pagemodel/shared/`                                   |
| `features/pagemodel/PageDynamicPayload`              | **GARDER**    | → `features/pagemodel/shared/`                                   |
| `features/pagemodel/WidgetDynamicError`              | **GARDER**    | → `features/pagemodel/shared/`                                   |
| `features/pagemodel/PageModelDynamicProvider`        | **GARDER**    | → `features/pagemodel/dynamic/`                                  |
| `features/pagemodel/PageModelDynamicResolver`        | **GARDER**    | → `features/pagemodel/dynamic/` + fix `toShared()`               |
| `features/pagemodel/dynamic/providers/**`            | **GARDER**    | Fix CashierOverviewProvider stub                                 |
| `features/pagemodel_backup/**`                       | **SUPPRIMER** | Dead code entier (17 fichiers)                                   |
| _`PublicPageModelController`_                        | **CRÉER**     | `features/pagemodel/public/web/`                                 |
| _`PublicPageModelService`_                           | **CRÉER**     | `features/pagemodel/public/app/`                                 |
| _`PublicPageModelResponse`_                          | **CRÉER**     | `features/pagemodel/public/model/`                               |
| _`DashboardPageModelController`_                     | **CRÉER**     | `features/pagemodel/dashboard/web/`                              |
| _`DashboardPageModelService`_                        | **CRÉER**     | `features/pagemodel/dashboard/app/`                              |
| _`PageModelOnboardingService`_                       | **CRÉER**     | `features/pagemodel/onboarding/app/`                             |
| _`HeroProvider`_                                     | **CRÉER**     | `features/pagemodel/dynamic/providers/`                          |
| _`DrawsProvider`_                                    | **CRÉER**     | `features/pagemodel/dynamic/providers/`                          |
| _`PlansProvider`_                                    | **CRÉER**     | `features/pagemodel/dynamic/providers/`                          |
| _`TchalaProvider`_                                   | **CRÉER**     | `features/pagemodel/dynamic/providers/`                          |
| _`TestimonialsProvider`_                             | **CRÉER**     | `features/pagemodel/dynamic/providers/`                          |
| _`FeaturesProvider`_                                 | **CRÉER**     | `features/pagemodel/dynamic/providers/`                          |

**Bilan** : 23 GARDER · 17 SUPPRIMER · 4 DÉPLACER · 12 CRÉER

---

## Ordre d'implémentation recommandé (pour OpenSpec #90)

L'ordre strict évite les conflits de compilation et de démarrage Spring :

```
Phase 1 — Cleanup (suppression des conflits bloquants)
  1.1  Supprimer features/pagemodel_backup/ entier
  1.2  Supprimer features/pagemodel/admin/ entier
  1.3  Supprimer features/pagemodel/PageModelRepository + @RepositoryRestResource

Phase 2 — Résoudre les doublons de types
  2.1  Supprimer features/pagemodel/PageModel.java (record)
       → Mettre à jour PageModelDynamicProvider + resolver pour utiliser PageModelDoc
  2.2  Supprimer features/pagemodel/PageModelType.java + PageStatus.java
       → Mettre à jour les imports
  2.3  Supprimer features/pagemodel/PageModelEntity.java
  2.4  Fix ClasspathPageModelTemplateLoader import dans ResolveEffectivePageModelHandler
       (s'assurer que port/out/ est le seul utilisé, supprimer les doublons port/)

Phase 3 — Créer les nouveaux slices
  3.1  Créer features/pagemodel/public/ (controller + service + model)
       → Migrer logique de pagemodelruntime/
  3.2  Créer features/pagemodel/dashboard/ (controller + service)
       → Migrer logique de features/pagemodel/PageModelController
  3.3  Supprimer features/pagemodelruntime/ entier
  3.4  Supprimer features/pagemodel/PageModelController.java + Orchestrator + Service

Phase 4 — Corriger la violation hexagonale
  4.1  Créer features/pagemodel/onboarding/app/PageModelOnboardingService.java
       → Implémentation via CommandBus (UpsertPageModelCommand) + catalog
  4.2  Déplacer PageModelStartupInitializer → onboarding/
  4.3  Supprimer core/infra/init/PageModelBootstrapService.java

Phase 5 — Corrections techniques
  5.1  Fix import PageModelType dans PageModelTemplateSeedRunner (features → core)
  5.2  Fix PageModelPersistenceAdapter.list() (findAll → filtre RLS)
  5.3  Supprimer toShared() dans PageModelDynamicResolver
  5.4  Ajouter PageModelId TypedId dans PageModelInstance
  5.5  Créer policy RLS sur table page_model (migration Flyway V48)

Phase 6 — Providers dynamiques manquants
  6.1  Créer HeroProvider, DrawsProvider, PlansProvider
  6.2  Créer TchalaProvider, TestimonialsProvider, FeaturesProvider
  6.3  Fix CashierOverviewProvider (brancher sur core/sales stats réels)
```

---

## Questions ouvertes pour validation métier

### 1. Qui peut modifier un pagemodel en production ?

Le code actuel ne définit pas d'endpoint pour `TENANT_ADMIN` pour éditer son propre pagemodel. `PageModelAdminController` (core/infra/web/) exige probablement `SUPER_ADMIN` ou `PLATFORM_ADMIN`. À confirmer : le `TENANT_ADMIN` peut-il customiser son `private.dashboard.tenant_admin` ?

_Impact_ : si oui, ajouter un slice `features/pagemodel/tenantconfig/` avec les endpoints `/tenant/pagemodels/*`.

### 2. `private.dashboard.operator` — rôle existant ou abandonné ?

L'enum `PageModelType` déclare `DASHBOARD_OPERATOR` et le JSON `private.dashboard.operator.json` existe. Mais aucune route Angular `/app/dashboard/operator` n'existe dans `private.routes.ts`. Quelle est la décision produit ?

_Impact_ : si le rôle `OPERATOR` est actif, ajouter la route Angular et le composant `OperatorDashboardPage`.

### 3. Scoping des providers : public vs authentifié

`PublicNewsProvider` et les providers `public.home` tournent sans contexte d'auth. Les providers `private.dashboard.*` (ex: `CashierOverviewProvider`) nécessitent un `TchRequestContext`. L'interface `PageModelDynamicProvider.load(... ctx)` reçoit déjà `ctx` nullable — mais faut-il une interface séparée par scope ?

_Impact_ : si les providers privés dépendent fortement du contexte auth (tenantId, userId), envisager `AuthenticatedPageModelDynamicProvider` pour typer le contexte.

### 4. HeroProvider — statique ou dynamique ?

Le JSON déclare `HeroWidget` avec `binding.mode: "static"` pour la page publique. Faut-il quand même un `HeroProvider` backend, ou les props statiques dans le JSON suffisent-elles ?

_Impact_ : si statique, `HeroWidget` ne nécessite pas de provider — réduire la liste à 5 providers à créer.

### 5. Template vs instance — quelle source de vérité pour la home publique ?

Au démarrage, `PageModelTemplateSeedRunner` upserte `page_model_template`, puis `PageModelOnboardingService` (futur) crée une `page_model` (instance) depuis ce template. La résolution cherche d'abord l'instance, puis le template catalog, puis le classpath JSON. Si un tenant n'a jamais été onboardé (ex: DEFAULT_TENANT_UUID non initialisé), quel fallback s'applique ?

_Impact_ : confirmer que le tenant DEFAULT_TENANT_UUID est toujours onboardé, sinon la home publique retourne le JSON classpath vide (4 null records).
