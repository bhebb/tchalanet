# Tchalanet — Architecture du domaine `pagemodel`

> **Status** : NORMATIVE  
> **Version** : 2.0 (refonte post-analyse 2026-04-25)  
> **Scope** : `catalog/pagemodeltemplate` · `core/pagemodel` · `features/pagemodel`  
> **Documents liés** :
> `ARCHITECTURE.md` · `PLAYBOOK.md` · `command_query_handlers.md` · `typed_ids.md` · `rls.md` · `routing_and_path.md` · `inter_domain_calls.md` · `cache.md` · `event_model.md`

---

## 0) Pourquoi ce domaine existe

Le domaine `pagemodel` résout un problème central : **comment le frontend sait-il quoi afficher, et comment le contenu est-il lié aux données métier ?**

La réponse est un modèle déclaratif JSON — le **PageModel** — qui décrit la structure d'une page (layout, widgets, navigation, thème) et déclare quelles données chaque widget doit recevoir. Le backend est responsable de :

1. **Maintenir** les gabarits (templates) déclaratifs — une fois par type de page, au niveau plateforme.
2. **Instancier** ces gabarits par tenant au démarrage.
3. **Résoudre** la page effective au runtime selon le scope (public / tenant / platform) et d'en **hydrater** les données dynamiques widget par widget.

Ce modèle permet de changer la structure d'une page sans déploiement frontend, de personnaliser par tenant, et de composer des dashboards selon le rôle de l'utilisateur.

---

## 1) Concepts fondamentaux

### 1.1 Les deux entités

| Entité              | Table                 | Rôle                                                 | Couche propriétaire         |
| ------------------- | --------------------- | ---------------------------------------------------- | --------------------------- |
| `PageModelTemplate` | `page_model_template` | Gabarit déclaratif, source de vérité, read-mostly    | `catalog/pagemodeltemplate` |
| `PageModel`         | `page_model`          | Instance effective par tenant, modifiable, publiable | `core/pagemodel`            |

**Relation** : un template → N instances (une par tenant + une pour `DEFAULT_TENANT`).

### 1.2 `logicalId` — la clé fonctionnelle

Format : `{scope}.{slug}` — unique parmi les templates non supprimés.

| logicalId                      | Surface                 | Audience              |
| ------------------------------ | ----------------------- | --------------------- |
| `public.home`                  | Page d'accueil publique | Visiteurs anonymes    |
| `public.verify`                | Vérification de ticket  | Visiteurs anonymes    |
| `private.dashboard.cashier`    | Dashboard caissier      | `CASHIER`, `OPERATOR` |
| `private.dashboard.manager`    | Dashboard manager       | `MANAGER`             |
| `private.dashboard.admin`      | Dashboard admin tenant  | `TENANT_ADMIN`        |
| `private.dashboard.superadmin` | Dashboard super admin   | `SUPER_ADMIN`         |

### 1.3 Niveaux de template

- **`GLOBAL`** : `tenant_id IS NULL` — template système, visible de tous les tenants via RLS.
- **`TENANT`** : `tenant_id NOT NULL` — template spécifique à un tenant.

Contrainte DB : `ck_pmt_level_target` garantit la cohérence niveau/tenant.

### 1.4 Statuts d'un PageModel

```
DRAFT ──── publish ────► PUBLISHED
  ▲                          │
  └──── edit (re-draft) ─────┘
```

- `DRAFT` : modifiable, non servi.
- `PUBLISHED` : servi au frontend. Un seul `PUBLISHED` par `(tenant_id, logicalId)` — garanti par `PublishPolicy`.
- `ARCHIVED` : désactivé, conservé pour audit.

### 1.5 PageModelDoc — le modèle canonique unique

Il existe **une seule** représentation Java d'un page model dans le système.

