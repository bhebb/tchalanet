# Delta Spec: PageModel — Fin de migration hexagonale

**Change ID:** `90-finish-pagemodel-migration-20260424`
**Affects:** `core/pagemodel`, `features/pagemodel`, `catalog/pagemodeltemplate`, `features/pagemodelruntime`, `features/pagemodel_backup`

---

## Context packs requis

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/80-core-rules.md`
- `openspec/context/81-feature-rules.md`

Near-code references:

- `tchalanet-docs/docs/02-features/FEATURE_PAGEMODEL.md`
- `tchalanet-docs/docs/02-features/PAGEMODEL-ARCHITECTURE-CIBLE.md`
- `tchalanet-server/src/main/resources/pagemodel/*.json`

---

## REMOVED

### Composants supprimés — features/pagemodel_backup/

Tout le répertoire `features/pagemodel_backup/` est supprimé.

Raison : dead code — `PublicPageModelController` y crée un troisième binding
sur la route `GET /api/v1/public/pagemodel/{logicalId}`, ce qui est bloquant au démarrage
Spring. Aucun composant actif ne dépend de ce package.

Classes supprimées :

- `PublicPageModelController` (conflit route)
- `PageModelRuntimeService` (legacy, remplacé par pagemodelruntime puis public/)
- `Block*` classes (HeroBlock, NewsBlock, PlansBlock, ResultsByGameBlock, TchalaBlock, FeaturesBlock)
- `*Provider` classes legacy (NewsProvider, PlansProvider, ResultsByGameProvider, etc.)
- `PageModel`, `PageModelDynamicProvider`, `PageModelDynamicResolver`, `PageModelResponse` (legacy)
- `PublicPageDynamicPayload`

---

### Composants supprimés — features/pagemodel/ (sélectif)

| Fichier                               | Raison                                                   |
| ------------------------------------- | -------------------------------------------------------- |
| `PageModelRepository.java`            | `@RepositoryRestResource` — convention interdite         |
| `admin/PageModelAdminController.java` | Doublon de `core/infra/web/PageModelAdminController`     |
| `admin/PageModelAdminService.java`    | Remplacé par CommandBus                                  |
| `admin/dto/*.java`                    | Remplacés par modèles core                               |
| `PageModelController.java`            | Remplacé par slices public/ + dashboard/                 |
| `PageModelOrchestrator.java`          | Remplacé par slice services                              |
| `PageModelService.java`               | Remplacé par QueryBus + ResolveEffectivePageModelHandler |
| `PageModel.java` (record)             | Doublon de `core.pagemodel.domain.model.PageModelDoc`    |
| `PageModelType.java`                  | Doublon de `core.pagemodel.domain.model.PageModelType`   |
| `PageStatus.java`                     | Doublon de `core.pagemodel.domain.model.PageModelStatus` |
| `PageModelEntity.java`                | Doublon de `core.infra.persistence.PageModelJpaEntity`   |
| `PageModelResponse.java`              | Remplacé par slice responses                             |

---

### Composants supprimés — features/pagemodelruntime/

Tout le répertoire `features/pagemodelruntime/` est supprimé après migration.

`PageModelRuntimeService` est migré vers `features/pagemodel/public/app/PublicPageModelService`.
`PageModelController` (pagemodelruntime) est remplacé par `PublicPageModelController`.
`PageModelRuntimeResponse` est remplacé par `PublicPageModelResponse`.

---

### Composants supprimés — core/pagemodel/

| Fichier                                       | Raison                                                                             |
| --------------------------------------------- | ---------------------------------------------------------------------------------- |
| `infra/init/PageModelBootstrapService.java`   | Violation hexagonale (core → features) — migré dans features/pagemodel/onboarding/ |
| `infra/init/PageModelStartupInitializer.java` | Idem — migré dans features/pagemodel/onboarding/                                   |
| `application/port/PageModelReadPort.java`     | Doublon de `application/port/out/PageModelReadPort.java`                           |
| `application/port/PageModelWritePort.java`    | Doublon de `application/port/out/PageModelWritePort.java`                          |

---

## MODIFIED

### `catalog/pagemodeltemplate/internal/init/PageModelTemplateSeedRunner`

#### Changement : fix import PageModelType

**Avant :**

```java
import com.tchalanet.server.features.pagemodel.PageModelType; // ❌ catalog → features
```

**Après :**

```java
import com.tchalanet.server.core.pagemodel.domain.model.PageModelType; // ✅
```

---

### `core/pagemodel/infra/persistence/PageModelPersistenceAdapter`

#### Changement : fix list() — cross-tenant

**Avant :**

```java
public List<PageModelInstance> list() {
    return repo.findAll().stream().map(mapper::toDomain).toList(); // ❌ cross-tenant
}
```

**Après :**

```java
public List<PageModelInstance> list() {
    return repo.findAllByDeletedAtIsNull().stream().map(mapper::toDomain).toList(); // ✅ RLS filtre par tenant
}
```

---

### `core/pagemodel/application/command/handler/PublishPageModelHandler`

#### Changement : archiver l'instance PUBLISHED précédente

**Invariant ajouté :** 1 seul `PUBLISHED` par (`tenant_id`, `logical_id`).

**Scenario :**

- GIVEN un tenant avec une instance `PUBLISHED` pour `public.home`
- WHEN `PublishPageModelCommand` est dispatché pour une autre instance de `public.home`
- THEN l'ancienne instance passe en `ARCHIVED` ET la nouvelle passe en `PUBLISHED`
  dans la même transaction

---

### `features/pagemodel/PageModelDynamicResolver`

#### Changement : supprimer toShared()

La méthode `toShared(PageModelDoc → features.PageModel)` (~150 lignes) est supprimée.

La méthode `resolve(PageModelDoc doc, String lang, TchRequestContext ctx)` travaille
directement sur `PageModelDoc` sans conversion intermédiaire.

L'interface `PageModelDynamicProvider.load()` reçoit `PageModelDoc` (core) au lieu
de `features.PageModel` (supprimé).

---

## ADDED

### Route publique unifiée

#### Requirement: Route GET /public/pagemodel/{logicalId} sans conflit

**Scenario :**

- GIVEN un logicalId valide (`public.home`, `private.dashboard.cashier`, etc.)
- WHEN `GET /api/v1/public/pagemodel/{logicalId}?lang=fr` est appelé
- THEN exactement 1 controller gère la requête : `PublicPageModelController`
- AND la réponse est `ApiResponse<PublicPageModelResponse>`
- AND `PublicPageModelResponse` contient `{ currentLang, langs, pageModel: PageModelDoc, dynamic: PageDynamicPayload }`

---

### Slice features/pagemodel/public/

#### `PublicPageModelController`

Route : `GET /public/pagemodel/{logicalId}` (prefix global `/api/v1` via config)
Scope : anonymous, pas d'auth requise
Délègue à : `PublicPageModelService.resolve(logicalId, langFromUrl)`
Retourne : `ApiResponse<PublicPageModelResponse>`

#### `PublicPageModelService`

Orchestration :

1. `QueryBus.send(new ResolveEffectivePageModelQuery(tenantId, logicalId))` → `PageModelDoc`
2. `LangResolver.resolve(ctx)` → `currentLang`
3. `PageModelDynamicResolver.resolve(doc, currentLang, requestCtx)` → `PageDynamicPayload`
4. Assembler `PublicPageModelResponse`

#### `PublicPageModelResponse`

```java
public record PublicPageModelResponse(
    String currentLang,
    List<String> langs,
    PageModelDoc pageModel,
    PageDynamicPayload dynamic
) {}
```

---

### Slice features/pagemodel/dashboard/

#### `DashboardPageModelController`

Routes :

```
GET /tenant/pagemodel/{logicalId}     → scope TENANT, @CurrentContext requis
GET /tenant/pagemodel/dashboard       → résout logicalId par rôle via PageModelTypeResolver
GET /platform/pagemodel/{logicalId}   → scope PLATFORM, SUPER_ADMIN requis
GET /platform/pagemodel/dashboard     → private.dashboard.superadmin
```

Toutes retournent `ApiResponse<DashboardPageModelResponse>`.

---

### Slice features/pagemodel/onboarding/

#### `PageModelOnboardingService`

Remplace `core/infra/init/PageModelBootstrapService` (violation hexagonale).

```java
// ✅ Orchestre catalog/ + core/ sans violer le graphe
@Service
public class PageModelOnboardingService {
    private final PageModelTemplateCatalog templateCatalog; // catalog/
    private final CommandBus commandBus;                    // core/ via bus
    private final PageModelReadPort readPort;               // core/ via port

    public void seedDefaults() {
        for (PageModelType type : PageModelType.values()) {
            if (readPort.findPublishedByLogicalId(type.logicalId()).isEmpty()) {
                var template = templateCatalog.findByLogicalId(type.logicalId())
                    .orElseThrow(...);
                commandBus.dispatch(new UpsertPageModelCommand(template.model(), type));
            }
        }
    }
}
```

**Scenario : onboarding d'un nouveau tenant**

- GIVEN un tenant sans pagemodels en base
- WHEN `PageModelOnboardingRunner` s'exécute (@Order(20))
- THEN 5 instances `PUBLISHED` sont créées (une par `PageModelType`)
- AND aucune dépendance directe de `core/` vers `features/`

---

### `PageModelId` TypedId

```java
// core/pagemodel/domain/model/PageModelId.java
public record PageModelId(UUID value) {
    public static PageModelId of(UUID value) { return new PageModelId(value); }
    public static PageModelId random()       { return new PageModelId(UUID.randomUUID()); }
}
```

---

### Policy RLS — table `page_model` (conditionnel)

Si la policy est absente de V40/V41 :

```sql
-- V52__pagemodel_rls.sql
ALTER TABLE page_model ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON page_model
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
```

Pattern canonique : identique à la policy RLS des autres tables tenantées (V40).

---

### Providers dynamiques ajoutés

#### `DrawsProvider`

| Propriété       | Valeur                                                   |
| --------------- | -------------------------------------------------------- |
| `providerKey()` | `"draws"`                                                |
| `supports()`    | `source == "results_by_game"` ou `source == "draws"`     |
| Payload         | `{ draws: [{ name, results[], drawnAt }] }` — LIMIT 4    |
| Source          | `QueryBus → GetRecentDrawResultsQuery` (core/drawresult) |

#### `PlansProvider`

| Propriété       | Valeur                                           |
| --------------- | ------------------------------------------------ |
| `providerKey()` | `"plans"`                                        |
| `supports()`    | `source == "plans"`                              |
| Payload         | `{ plans: [{ code, name, price, features[] }] }` |
| Source          | `PlanCatalog.findAllActive()` (catalog/plan)     |

#### `HeroProvider`

| Propriété       | Valeur                                              |
| --------------- | --------------------------------------------------- |
| `providerKey()` | `"hero"`                                            |
| `supports()`    | `source == "hero"`                                  |
| Payload         | `{ backgroundUrl?, ctaLinks[], tagline?, stats? }`  |
| Source          | Configuration + enrichissement contextuel optionnel |

#### `CashierOverviewProvider` (fix)

| Propriété       | Valeur                                                                        |
| --------------- | ----------------------------------------------------------------------------- |
| `providerKey()` | `"overview"`                                                                  |
| Payload         | `{ ticketsToday, totalAmount, sessionOpen, sessionId, openedAt? }`            |
| Source          | `QueryBus → GetPosSessionTotalsQuery` (core/sales)                            |
| Fallback        | `{ ticketsToday: 0, totalAmount: 0, sessionOpen: false }` si session inactive |

---

## Invariants non modifiés

- Cycle de vie `PageModelInstance` : `DRAFT → PUBLISHED → ARCHIVED` — inchangé
- Fallback 3 niveaux dans `ResolveEffectivePageModelHandler` — inchangé
- Format JSON `PageModelDoc` (meta/theme/shell/content/widgets) — inchangé
- 5 logicalIds actifs (`PageModelType` enum) — inchangé
- `catalog/pagemodeltemplate/` structure et comportement — inchangé
- `LangResolver` algorithme de priorité — inchangé
- `PageModelDynamicProvider` interface (`supports()`, `load()`, `providerKey()`) — inchangé
  (seul le type du paramètre `pageModel` change de `features.PageModel` → `core.PageModelDoc`)
