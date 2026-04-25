# Implementation Tasks: Finir la migration PageModel

**Change ID:** `90-finish-pagemodel-migration-20260424`
**Scope:** Backend Spring Boot uniquement
**Ordre d'exécution:** Groupe A → B → C → D → E (dépendances strictes)

---

## Groupe A — Cleanup bloquant (Phase 1)

> Résoudre les conflits Spring au démarrage.
> **Règle** : compiler après chaque suppression. Ne jamais merger une phase partiellement.
> Chaque tâche = 1 point.

---

- [x] A.1 Supprimer `features/pagemodel_backup/` entier
      Fichier(s) : `features/pagemodel_backup/` (répertoire complet — 17 fichiers)
      Action : SUPPRIMER
      Dépend de : —
      Points : 1
      Notes : Inclut PublicPageModelController (conflit route), PageModelRuntimeService (legacy),
      Block* classes, *Provider classes legacy. Supprimer le répertoire entier d'un coup.

- [x] A.2 Supprimer `PageModelRepository` (@RepositoryRestResource)
      Fichier(s) : `features/pagemodel/PageModelRepository.java`
      Action : SUPPRIMER
      Dépend de : A.1
      Points : 1
      Notes : Violation critique. Vérifier que `PageModelBootstrapService` (core/) et `PageModelService`
      référencent ce repo — ils seront supprimés dans A.6 et C.3.

- [x] A.3 Supprimer `features/pagemodel/admin/` entier
      Fichier(s) : `features/pagemodel/admin/PageModelAdminController.java`,
      `features/pagemodel/admin/PageModelAdminService.java`,
      `features/pagemodel/admin/dto/PageModelAdminDetailDto.java`,
      `features/pagemodel/admin/dto/PageModelAdminListItemDto.java`,
      `features/pagemodel/admin/dto/PageModelAdminUpsertRequest.java`
      Action : SUPPRIMER
      Dépend de : A.2
      Points : 1
      Notes : Le CRUD admin est assuré par `core/infra/web/PageModelAdminController` (CommandBus).
      Vérifier qu'aucun test ne référence `PageModelAdminService`.

- [x] A.4 Supprimer `features/pagemodel/PageModelController.java`
      Fichier(s) : `features/pagemodel/PageModelController.java`
      Action : SUPPRIMER
      Dépend de : A.3
      Points : 1
      Notes : Routes /public/, /tenant/, /platform/ — seront recréées dans slices C.1 et C.2.
      Ce controller délègue à PageModelOrchestrator (supprimé en A.5).

- [x] A.5 Supprimer `features/pagemodel/PageModelOrchestrator.java`
      Fichier(s) : `features/pagemodel/PageModelOrchestrator.java`
      Action : SUPPRIMER
      Dépend de : A.4
      Points : 1
      Notes : Logique de dispatch public/tenant/platform migrée dans slice services (C.1, C.2).

- [x] A.6 Supprimer `features/pagemodel/PageModelService.java`
      Fichier(s) : `features/pagemodel/PageModelService.java`
      Action : SUPPRIMER
      Dépend de : A.5
      Points : 1
      Notes : Remplacé par QueryBus → ResolveEffectivePageModelHandler dans les nouveaux services.
      Vérifier que `PageModelBootstrapService` (core/) sera traité en C.3.

- [x] A.7 Supprimer `features/pagemodelruntime/PageModelController.java`
      Fichier(s) : `features/pagemodelruntime/PageModelController.java`
      Action : SUPPRIMER
      Dépend de : A.4
      Points : 1
      Notes : Route `/api/v1/public/pagemodel/{logicalId}` hardcodée (violation convention).
      La logique de service est migrée en C.1 depuis pagemodelruntime/PageModelRuntimeService.

**Quality Gate A :**

- [x] `./mvnw clean compile` passe sans erreur
- [x] Aucun conflit `@RequestMapping` détecté (vérifier avec `grep -r "pagemodel" --include="*.java" | grep "@RequestMapping"`)
- [x] Aucun `@RepositoryRestResource` dans `features/pagemodel/`

---