```
PageModelDoc                                    ← core/pagemodel/domain/model/
├── meta     : Meta(id, scope, slug, context, schemaVersion, langs, defaultLang)
├── theme    : Theme(preset, mode, density)
├── shell    : Shell(header, sidenav, footer)
│                └── ShellSection(component, nav, props)
│                         └── Nav(primary[], secondary[])
│                                  └── NavItem(labelKey, path, icon?)
└── content  : Content(layout, widgets)
                  ├── Layout(component, rows[])
                  │            └── LayoutRow(id, labelKey, columns[])
                  │                         └── LayoutColumn(span, widgetIds[])
                  └── widgets : Map<widgetId, WidgetConfig>
                                     └── WidgetConfig(type, binding, props)
                                              └── WidgetBinding(mode, source)
                                                       mode: "static" | "dynamic"
```

**Règle absolue** : aucune copie de `PageModelDoc` dans `features/`. `features/` importe directement depuis `core/pagemodel/domain/model/`.

### 1.6 Widgets dynamiques vs statiques

```
binding.mode = "static"   → aucune donnée backend, le frontend utilise les props
binding.mode = "dynamic"  → le backend charge les données via un PageModelDynamicProvider
```

La source (`binding.source`) identifie quel provider est responsable :

| source             | provider                  | données                      |
| ------------------ | ------------------------- | ---------------------------- |
| `results_by_game`  | `DrawsProvider`           | derniers tirages par jeu     |
| `cashier_overview` | `CashierOverviewProvider` | sessions et ventes caissier  |
| `plans`            | `PlansProvider`           | plans actifs du catalog      |
| `hero`             | `HeroProvider`            | bannière (statique enrichie) |
| `news`             | `PublicNewsProvider`      | actualités publiques         |

---

## 2) Vue d'ensemble des trois flows

```
┌─────────────────────────────────────────────────────────────────────┐
│  FLOW 1 — Seed templates (démarrage)                                │
│                                                                     │
│  classpath JSON  ──►  PageModelTemplateSeedRunner (Order=10)       │
│                             ──►  page_model_template (GLOBAL)       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  FLOW 2 — Instanciation par tenant (démarrage + admin)              │
│                                                                     │
│  PageModelOnboardingRunner (Order=20)                               │
│       ──►  catalog.findByLogicalId()                                │
│       ──►  PageModel PUBLISHED par type  (DEFAULT_TENANT)           │
│                                                                     │
│  Admin : PUT /admin/pagemodels/{id}   →  DRAFT                      │
│          POST /admin/pagemodels/{id}/publish  →  PUBLISHED          │
│                                                                     │
│  Propagation template → instances (after-commit)                    │
│       PageModelTemplateUpdatedEvent  ──►  instances → DRAFT         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  FLOW 3 — Résolution runtime (HTTP)                                 │
│                                                                     │
│  GET /public/pagemodel/{logicalId}                                  │
│  GET /tenant/pagemodel/{logicalId}                                  │
│  GET /tenant/pagemodel/dashboard       ← logicalId selon rôle      │
│  GET /platform/pagemodel/{logicalId}                                │
│  GET /platform/pagemodel/dashboard                                  │
│                                                                     │
│  Controller  ──►  QueryBus  ──►  Handler                            │
│                                     ├── PageModelReaderPort         │
│                                     │    (fallback 3 niveaux)       │
│                                     └── PageModelDynamicResolver    │
│                                          (providers → QueryBus)     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3) Flow 1 — Seed des templates au démarrage

### 3.1 Mécanisme

```
resources/pagemodel/public.home.json
resources/pagemodel/public.verify.json
resources/pagemodel/private.dashboard.cashier.json
resources/pagemodel/private.dashboard.manager.json
resources/pagemodel/private.dashboard.admin.json
resources/pagemodel/private.dashboard.superadmin.json
         │
         ▼ PageModelTemplateSeedRunner @Order(10)
         │    ├── pour chaque JSON : findByLogicalId()
         │    ├── absent → INSERT (GLOBAL, tenant_id=NULL)
         │    └── présent + schemaVersion plus récente → UPDATE
         ▼
  page_model_template (GLOBAL, tenant_id=NULL)
```

### 3.2 Règles du seed

- Exécuté **sans** contexte tenant (templates GLOBAL n'appartiennent à personne).
- **Upsert** basé sur `logicalId` — idempotent, safe à rejouer.
- Ne crée **jamais** d'instances `page_model` — c'est le rôle du Flow 2.
- Fail-fast si un JSON est malformé.

### 3.3 Structure JSON de template

Le JSON embarqué suit exactement le schéma de `PageModelDoc` plus les métadonnées du template :

```json
{
  "code": "public.home",
  "logical_id": "public.home",
  "name": "Public home",
  "schema_version": 1,
  "is_default": true,
  "model": {
    /* PageModelDoc complet */
  }
}
```

---

## 4) Flow 2 — Instanciation des PageModel par tenant

### 4.1 Seed au démarrage

```
PageModelOnboardingRunner @Order(20)
    │ TchContextRunner.runAsTenant(DEFAULT_TENANT_UUID, "startup", ...)
    │
    ▼ PageModelOnboardingService
         pour chaque PageModelType (enum des logicalIds connus) :
              ├── page_model existe déjà ?  → skip (idempotent)
              └── non → PageModelTemplateCatalog.findByLogicalId()
                              ├── template absent → EXCEPTION (fail-fast)
                              └── template présent → createFromTemplate()
                                       → page_model (PUBLISHED, DEFAULT_TENANT)
```

**Règle** : l'absence d'un template pour un `PageModelType` connu est une erreur fatale au démarrage.

### 4.2 Fallback de résolution effective (runtime)

```
PageModelReaderPort.loadEffective(tenantId?, logicalId)
    │
    ├── 1. page_model PUBLISHED pour le tenant courant      → retourner
    ├── 2. page_model PUBLISHED pour DEFAULT_TENANT         → retourner
    └── 3. ClasspathPageModelTemplateLoader.load(logicalId) → retourner (en mémoire, jamais persisté)
```

Ce fallback garantit qu'une page est **toujours** servie, même pour un tenant sans instance personnalisée.

### 4.3 Gestion admin des instances

```
PUT /api/v1/admin/pagemodels/{id}
    ──► UpsertPageModelCommand(id, tenantId, actorId, logicalId, scope, slug, schemaVersion, model)
    ──► UpsertPageModelHandler @TchTx
              ├── charge l'instance (vérifie appartenance au tenant via RLS)
              ├── met à jour le JSON model
              └── status → DRAFT

POST /api/v1/admin/pagemodels/{id}/publish
    ──► PublishPageModelCommand(id, tenantId, actorId)
    ──► PublishPageModelHandler @TchTx
              ├── PublishPolicy.check() : 1 seul PUBLISHED par (tenant, logicalId)
              ├── status → PUBLISHED, publishedAt = clock.instant()
              └── AfterCommit → PageModelPublishedEvent
```

### 4.4 Propagation template → instances (after-commit)

```
PUT /api/v1/platform/pagemodeltemplates/{id}
    ──► UpdatePageModelTemplateCommand
    ──► UpdatePageModelTemplateHandler @TchTx
              ├── met à jour le template
              └── AfterCommit → PageModelTemplateUpdatedEvent(templateId, newModel, newSchemaVersion, actorId)

PageModelTemplateUpdatedListener @EventListener
    ──► PageModelWriterPort.applyTemplateUpdate(templateId, newModel, newSchemaVersion, actorId)
              └── pour chaque instance liée au templateId :
                       ├── met à jour le model JSON
                       ├── status → DRAFT  (jamais PUBLISHED automatiquement)
                       └── updatedBy = actorId