## Groupe B — Résolution des doublons de types (Phase 2)

> Éliminer les types dupliqués entre `features/` et `core/`.
> Chaque tâche = 2 points (recherche + remplacement + compile check).

---

- [x] B.1 Supprimer `features/pagemodel/PageModel.java` (record doublon)
      Fichier(s) : `features/pagemodel/PageModel.java`
      Action : SUPPRIMER
      Dépend de : A.7
      Points : 2
      Notes : Remplacer tous les usages par `core.pagemodel.domain.model.PageModelDoc`.
      Principalement : `PageModelDynamicProvider.load()`, `PageModelDynamicResolver`,
      `PageModelOrchestrator` (déjà supprimé). Vérifier avec `grep -r "features.pagemodel.PageModel"`.

- [x] B.2 Supprimer `features/pagemodel/PageModelType.java` (enum doublon)
      Fichier(s) : `features/pagemodel/PageModelType.java`
      Action : SUPPRIMER
      Dépend de : B.1
      Points : 2
      Notes : Remplacer par `core.pagemodel.domain.model.PageModelType`.
      Références : `PageModelTypeResolver`, `PageModelTemplateSeedRunner` (catalog — important).

- [x] B.3 Supprimer `features/pagemodel/PageStatus.java` (enum doublon)
      Fichier(s) : `features/pagemodel/PageStatus.java`
      Action : SUPPRIMER
      Dépend de : B.2
      Points : 2
      Notes : Remplacer par `core.pagemodel.domain.model.PageModelStatus`.
      Vérifier les usages dans les mappers et entity features (déjà supprimée en B.5).

- [x] B.4 Supprimer `features/pagemodel/PageModelEntity.java` (entité doublon)
      Fichier(s) : `features/pagemodel/PageModelEntity.java`
      Action : SUPPRIMER
      Dépend de : B.3
      Points : 2
      Notes : Doublon de `core/infra/persistence/PageModelJpaEntity`.
      Plus référencée après suppression de PageModelRepository (A.2) et PageModelService (A.6).

- [x] B.5 Supprimer `features/pagemodel/PageModelResponse.java`
      Fichier(s) : `features/pagemodel/PageModelResponse.java`
      Action : SUPPRIMER
      Dépend de : B.4
      Points : 2
      Notes : Remplacée par `features/pagemodel/public/model/PublicPageModelResponse` (créée en C.1)
      et `features/pagemodel/dashboard/model/DashboardPageModelResponse` (créée en C.2).

- [x] B.6 Supprimer `PageModelDynamicResolver.toShared()` (bridge inutile)
      Fichier(s) : `features/pagemodel/PageModelDynamicResolver.java`
      Action : MODIFIER
      Dépend de : B.1
      Points : 2
      Notes : Supprimer la méthode `toShared(PageModelDoc → features.PageModel)` (~150 lignes).
      Garder uniquement `resolve(PageModelDoc, String, TchRequestContext)` qui appelle
      directement la logique sur PageModelDoc.
      L'overload `resolve(PageModel, ...)` disparaît aussi (type supprimé).

- [x] B.7 Supprimer `core/application/port/PageModelReadPort.java` (doublon de port/out/)
      Fichier(s) : `core/pagemodel/application/port/PageModelReadPort.java`
      Action : SUPPRIMER
      Dépend de : A.7
      Points : 2
      Notes : Le port canonique est dans `core/pagemodel/application/port/out/PageModelReadPort.java`.
      Mettre à jour l'import dans `ResolveEffectivePageModelHandler` :
      `port.PageModelReadPort` → `port.out.PageModelReadPort`.

- [x] B.8 Supprimer `core/application/port/PageModelWritePort.java` (doublon de port/out/)
      Fichier(s) : `core/pagemodel/application/port/PageModelWritePort.java`
      Action : SUPPRIMER
      Dépend de : B.7
      Points : 2
      Notes : Même correction pour `UpsertPageModelHandler` et `PublishPageModelHandler`.

**Quality Gate B :**