```

**Règle** : la propagation est **eventually consistent** (after-commit). Les pages publiées continuent d'être servies jusqu'au re-publish explicite par l'admin.

---

## 5) Flow 3 — Résolution d'une page au runtime

### 5.1 Endpoints et sécurité

| Scope    | Path                                  | Auth | Tenant    | Permission                |
| -------- | ------------------------------------- | ---- | --------- | ------------------------- |
| PUBLIC   | `GET /public/pagemodel/{logicalId}`   | Non  | Optionnel | —                         |
| TENANT   | `GET /tenant/pagemodel/{logicalId}`   | Oui  | Requis    | `pagemodel.read`          |
| TENANT   | `GET /tenant/pagemodel/dashboard`     | Oui  | Requis    | `pagemodel.read`          |
| PLATFORM | `GET /platform/pagemodel/{logicalId}` | Oui  | Non       | `pagemodel.platform.read` |
| PLATFORM | `GET /platform/pagemodel/dashboard`   | Oui  | Non       | `pagemodel.platform.read` |

### 5.2 Résolution du logicalId pour les dashboards

La sélection du `logicalId` selon le rôle se fait dans le **handler**, pas dans le controller :

```java
// ResolveTenantDashboardHandler
String logicalId = switch (query.role()) {
    case CASHIER, OPERATOR -> "private.dashboard.cashier";
    case MANAGER           -> "private.dashboard.manager";
    case TENANT_ADMIN      -> "private.dashboard.admin";
    default -> throw ProblemRest.forbidden("no_dashboard_for_role");
};
```

### 5.3 Flow de résolution complet

```
Controller (thin)
    │ extrait : tenantId (depuis ctx), lang (depuis header Accept-Language)
    │ construit : ResolveXxxPageQuery(logicalId, tenantId?, lang, role?)
    │
    ▼ QueryBus
    │
    ▼ ResolveXxxPageHandler @UseCase
         │
         ├── valide le préfixe logicalId (public. / private.)
         ├── PageModelReaderPort.loadEffective(tenantId, logicalId)  ← fallback 3 niveaux
         ├── LangResolver.resolve(model, acceptLang)                 ← langue effective
         └── PageModelDynamicResolver.resolve(model, lang, ctx)
                  │
                  │ pour chaque widget avec binding.mode = "dynamic"
                  │
                  ▼ PageModelDynamicProvider.supports(pageId, widgetType, source) ?
                       ├── oui → provider.load(model, widgetId, config, lang, ctx)
                       │              └── QueryBus.send(...) uniquement
                       └── non → WidgetDynamicError(NO_PROVIDER)
         │
         └── PageModelResponse(model, dynamic, lang, langs, errors)
```

### 5.4 Réponse HTTP

```json
{
  "data": {
    "lang": "fr",
    "langs": ["fr", "en", "ht"],
    "model": {
      /* PageModelDoc complet */
    },
    "dynamic": {
      "home.draws": {
        /* payload DrawsProvider */
      },
      "home.plans": {
        /* payload PlansProvider */
      }
    },
    "errors": []
  }
}
```

Les erreurs de providers sont **non bloquantes** : la page est servie avec les widgets disponibles, les widgets en erreur ont un payload null avec une entrée dans `errors`.

---

## 6) Structure de code cible

```
catalog/pagemodeltemplate/
├── api/
│   ├── model/
│   │   ├── PageModelTemplateLevel.java          enum GLOBAL | TENANT
│   │   ├── PageModelTemplateView.java           vue read-only (record)
│   │   └── PageModelTemplateStatsView.java      stats console (record)
│   └── PageModelTemplateCatalog.java            interface publique
├── internal/
│   ├── cache/
│   │   └── PageModelTemplateCacheNames.java     constantes "catalog.pagemodel_template.*"
│   ├── init/
│   │   └── PageModelTemplateSeedRunner.java     ApplicationRunner @Order(10)
│   ├── mapper/
│   │   └── PageModelTemplateMapper.java         Entity → View (MapStruct)
│   ├── persistence/
│   │   ├── PageModelTemplateEntity.java         JPA entity
│   │   └── PageModelTemplateRepository.java     méthodes SANS tenantId (read-side)
│   ├── read/
│   │   └── PageModelTemplateCatalogImpl.java    implémente catalog, @Cacheable, RLS-safe
│   ├── web/
│   │   └── PlatformPageModelTemplateController.java  /platform/pagemodeltemplates
│   └── write/
│       └── PageModelTemplateAdminService.java   write-side uniquement (seed + propagation)

core/pagemodel/
├── domain/
│   ├── model/
│   │   ├── PageModelDoc.java                   modèle canonique unique (record imbriqué)
│   │   ├── PageModelInstance.java              agrégat mutable (state machine)
│   │   ├── PageModelStatus.java                enum DRAFT | PUBLISHED | ARCHIVED
│   │   └── PageModelType.java                  enum des logicalIds connus
│   ├── policy/
│   │   └── PublishPolicy.java                  POJO (pas de @Component)
│   └── event/
│       ├── PageModelPublishedEvent.java         after publish
│       └── PageModelTemplateUpdatedEvent.java   after template update
├── application/
│   ├── port/out/
│   │   ├── PageModelReaderPort.java             loadEffective, findById, search
│   │   ├── PageModelWriterPort.java             save, publish, applyTemplateUpdate
│   │   └── PageModelTemplateLoaderPort.java     chargement classpath fallback
│   ├── command/
│   │   ├── model/
│   │   │   ├── UpsertPageModelCommand.java      (id?, tenantId, actorId, ..., model)
│   │   │   └── PublishPageModelCommand.java     (id, tenantId, actorId)
│   │   └── handler/
│   │       ├── UpsertPageModelHandler.java      @UseCase @TchTx CommandHandler<..., PageModelInstance>
│   │       └── PublishPageModelHandler.java     @UseCase @TchTx CommandHandler<..., Void>
│   └── query/
│       ├── model/
│       │   ├── ResolvePublicPageQuery.java      (logicalId, lang)
│       │   ├── ResolveTenantPageQuery.java      (logicalId, tenantId, lang)
│       │   ├── ResolveTenantDashboardQuery.java (tenantId, role, lang)
│       │   ├── ResolvePlatformPageQuery.java    (logicalId, lang)
│       │   ├── ListPageModelsQuery.java         (tenantId?, scope?, logicalId?, pageable)
│       │   └── PageModelSummaryView.java        projection liste admin
│       └── handler/
│           ├── ResolvePublicPageHandler.java    @UseCase QueryHandler<..., PageModelResponse>
│           ├── ResolveTenantPageHandler.java    @UseCase QueryHandler<..., PageModelResponse>
│           ├── ResolveTenantDashboardHandler.java  @UseCase (résout logicalId par rôle)
│           ├── ResolvePlatformPageHandler.java  @UseCase QueryHandler<..., PageModelResponse>
│           └── ListPageModelsHandler.java       @UseCase QueryHandler<..., TchPage<PageModelSummaryView>>
├── infra/
│   ├── persistence/
│   │   ├── PageModelJpaEntity.java
│   │   ├── PageModelJpaRepository.java          méthodes SANS tenantId (RLS scope)
│   │   ├── PageModelPersistenceAdapter.java     implémente ReaderPort + WriterPort
│   │   └── mapper/
│   │       └── PageModelPersistenceMapper.java  Entity ↔ Instance/Doc (MapStruct, JsonUtils)
│   ├── resources/
│   │   └── ClasspathPageModelTemplateLoader.java  implémente TemplateLoaderPort (JsonUtils injecté)
│   ├── event/
│   │   └── PageModelTemplateUpdatedListener.java  @EventListener → WriterPort.applyTemplateUpdate
│   ├── init/
│   │   ├── PageModelOnboardingRunner.java       ApplicationRunner @Order(20)
│   │   └── PageModelOnboardingService.java      seed instances par tenant
│   └── web/
│       └── PageModelAdminController.java        /admin/pagemodels (TENANT_ADMIN)