- [x] `./mvnw clean compile` passe sans erreur
- [x] `grep -r "features.pagemodel.PageModel[^D]" --include="*.java"` retourne 0 résultat
- [x] `grep -r "features.pagemodel.PageModelType" --include="*.java"` retourne 0 résultat
- [x] `grep -r "application.port.PageModelReadPort" --include="*.java"` retourne 0 résultat (sauf port/out)

---

## Groupe C — Nouveaux slices features/pagemodel/ (Phase 3)

> Créer la structure par slice. Dépend de A entier + B entier.
> Chaque slice = 2–3 points.

---

- [x] C.1 Créer slice `features/pagemodel/public/`
      Fichier(s) :
      `features/pagemodel/public/web/PublicPageModelController.java`
      `features/pagemodel/public/app/PublicPageModelService.java`
      `features/pagemodel/public/model/PublicPageModelResponse.java`
      Action : CRÉER
      Dépend de : B.6
      Points : 3
      Notes :
      PublicPageModelController :
      @GetMapping("/public/pagemodel/{logicalId}")
      Scope : anonymous, pas d'authGuard
      Délègue à PublicPageModelService.resolve(logicalId, lang)
      Retourne ApiResponse<PublicPageModelResponse>
      PublicPageModelService :
      = pagemodelruntime/PageModelRuntimeService migré
      QueryBus.send(ResolveEffectivePageModelQuery) + LangResolver + PageModelDynamicResolver
      PublicPageModelResponse :
      record(String currentLang, List<String> langs, PageModelDoc pageModel, PageDynamicPayload dynamic)

- [x] C.2 Créer slice `features/pagemodel/dashboard/`
      Fichier(s) :
      `features/pagemodel/dashboard/web/DashboardPageModelController.java`
      `features/pagemodel/dashboard/app/DashboardPageModelService.java`
      `features/pagemodel/dashboard/app/PageModelTypeResolver.java`
      `features/pagemodel/dashboard/model/DashboardPageModelResponse.java`
      Action : CRÉER + DÉPLACER (PageModelTypeResolver)
      Dépend de : C.1
      Points : 3
      Notes :
      DashboardPageModelController :
      @GetMapping("/tenant/pagemodel/{logicalId}") ← scope TENANT
      @GetMapping("/tenant/pagemodel/dashboard") ← dashboard par rôle
      @GetMapping("/platform/pagemodel/{logicalId}") ← scope PLATFORM
      @GetMapping("/platform/pagemodel/dashboard") ← dashboard superadmin
      DashboardPageModelService : même mécanique que PublicPageModelService
      mais avec TchRequestContext (tenantId disponible)
      PageModelTypeResolver : déplacer depuis features/pagemodel/ (supprimé après déplacement)

- [x] C.3 Créer slice `features/pagemodel/onboarding/`
      Fichier(s) :
      `features/pagemodel/onboarding/app/PageModelOnboardingService.java`
      `features/pagemodel/onboarding/app/PageModelOnboardingRunner.java`
      Action : CRÉER (remplace core/infra/init/)
      Dépend de : B.8
      Points : 3
      Notes :
      PageModelOnboardingService :
      Remplace core/PageModelBootstrapService (violation hexagonale)
      Orchestre : PageModelTemplateCatalog.findByLogicalId() + CommandBus.dispatch(new UpsertPageModelCommand(...))
      Itère sur PageModelType.values() (core enum)
      Vérifie existence via PageModelReadPort avant de créer
      PageModelOnboardingRunner :
      @Order(20) — APRÈS PageModelTemplateSeedRunner (@Order(10))
      Appelle PageModelOnboardingService.seedDefaults()
      Après création : supprimer
      core/pagemodel/infra/init/PageModelBootstrapService.java
      core/pagemodel/infra/init/PageModelStartupInitializer.java

- [x] C.4 Organiser `features/pagemodel/shared/`
      Fichier(s) :
      `features/pagemodel/shared/LangResolver.java`
      `features/pagemodel/shared/PageDynamicPayload.java`
      `features/pagemodel/shared/WidgetDynamicError.java`
      Action : DÉPLACER (depuis features/pagemodel/ racine)
      Dépend de : C.2
      Points : 2
      Notes : Déplacer LangResolver, PageDynamicPayload, WidgetDynamicError
      vers le sous-package shared/. Mettre à jour tous les imports.
      Conserver le package `com.tchalanet.server.features.pagemodel.shared`.

- [x] C.5 Organiser `features/pagemodel/dynamic/`
      Fichier(s) :
      `features/pagemodel/dynamic/PageModelDynamicProvider.java`
      `features/pagemodel/dynamic/PageModelDynamicResolver.java`
      `features/pagemodel/dynamic/providers/PublicNewsProvider.java`
      `features/pagemodel/dynamic/providers/CashierOverviewProvider.java`
      Action : DÉPLACER (depuis features/pagemodel/ racine)
      Dépend de : C.4
      Points : 2
      Notes : Déplacer les fichiers existants dans le sous-package dynamic/.
      PageModelDynamicProvider et Resolver sont déjà dans features/pagemodel/ à la racine —
      déplacer vers dynamic/ et mettre à jour tous les imports.
      Règle des 3 : 4 classes dans providers/ → sous-package justifié.

- [x] C.6 Supprimer `features/pagemodelruntime/` entier
      Fichier(s) : `features/pagemodelruntime/PageModelRuntimeService.java`,
      `features/pagemodelruntime/PageModelRuntimeResponse.java`
      Action : SUPPRIMER
      Dépend de : C.1 (migré dans slice public/)
      Points : 1
      Notes : Vérifier qu'aucune autre classe n'importe depuis pagemodelruntime/ avant suppression.

**Quality Gate C :**

- [x] `./mvnw clean compile` passe
- [x] `GET /api/v1/public/pagemodel/public.home` → HTTP 200 (test manuel ou curl)
- [x] `GET /api/v1/tenant/pagemodel/dashboard` avec token CASHIER → HTTP 200
- [x] Aucun import `core.pagemodel.infra.init.PageModelBootstrapService` (supprimé)
- [x] Aucun import `features.pagemodelruntime.*` (supprimé)

---

## Groupe D — Corrections techniques (Phase 4)

> Corrections de convention et de sécurité. Peuvent être faites en parallèle de C.

---

- [x] D.1 Fix `PageModelPersistenceAdapter.list()` — cross-tenant
      Fichier(s) : `core/pagemodel/infra/persistence/PageModelPersistenceAdapter.java`
      Action : MODIFIER
      Dépend de : A.1
      Points : 1
      Notes : Remplacer `repo.findAll()` par `repo.findAllByDeletedAtIsNull()`
      (RLS filtre par tenant automatiquement via `BaseTenantEntity`).
      Ajouter la méthode dans `PageModelJpaRepository` si absente.

- [x] D.2 Fix import `PageModelType` dans `PageModelTemplateSeedRunner`
      Fichier(s) : `catalog/pagemodeltemplate/internal/init/PageModelTemplateSeedRunner.java`
      Action : MODIFIER
      Dépend de : B.2
      Points : 1
      Notes : Changer :
      `import com.tchalanet.server.features.pagemodel.PageModelType;`
      → `import com.tchalanet.server.core.pagemodel.domain.model.PageModelType;`
      Violation catalog/ → features/ résolue.

- [x] D.3 Vérifier + créer policy RLS sur table `page_model`
      Fichier(s) : `src/main/resources/db/migration/V40__rls_policies.sql` (lecture),
      `src/main/resources/db/migration/V52__pagemodel_rls.sql` (création si absente)
      Action : MODIFIER (conditionnel — si policy absente)
      Dépend de : A.1
      Points : 2
      Notes : Lire V40 et V41 pour vérifier si `page_model` a une policy RLS.
      Si absente :
      CREATE POLICY tenant_isolation ON page_model
      USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
      Pattern canonique : même structure que V40 pour les tables tenantées.
      Ne créer V52 QUE si la policy est confirmée absente (éviter doublon).