features/pagemodel/
├── shared/
│   ├── PageModelResponse.java                  contrat HTTP (model + dynamic + lang + errors)
│   ├── PageDynamicPayload.java                 Map<widgetId, Object>
│   ├── WidgetDynamicError.java                 (widgetId, providerKey, code, message)
│   └── LangResolver.java                       résolution langue (6 niveaux)
├── dynamic/
│   ├── PageModelDynamicResolver.java           itère widgets, dispatch providers
│   ├── PageModelDynamicProvider.java           interface (supports + load)
│   └── providers/
│       ├── DrawsProvider.java                  source "results_by_game" → QueryBus
│       ├── CashierOverviewProvider.java        source "cashier_overview" → QueryBus
│       ├── PlansProvider.java                  source "plans" → PlanCatalog
│       ├── HeroProvider.java                   source "hero" → statique enrichie
│       └── PublicNewsProvider.java             source "news" → QueryBus
├── onboarding/
│   └── app/
│       ├── PageModelOnboardingRunner.java       (dans core/infra/init — référence)
│       └── PageModelOnboardingService.java      (dans core/infra/init — référence)
├── public/
│   └── web/
│       └── PublicPageModelController.java      /public/pagemodel (pas d'auth)
├── tenant/
│   └── web/
│       └── TenantPageModelController.java      /tenant/pagemodel (@PreAuthorize pagemodel.read)
└── platform/
    └── web/
        └── PlatformPageModelController.java    /platform/pagemodel (@PreAuthorize pagemodel.platform.read)
```

---

## 7) Dépendances entre couches

```
                    ┌──────────────────────────────────┐
                    │   classpath JSON (resources/)     │
                    └──────────────┬───────────────────┘
                                   │ seed
                    ┌──────────────▼───────────────────┐
                    │   catalog/pagemodeltemplate        │
                    │   (PageModelTemplateCatalog)       │
                    └──────────────┬───────────────────┘
                                   │ findByLogicalId() [lecture seule]
                    ┌──────────────▼───────────────────┐
                    │   core/pagemodel                  │
                    │   domain / application / infra    │
                    └──────────────┬───────────────────┘
                                   │ QueryBus / CommandBus
                                   │ PageModelDoc
                    ┌──────────────▼───────────────────┐
                    │   features/pagemodel              │
                    │   controllers / resolver          │
                    │   providers → QueryBus            │
                    └──────────────────────────────────┘
```

| Dépendance                                  | Statut                  |
| ------------------------------------------- | ----------------------- |
| `features/` → `core/` via bus               | ✅ autorisé             |
| `features/` → `catalog/` en lecture directe | ✅ autorisé (read-only) |
| `core/` → `catalog/` en lecture via port    | ✅ autorisé             |
| `core/` → `features/`                       | ❌ interdit             |
| `catalog/` → `core/`                        | ❌ interdit             |
| Écriture cross-domain en même transaction   | ❌ interdit             |

---

## 8) Règles non négociables

### 8.1 RLS — jamais de tenantId en SQL read-side

```java
// ❌ INTERDIT dans read-side
repository.findByTenantIdAndLogicalIdAndDeletedAtIsNull(tenantId, logicalId)

// ✅ CORRECT — RLS filtre
repository.findByLogicalIdAndDeletedAtIsNull(logicalId)
```

Cette règle s'applique à `PageModelJpaRepository` et `PageModelTemplateRepository` dans leurs usages read-side. Les méthodes avec `tenantId` sont réservées aux opérations write-side (admin) uniquement.

### 8.2 Contexte — jamais dans les handlers

```java
// ❌ INTERDIT dans un handler
TchContext.get().tenantUuid()

// ✅ CORRECT — passé par la commande/query
command.tenantId()
query.tenantId()
```

Le contexte HTTP s'arrête au controller. Les handlers reçoivent uniquement des données extraites et typées.

### 8.3 Typed IDs — jamais de UUID brut hors persistence

```java
// ❌ INTERDIT dans command, query, handler, port, DTO
UUID tenantId
UUID actorId
UUID pageModelId

// ✅ CORRECT
TenantId tenantId
UserId actorId
PageModelId pageModelId
```

### 8.4 Sécurité — @PreAuthorize obligatoire

Tout endpoint hors `/public/**` doit avoir `@PreAuthorize` déclaré sur la méthode. Pas d'if/else de sécurité dans les services ou handlers.

### 8.5 ObjectMapper — jamais instancié directement

```java
// ❌ INTERDIT
new ObjectMapper()
static final ObjectMapper M = new ObjectMapper()

// ✅ CORRECT
@RequiredArgsConstructor
public class MyService {
    private final JsonUtils jsonUtils;
}
```

### 8.6 Génération d'ID — jamais UUID.randomUUID() dans les handlers

```java
// ❌ INTERDIT dans un handler
UUID.randomUUID()
PageModelId.random() // si random() appelle UUID.randomUUID()

// ✅ CORRECT
PageModelId.of(idGenerator.newUuid())
```

### 8.7 PageModelDynamicProvider — QueryBus uniquement

Un provider ne touche jamais directement un repository. Il dispatch uniquement via `QueryBus`.

### 8.8 Propagation template → instances

La propagation est after-commit. Les instances propagées passent en `DRAFT`. Un admin doit explicitement publier après propagation. Jamais de `PUBLISHED` automatique.

### 8.9 PublishPolicy — POJO, pas @Component

`PublishPolicy` est un service domaine pur. Il est instancié par le handler, pas injecté par Spring. Le domaine est framework-free.

---

## 9) Contrat HTTP complet

### 9.1 Résolution publique

```
GET /api/v1/public/pagemodel/{logicalId}
  Auth       : non
  Tenant     : optionnel (DEFAULT_TENANT si absent)
  Préfixe    : doit commencer par "public."
  Retour     : ApiResponse<PageModelResponse>
  Cache      : L1 Caffeine TTL court (données modifiées par admin uniquement)
```

### 9.2 Résolution tenant

```
GET /api/v1/tenant/pagemodel/{logicalId}
  Auth       : oui
  Tenant     : requis depuis TchRequestContext
  Préfixe    : doit commencer par "private."
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.read')")
  Retour     : ApiResponse<PageModelResponse>

GET /api/v1/tenant/pagemodel/dashboard
  Auth       : oui
  Tenant     : requis
  Note       : logicalId résolu par ResolveTenantDashboardHandler selon ctx.currentRole()
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.read')")
  Retour     : ApiResponse<PageModelResponse>
```

### 9.3 Résolution plateforme

```
GET /api/v1/platform/pagemodel/{logicalId}
  Auth       : oui
  Tenant     : non (platform-level, pas de tenantId)
  Préfixe    : doit commencer par "private."
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.platform.read')")
  Retour     : ApiResponse<PageModelResponse>

GET /api/v1/platform/pagemodel/dashboard
  Auth       : oui
  Note       : logicalId fixe = "private.dashboard.superadmin"
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.platform.read')")
  Retour     : ApiResponse<PageModelResponse>
```

### 9.4 Administration des instances (TENANT_ADMIN)

```
GET /api/v1/admin/pagemodels
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.admin.read')")
  Params     : scope?, logicalId?, @TchPaging(allowedSort=["logicalId","scope","updatedAt"])
  Retour     : ApiResponse<TchPage<PageModelSummaryView>>

PUT /api/v1/admin/pagemodels/{id}
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.admin.write')")
  Corps      : PageModelUpsertRequest(logicalId, scope, slug, schemaVersion, model: JsonNode)
  Note       : status → DRAFT après modification
  Retour     : ApiResponse<PageModelSummaryView>

POST /api/v1/admin/pagemodels/{id}/publish
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.admin.publish')")
  Note       : PublishPolicy vérifie unicité PUBLISHED par (tenant, logicalId)
  Retour     : ApiResponse<Void>
```

### 9.5 Administration des templates (SUPER_ADMIN)

```
GET /api/v1/platform/pagemodeltemplates
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.template.read')")
  Params     : logicalId?, name?, @TchPaging(allowedSort=["logicalId","name","updatedAt"])
  Retour     : ApiResponse<TchPage<PageModelTemplateView>>

GET /api/v1/platform/pagemodeltemplates/{id}
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.template.read')")
  Retour     : ApiResponse<PageModelTemplateView>

PUT /api/v1/platform/pagemodeltemplates/{id}
  Permission : @PreAuthorize("hasPermission(null, 'pagemodel.template.write')")
  Note       : after-commit → PageModelTemplateUpdatedEvent → instances → DRAFT
  Retour     : ApiResponse<PageModelTemplateView>
```

---

## 10) Cache

| Cache name                                 | TTL suggéré | Clé          | Invalidation        |
| ------------------------------------------ | ----------- | ------------ | ------------------- |
| `catalog.pagemodel_template.by_logical_id` | 5 min       | `logicalId`  | sur update template |
| `catalog.pagemodel_template.by_id`         | 5 min       | `id.value()` | sur update template |
| `catalog.pagemodel_template.list_visible`  | 5 min       | `""`         | sur update template |
| `public.pagemodel.effective`               | 2 min       | `logicalId`  | sur publish         |

Le cache de résolution public (`public.pagemodel.effective`) est optionnel en phase 1 — ajouter seulement si la latence de résolution devient un problème mesuré.

---

## 11) Startup — ordre et dépendances

```
Order=10  PageModelTemplateSeedRunner
          │ contexte : pas de tenant (templates GLOBAL)
          │ dépend de : DB disponible, classpath JSON présents
          └─► page_model_template upsertés

Order=20  PageModelOnboardingRunner
          │ contexte : TchContextRunner.runAsTenant(DEFAULT_TENANT_UUID, "startup", ...)
          │ dépend de : Order=10 complété (templates présents)
          └─► page_model PUBLISHED pour DEFAULT_TENANT
```

Si `Order=10` échoue (JSON manquant, DB indisponible) → `Order=20` doit aussi échouer (exception propagée). Le serveur ne démarre pas avec un état incohérent.

---

## 12) Checklist PR — domaine pagemodel

### Sécurité (bloquant)

- [ ] `@PreAuthorize` sur chaque méthode de controller hors `/public/**`
- [ ] Aucun `TchContext.get()` dans les handlers
- [ ] Aucun filtre tenant SQL explicite dans le read-side
- [ ] `tenantId` jamais accepté comme paramètre HTTP client

### Architecture (bloquant)

- [ ] `PageModelDoc` utilisé partout — aucune copie dans `features/`
- [ ] Handlers : `@UseCase` + `implements CommandHandler` ou `QueryHandler`
- [ ] Commands/Queries : `TenantId`, `UserId`, `PageModelId` — pas de `UUID` brut
- [ ] Ports : signatures avec Typed IDs uniquement
- [ ] `PageModelId` : record conforme (of, nullableOf, parse — pas de random())

### Code quality (majeur)

- [ ] `JsonUtils` injecté partout — aucun `new ObjectMapper()`
- [ ] `IdGenerator` injecté pour la génération d'ID — aucun `UUID.randomUUID()`
- [ ] `PublishPolicy` : POJO sans `@Component`
- [ ] `Clock` injecté — aucun `Instant.now()` direct

### Complétude (majeur)

- [ ] `ListPageModelsHandler` présent et enregistré dans le bus
- [ ] `PageModelTemplateUpdatedEvent` + listener after-commit présents
- [ ] Split controllers : `PublicPageModelController` / `TenantPageModelController` / `PlatformPageModelController`
- [ ] `ResolveTenantDashboardHandler` : logique rôle→logicalId dans le handler

### Startup

- [ ] `PageModelTemplateSeedRunner` : `@Order(10)`
- [ ] `PageModelOnboardingRunner` : `@Order(20)` + `TchContextRunner.runAsTenant(...)`
- [ ] Fail-fast si template absent pour un `PageModelType` connu

---

## 13) Décisions d'architecture et raisons

| Décision                                                | Raison                                                                                                                         |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `PageModelDoc` dans `core/domain/` (pas dans `common/`) | C'est un concept métier propre à la gestion de pages, pas un type technique partagé                                            |
| Fallback 3 niveaux dans le port (pas dans le handler)   | Le fallback est une règle de résolution de données, pas un use-case métier — il appartient à l'adapter                         |
| Résolution dashboard par rôle dans le handler           | C'est de la logique applicative (quel logicalId pour quel rôle) — pas du routing, pas du controller                            |
| Propagation template → instances after-commit           | Les instances publiées ne peuvent pas changer sans validation explicite. La propagation crée un travail explicite pour l'admin |
| PublishPolicy : POJO sans Spring                        | Le domaine doit rester testable sans Spring. PublishPolicy est instancié par le handler qui contrôle son cycle de vie          |
| Cache court sur résolution publique                     | Les PageModel publics changent peu mais doivent refléter rapidement les publications admin                                     |
| Providers → QueryBus uniquement                         | Les providers sont des adaptateurs, pas des domaines. Ils ne contiennent aucune logique — ils posent des questions au bus      |