- [x] D.4 Créer `PageModelId` TypedId
      Fichier(s) : `core/pagemodel/domain/model/PageModelId.java`
      Action : CRÉER
      Dépend de : A.1
      Points : 1
      Notes : `public record PageModelId(UUID value) {}`
      Pattern standard : voir `TicketId`, `DrawId` dans core/ pour référence.
      Mettre à jour `PageModelInstance` pour utiliser `PageModelId` au lieu de UUID brut.
      `PageModelJpaAdapter` fait la conversion UUID ↔ PageModelId.

- [x] D.5 Fix `PublishPageModelHandler` — archiver le PUBLISHED précédent
      Fichier(s) : `core/pagemodel/application/command/handler/PublishPageModelHandler.java`
      Action : MODIFIER
      Dépend de : A.1
      Points : 2
      Notes : Invariant métier : 1 seul instance PUBLISHED par (tenant, logicalId).
      Avant de publier la nouvelle instance, trouver l'ancienne PUBLISHED via ReadPort
      et appeler .markArchived() + save().
      Utiliser `@TchTx` pour garantir l'atomicité des deux opérations.
      ✅ PRÉ-IMPLÉMENTÉ : PublishPolicy.apply() gère déjà cet invariant.
      Vérifier uniquement que @TchTx est utilisé à la place de @Transactional.

- [x] D.6 Ajouter validation de schema dans UpsertPageModelHandler
      Fichier(s) : `core/pagemodel/application/command/handler/UpsertPageModelHandler.java`
      `core/pagemodel/domain/exception/PageModelSchemaViolationException.java` (CRÉER)
      Action : MODIFIER
      Dépend de : D.2
      Points : 2
      Notes : 1. Injecter PageModelTemplateCatalog dans UpsertPageModelHandler 2. Charger le template : catalog.findByLogicalId(cmd.logicalId()) 3. Si template.schema non-null et non-vide (!) → valider cmd.modelJson() contre template.schema 4. Si invalide → lever PageModelSchemaViolationException (hérite de PageModelDomainException) 5. Si schema vide ({}) → aucune validation (acceptable, activé progressivement par template)
      Bibliothèque recommandée : networknt/json-schema-validator (déjà dans le classpath si utilisé ailleurs)
      Vérifier d'abord : grep -r "json-schema" tchalanet-server/pom.xml
      Format exception : code=SCHEMA_VIOLATION, violations=[{ path, message }]

- [x] D.7 Confirmer séparation pagemodel / i18n — cleanup résidu frontend
      Fichier(s) : `libs/shared/types/src/lib/page.model.ts`
      `libs/shared/data-access/page/src/.../page.effects.ts`
      `libs/shared/facades/src/lib/i18n.facade.ts`
      Action : VÉRIFIER + NETTOYER (hors scope #90 backend — à planifier dans proposal frontend)
      Dépend de : —
      Points : 1
      Notes :
      Vérification effectuée en session 2026-04-24 :
      ✅ Backend PROPRE : 0 occurrence i18nOverrides dans core/, catalog/, features/, JSON templates
      ⚠️ Frontend résidu INACTIF : - PageModel.i18n: Record<string,Record<string,string>> dans le type TypeScript - PageEffects passe page.i18n à initFromPage() mais I18nFacade ignore ce paramètre - I18nFacade.applyOverrides() est un stub (console.log uniquement)
      ✅ MergedTranslateLoader appelle backendPath?lang= (catalog/i18n/) correctement
      ✅ me.api.ts.i18nOverrides est dans /v1/me/context, pas lié au pagemodel
      Action dans cette session : NÉ — backend scope only
      Action future (proposal frontend) : 1. Supprimer PageModel.i18n du type TypeScript 2. Retirer page.i18n du call initFromPage dans PageEffects 3. Supprimer applyOverrides stub de I18nFacade

**Quality Gate D :**

- [x] `./mvnw clean verify` passe
- [x] `grep -r "features.pagemodel.PageModelType" --include="*.java"` → 0 résultats
- [x] Policy RLS confirmée active sur `page_model` (V40, V41 ou V52)
- [x] `PageModelId.java` existe dans `core/pagemodel/domain/model/`

---

## Groupe E — Providers dynamiques manquants (Phase 5)

> Brancher les données réelles dans les widgets dynamiques.
> Dépend de Groupe C entier (structure dynamic/ en place).
> Chaque provider = 2 points.

---

- [x] E.1 Créer `DrawsProvider` (source: `results_by_game`)
      Fichier(s) : `features/pagemodel/dynamic/providers/DrawsProvider.java`
      Action : CRÉER
      Dépend de : C.5
      Points : 2
      Notes :
      implements PageModelDynamicProvider
      supports() : source = "results_by_game" ou "draws"
      load() : QueryBus → GetRecentDrawResultsQuery (core/drawresult)
      Retourne les derniers résultats par tirage : { draws: [{ name, results[], drawnAt }] }
      LIMIT 4 (un résultat par tirage majeur : Miami, NY, TX, GA)
      Graceful : si no results → retourner empty list, pas d'exception

- [x] E.2 Créer `PlansProvider` (source: `plans`)
      Fichier(s) : `features/pagemodel/dynamic/providers/PlansProvider.java`
      Action : CRÉER
      Dépend de : C.5
      Points : 2
      Notes :
      implements PageModelDynamicProvider
      supports() : source = "plans"
      load() : PlanCatalog.findAllActive() (catalog/)
      Retourne : { plans: [{ code, name, price, features[] }] }
      Interface catalog : vérifier si PlanCatalog existe dans catalog/plan/ — sinon créer.

- [x] E.3 Créer `HeroProvider` (source: `hero`)
      Fichier(s) : `features/pagemodel/dynamic/providers/HeroProvider.java`
      Action : CRÉER
      Dépend de : C.5
      Points : 2
      Notes :
      implements PageModelDynamicProvider
      supports() : source = "hero"
      load() : données enrichies contextuelles
      Si contexte tenant disponible → enrichir avec stats du tenant
      Sinon → retourner props statiques enrichis (CTA links, description)
      Format : { backgroundUrl?, ctaLinks[], tagline?, stats? }
      Ce provider est léger — pas de DB query lourde, principalement config.

- [x] E.4 Fix `CashierOverviewProvider` — remplacer le stub par des données réelles
      Fichier(s) : `features/pagemodel/dynamic/providers/CashierOverviewProvider.java`
      Action : MODIFIER
      Dépend de : C.5
      Points : 2
      Notes :
      Supprimer le stub `ticketsToday: 0, sessionOpen: true` hardcodé.
      Brancher sur core/sales :
      QueryBus → GetPosSessionTotalsQuery(sessionId, date)
      ou lire directement via un TicketReaderPort si query existe
      Format retourné : { ticketsToday, totalAmount, sessionOpen, sessionId, openedAt? }
      Fallback : si QueryBus retourne null (session non ouverte), retourner
      { ticketsToday: 0, totalAmount: 0, sessionOpen: false }

**Quality Gate E :**

- [x] `GET /api/v1/public/pagemodel/public.home` → `dynamic.widgets` non vide (si draws/plans existent en base)
- [x] `dynamic.errors` vide ou limité à des widgets non encore implémentés côté Angular
- [x] `GET /api/v1/public/pagemodel/private.dashboard.cashier` → `dynamic.widgets.overview` non null

---

## Completion Checklist

- [x] Groupe A complet — `./mvnw clean compile` passe
- [x] Groupe B complet — 0 doublon de types entre features/ et core/
- [x] Groupe C complet — Slices créées, pagemodelruntime/ supprimé
- [x] Groupe D complet — RLS OK, TypedId OK, handlers corrects
- [x] Groupe E complet — 4 providers branchés
- [x] `./mvnw clean verify` passe (tests existants non cassés)
- [x] Aucun conflit `@RequestMapping` dans le module pagemodel
- [x] Aucune violation hexagonale `core/ → features/`
- [x] Documentation mise à jour : `FEATURE_PAGEMODEL.md` + `PAGEMODEL-ARCHITECTURE-CIBLE.md`
- [x] Prêt pour `/openspec-archive 90-finish-pagemodel-migration-20260424`
