# tasks — limits-contextual-config-v1

## Conventions à respecter

Chaque slice cible un projet précis. Respecter strictement les conventions du projet touché :

**Backend** (`tchalanet-server`)
- Hexagonal : pas d'appel direct entre `core/*` — tout passe par ports/bus. `features/*` peut appeler plusieurs cores.
- Records Java pour les DTOs de réponse, `@Transactional` uniquement sur les command handlers, pas sur les query handlers.
- Pas de logique métier dans les controllers.

**Web** (`tchalanet-web`)
- Angular 20 : signals + `resource`/`rxResource`, `OnPush` change detection partout.
- Pas de `ngModel` ni `EventEmitter` dans les composants signals.
- Chaque composant est `standalone: true`. Librairie `libs/ui/console/` pour les composants réutilisables cross-features.
- **Mobile-first obligatoire** : tout nouveau composant est conçu pour 360 dp en premier (cible POS Sunmi V2), puis s'étend vers desktop avec des media queries progressives. Pas de layout qui suppose un écran large par défaut.

**Mobile** (`tchalanet-mobile`)
- Architecture MVVM feature-first : `data/services/` (stateless, appelle Dio) → `data/models/` (DTOs + domain models) → `presentation/view_models/` (Riverpod providers) → `presentation/views/` (widgets).
- `FutureProvider.autoDispose.family` pour les données par paramètre écran ; `FutureProvider` non-autoDispose pour les données session.
- Views ne contiennent aucune logique métier, pas de Dio, pas de parsing JSON.
- Tous les textes visibles passent par `translations.translate('...')` — aucun texte hardcodé.
- `flutter analyze` et `flutter test` doivent passer après chaque slice mobile.

**i18n (web et mobile)** — toute clé nouvelle doit exister dans **ht**, **fr** et **en**.

---

## Slice 0 — Backend : archivage draw_exposure

Les lignes `draw_exposure` s'accumulent indéfiniment ; le cycle de vie du draw est la référence naturelle de purge — une fois le draw archivé, ses projections d'exposition ne sont plus jamais consultées par les queries runtime de `limitpolicy`. L'archivage suit donc le draw, pas une période calendaire.

**Important** : `draw_exposure` est une projection stateful — on ne peut pas soft-delete des données d'accumulation. On hard-delete après archivage du draw (les rows ne valent plus rien une fois le draw terminé).

- [ ] Créer `core/limitpolicy/internal/infra/archive/DrawExposureArchiveDatasetProvider`
  - `key()` → `ArchiveDatasetKey.of("draw_exposure", "Draw Exposure")`
  - `plan(drawArchiveRef)` → count des rows `draw_exposure` dont le draw associé est dans l'état `ARCHIVED` (join sur `draw.status = 'ARCHIVED'` ou via `draw.scheduled_at < cutoffDate` passée en paramètre par le scheduler d'archive)
  - `export(drawArchiveRef)` → stream des rows éligibles, puis **hard-delete** après export confirmé — pas de `deleted_at`, pas de soft-delete
  - `generateLookupRows()` → liste vide (pas de lookup individuel sur l'exposition)
- [ ] Créer `DrawExposureArchiveJdbcRepository` : `countByArchivedDraw()`, `streamByArchivedDraw()`, `deleteByArchivedDraw()`
- [ ] **Preuve qu'aucune query runtime ne dépend des lignes archivées** :
  - Lister toutes les queries SQL de `ExposureProjectorAdapter` et `ExposureQueryAdapter` — elles filtrent toutes sur `draw_id = ?` (draw OPEN/CLOSED en cours)
  - Aucune query ne fait un scan global sans filtre `draw_id`
  - Ajouter dans `DrawExposureArchiveDatasetProviderTest` : après delete, rejouer `ExposureQueryAdapter.getExposure(archivedDrawId)` → retourne vide (pas d'erreur, pas de résultat fantôme)

## Slice 1 — Backend : projection SELLER_TERMINAL + BFF endpoints

**Clarification : deux usages distincts de SELLER_TERMINAL**

- **Projection** (ce Slice) = `DrawExposureJpaEntity` rows avec `scope_type = SELLER_TERMINAL`. Accumulées automatiquement dans `ExposureProjectorAdapter` à chaque `TicketPlacedEvent`. Orthogonal à la configuration.
- **Configuration** = `LimitAssignment` rows avec `scope_type = SELLER_TERMINAL`. Posées explicitement par l'admin via `UpsertLimitAssignmentCommand`. Orthogonal à la projection.

- [ ] `ExposureProjectorAdapter.scopesFor()` : ajouter scope SELLER_TERMINAL
  - `if (event.context().sellerTerminalId() != null) scopes.add(LimitScopeRef.sellerTerminal(...))`
  - `sellerTerminalId` est déjà dans `TicketContextPayload`
  - Pas de backfill : seuls les nouveaux tickets post-déploiement sont projetés par terminal
- [ ] BFF admin : créer `features/tenantadmin/draw/TenantAdminDrawOverviewController`
  - `GET /admin/draws/{drawId}/overview`
  - agrège : draw (core/draw) + channel info (catalog) + résultat + top selections (core/sales) + exposure DRAW_CHANNEL (core/limitpolicy) + `effectiveLimits` résolu
  - `effectiveLimits` : résolution TENANT + DRAW_CHANNEL uniquement — la vue admin n'est pas contextualisée à un terminal ; ne jamais inclure une résolution SELLER_TERMINAL dans cette réponse
  - retourne `AdminDrawOverviewResponse` (record) — même shape que draw OPEN ou CLOSED ; le champ `exposureAlerts` est `[]` si draw CLOSED, pas absent
- [ ] BFF POS : ajouter `@GetMapping("/{drawId}/detail")` dans `features/pos/draws/PosDrawsController` (controller existant)
  - `GET /tenant/cashier/draws/{drawId}/detail`
  - `sellerTerminalId` résolu depuis `TchRequestContext.sellerTerminalIdRequired()` — jamais depuis un query param client
  - agrège : draw info + top selections scope SELLER_TERMINAL + exposure SELLER_TERMINAL
  - retourne `PosDrawDetailResponse` (record) — **toujours le même shape**, draw OPEN ou CLOSED
  - champ `exposure.active = true` si draw OPEN et expositions calculées, `false` si draw CLOSED (le client lit ce flag, pas le statut du draw)

## Slice 2 — Web : composant LimitPolicyBlockComponent

Conventions : composant standalone `OnPush`, signals, librairie `libs/ui/console/`. **Mobile-first** : layout en colonne à 360 dp, une ligne par ruleKey ; s'élargit en grille à partir de 600 dp.

- [ ] Créer `libs/ui/console/src/lib/limit-policy-block/limit-policy-block.component.ts`
  - Règles groupées par intention (pas de liste technique plate) :
    - **Vente** : `MAX_STAKE_PER_LINE`, `MAX_LINES_PER_TICKET`, `MAX_STAKE_PER_TICKET`
    - **Restrictions** : `BLOCK_BET_TYPE`, `BLOCK_SELECTION_PER_DRAW`
    - **Exposition** : `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW`, `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW`
  - Les règles Exposition peuvent être dans une zone repliable "Avancé" (moins courantes)
  - Chaque champ est nullable ; valeur héritée + provenance affichées en grisé (`Hérite du tenant · 500 G` / `Hérite du tirage · 300 G`)
  - Ne jamais afficher les scores internes (10/30/60)
  - Emits `LimitPolicyOverride` (map ruleKey → valeur | null)
  - Chaque groupe (Vente / Restrictions / Exposition) est individuellement collapsible — expand/collapse indépendants
  - Input `defaultExpandedGroups = input<LimitGroup[]>([])` — le contexte parent contrôle quels groupes sont ouverts par défaut ; l'état est un signal local, non persisté
    - Provisioning tenant (platform) → `[]` tout replié (écran dense, limites optionnelles)
    - Setup tenant config (admin) → `['VENTE']` seulement (config la plus fréquente)
    - Draw channel detail → `['VENTE', 'RESTRICTIONS']`
    - Seller terminal create/edit → `[]` tout replié (exception, pas la règle)
- [ ] Charger les specs depuis `api.listRules()` pour libellés/paramsTemplate

## Slice 3 — Web : intégration dans Setup tenant config

Conventions : page existante, ajouter une section signal-driven `OnPush`.

- [ ] Ajouter section "Limites par défaut" dans la page setup/config tenant
  - scope TENANT
  - load : `api.listAssignments('TENANT')`
  - save : upsert/delete par champ modifié

## Slice 4 — Web : intégration dans Draw Channel config

Conventions : même pattern que Slice 3, scope différent.

- [ ] Ajouter section "Limites" dans la page draw channel detail/edit
  - scope DRAW_CHANNEL, targetId = channelId
  - afficher valeur héritée du tenant en grisé
  - load : `api.listAssignments('DRAW_CHANNEL', channelId)`
  - save : upsert/delete par champ modifié

## Slice 5 — Web : intégration dans Seller Terminal

Conventions : même pattern, ruleKeys TICKET uniquement.

- [ ] Ajouter section "Limites" dans la page seller terminal création/édition
  - scope SELLER_TERMINAL, targetId = terminalId
  - ruleKeys : TICKET uniquement (pas EXPOSURE)
  - afficher valeur héritée du tenant en grisé

## Slice 6 — Web : bouton "Bloke nimero" dans le détail tirage

Conventions : `AdminDrawDetailPage` existante, ajouter un `input` optionnel au dialog existant.

- [ ] Ajouter `channelId = input<string | undefined>()` dans `BlockNumberQuickDialogComponent`
  - masquer le picker de tirage dans le dialog quand `channelId()` est défini
- [ ] Ajouter bouton "Bloke nimero" dans le header d'actions de `AdminDrawDetailPage`
  - visible seulement si le draw est OPEN
  - ouvre `BlockNumberQuickDialogComponent` avec `drawChannelId` pré-rempli

## Slice 7 — Web : widget "Numéros à risque" dans le détail tirage

Conventions : `AdminDrawDetailPage` charge le BFF `/admin/draws/{drawId}/overview` comme **source unique** — pas de second appel API pour l'exposition.

- [ ] Ajouter section "Numéros à risque" dans `DrawDetailActivityComponent`
  - lire `exposureAlerts` depuis la réponse `AdminDrawOverviewResponse` déjà chargée dans `AdminDrawDetailPage` (signal/resource existant) — ne pas créer de `rxResource` séparé
  - masqué si `exposureAlerts` vide ou si `effectiveLimits` ne contient aucune règle `MAX_STAKE_EXPOSURE` configurée (`limitConfigured: false`)
  - afficher chips numéros avec codage couleur du ratio fourni par le BFF : < 50 % vert, 50–79 % orange, ≥ 80 % rouge — ne pas recalculer côté frontend
  - bouton "Bloquer" à côté de chaque numéro à risque (ouvre quick dialog pré-rempli)
- **Ne pas** ajouter `getExposureAlerts()` dans `AdminLimitsApi` — les données viennent du BFF overview

## Slice 8 — Simplification menu limits (backend pagemodel + web)

La navigation est définie à deux endroits qui doivent rester synchrones.

**Backend — `tchalanet-app/src/main/resources/pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json`**

- [ ] Dans la section `limits` (navigationDrawer), retirer les enfants `limits-global`, `limits-draw`, `limits-seller`
  - Garder : `limits-overview` (`/app/admin/limits`, exact) + `limits-number` (`/app/admin/limits/number`)
  - Les routes `/app/admin/limits/global`, `/app/admin/limits/draw`, `/app/admin/limits/seller-terminal` restent actives côté Angular — seulement retirées du nav
- [ ] Dans la section `sellers` (navigationDrawer), retirer l'enfant `sellers-limits` (pointait vers `/app/admin/limits/seller-terminal` — désormais intégré dans la page seller terminal)

**Web — `tchalanet-web/libs/web/shell/src/lib/private-shell/private-navigation.model.ts`**

- [ ] Retirer `limits-global`, `limits-draw`, `limits-seller` de `TENANT_ADMIN_NAVIGATION`
  - Miroir exact des retraits backend — les deux fichiers doivent refléter la même structure
- [ ] Retirer `sellers-limits` de la section sellers dans le même fichier

**Les deux fichiers modifiés dans le même commit** pour éviter toute désynchronisation.

- [ ] Ajouter lien "Vue avancée →" sur la page `/limits` (overview) vers `/limits/draw` pour accès admin exceptionnel

## Slice 9 — i18n web

Fichiers : `tchalanet-web/libs/shared-assets/public/assets/i18n/{ht,fr,en}/feature-admin.json`

Toutes les clés ci-dessous dans les 3 locales (ht = Créole haïtien — langue première) :

- [ ] `feature-admin.limits.blockComponent.*` — libellés champs ruleKey (MAX_STAKE_PER_LINE, MAX_LINES_PER_TICKET, MAX_STAKE_PER_TICKET, BLOCK_BET_TYPE), placeholder héritage, label section
- [ ] `feature-admin.draws.detail.blockNumber` — bouton "Bloke nimero" header tirage
- [ ] `feature-admin.draws.detail.hotNumbers.*` — titre "Numéros à risque", colonne ratio, état vide, état non configuré
- [ ] `feature-admin.draws.detail.blockFromRisk` — bouton "Bloquer" dans widget numéros à risque
- [ ] `feature-admin.tenantSetup.limitsSection.*` — titre section, description inherit
- [ ] `feature-admin.drawChannel.limitsSection.*` — titre section, description inherit
- [ ] `feature-admin.sellerTerminal.limitsSection.*` — titre section, description inherit

## Slice 10 — Mobile : widget "Nimero cho" dans SellerTerminalDrawReportPage

Conventions : MVVM feature-first, Riverpod, i18n obligatoire (ht/fr/en), `flutter analyze` + `flutter test` après.

**Data layer**

- [ ] Créer `lib/features/cashier/home/data/models/pos_draw_detail_models.dart`
  - `PosDrawDetailResponse` — DTO JSON : `topSelections: List<PosDrawTopSelection>`, `exposureAlerts: List<PosDrawExposureAlert>?`
  - `PosDrawTopSelection` — `selectionKey: String`, `stakeTotalCents: int`, `salesCount: int`
  - `PosDrawExposureAlert` — `selectionKey: String`, `stakeTotalCents: int`, `limitCents: int`, `ratio: double`
- [ ] Créer `lib/features/cashier/home/data/services/pos_draw_detail_service.dart`
  - `PosDrawDetailService(Dio)` — stateless
  - `fetchDrawDetail(drawId)` → `GET /tenant/cashier/draws/{drawId}/detail`
  - mappe la réponse en `PosDrawDetailResponse` ; null-guard sur `exposureAlerts`

**ViewModel layer**

- [ ] Ajouter `posDrawDetailServiceProvider` dans `cashier_home_providers.dart`
- [ ] Ajouter `posDrawDetailProvider = FutureProvider.autoDispose.family<PosDrawDetailResponse?, String>((ref, drawId) async {...})`
  - retourne `null` si le tirage n'est pas OPEN (ne pas bloquer le rendu de la page)

**View layer**

- [ ] Ajouter `_DrawTopSelectionsBlock` dans `seller_terminal_draw_report_page.dart`
  - `ref.watch(posDrawDetailProvider(drawId))` → AsyncValue
  - visible seulement si tirage OPEN (`isOpen == true`) et données non nulles
  - liste les `topSelections` avec numéro + montant formaté
  - si `exposureAlerts` présent et non vide : afficher chip coloré avec le ratio fourni par le BFF (< 50 % = vert, 50–79 % = orange, ≥ 80 % = rouge) — ne pas recalculer côté Flutter
  - état loading : `CircularProgressIndicator` compact inline (ne pas bloquer la page principale)
  - état error : texte discret `pos.reports.hot_numbers_error` (ne pas afficher de FeedbackState)
  - titre section : `pos.reports.hot_numbers`

## Slice 11 — i18n mobile

Fichiers : `tchalanet-mobile/assets/i18n/{ht,fr,en}/feature-seller-terminal.json`

Toutes les clés dans les 3 locales. Créole haïtien (`ht`) = langue première, rédigée en premier.

- [ ] `pos.reports.hot_numbers` — titre section numéros chauds du terminal
  - ht: `Nimero cho` / fr: `Numéros chauds` / en: `Hot numbers`
- [ ] `pos.reports.hot_numbers_error` — erreur chargement inline
  - ht: `Nou pa kapab chaje nimero cho yo` / fr: `Impossible de charger les numéros chauds` / en: `Unable to load hot numbers`
- [ ] `pos.reports.hot_numbers_empty` — état vide (draw open mais aucune vente encore)
  - ht: `Pa gen vant ankò pou tiraj sa a` / fr: `Aucune vente pour ce tirage` / en: `No sales for this draw yet`
- [ ] `pos.reports.exposure_ratio` — label ratio exposition (accessible + tooltip)
  - ht: `{ratio}% nan limit la` / fr: `{ratio}% du plafond` / en: `{ratio}% of limit`

---

## Slice 12 — Tests backend : unitaires + intégration limitpolicy (couverture 100 %)

Cible : tout le code nouveau ou modifié dans `core/limitpolicy`.

**ExposureProjectorAdapterTest** (fichier existant, compléter) :
- [ ] `scopesForIncludesSellerTerminalWhenPresent` — event avec `sellerTerminalId` → 3 scopes (TENANT, DRAW_CHANNEL, SELLER_TERMINAL)
- [ ] `scopesForExcludesSellerTerminalWhenAbsent` — event sans sellerTerminalId → 2 scopes uniquement
- [ ] `replayDoesNotDoubleCountAnyScope` — même eventId rejoué deux fois → chaque scope upsert idempotent (mock JdbcTemplate vérifie que le SQL contient `ON CONFLICT DO UPDATE` et pas `INSERT` naïf)

**LimitResolver / cascade override (nouveaux tests unitaires)** :
- [ ] `sellerTerminalOverridesDrawChannel` — ruleKey configuré TENANT=500 + DRAW_CHANNEL=300 + SELLER_TERMINAL=200 → résolu à 200
- [ ] `drawChannelOverridesTenantWhenNoTerminalOverride` — ruleKey configuré TENANT=500 + DRAW_CHANNEL=300, pas de SELLER_TERMINAL → résolu à 300
- [ ] `tenantFallsBackWhenNoOtherOverride` — ruleKey configuré TENANT=500 uniquement → résolu à 500
- [ ] `nullResultWhenNoAssignmentAtAnyScope` — aucune règle configurée → résultat null (pas d'erreur)

**DrawExposureArchiveDatasetProvider (nouveaux tests unitaires)** :
- [ ] `planCountsRowsByArchivedDraw` — JDBC stub retourne N rows pour draws archivés → `plan().count() == N`
- [ ] `exportStreamsAndHardDeletes` — export stream, puis vérifie que `deleteByArchivedDraw()` est appelé (pas de `deleted_at`, hard-delete)
- [ ] `afterDeleteQueryReturnsEmpty` — après delete, `ExposureQueryAdapter.getExposure(archivedDrawId)` retourne vide (pas d'exception)

**BFF query handlers (nouveaux tests unitaires)** :
- [ ] `adminDrawOverviewHandlerAggregatesAllDomains` — fake query bus → `AdminDrawOverviewResponse` contient draw + topSelections + exposureAlerts
- [ ] `posDrawDetailHandlerResolvesTerminalFromContext` — `sellerTerminalIdRequired()` appelé ; aucun paramètre client accepté
- [ ] `posDrawDetailHandlerReturnsExposureInactiveWhenDrawClosed` — draw CLOSED → `exposure.active == false`, topSelections présent

## Slice 13 — Tests backend Spring IT : 3 cas e2e limitpolicy

Classe à créer : `LimitPolicyRuntimeIntegrationTest extends BusinessRuntimeIntegrationTestBase`

Pattern identique à `SalesPolicyPromotionSpringIntegrationTest` : SpringBoot + Testcontainers, appels via `commandBus` / `queryBus`, assertions SQL directes si nécessaire.

**Cas 1 — Block number**
- [ ] Configurer `BLOCK_SELECTION_PER_DRAW` scope TENANT pour le numéro "34" sur le draw
- [ ] Tenter une vente avec la sélection "34"
- [ ] Assertion : `SellTicketOutcome` status = REJECTED, `BreachOutcome` ruleKey = `BLOCK_SELECTION_PER_DRAW`
- [ ] Vérifier qu'aucun ticket n'est persisted en base

**Cas 2 — Override de scope (cascade TENANT → DRAW_CHANNEL → SELLER_TERMINAL)**
- [ ] Configurer `MAX_STAKE_PER_LINE` : TENANT=500, DRAW_CHANNEL=300, SELLER_TERMINAL=100
- [ ] Tenter une vente avec mise 150 HTG (dépasse SELLER_TERMINAL=100, mais pas DRAW_CHANNEL=300)
- [ ] Assertion : sale REJECTED avec ruleKey = `MAX_STAKE_PER_LINE`, limite effective = 100
- [ ] Tenter une vente avec mise 50 HTG (sous tous les plafonds)
- [ ] Assertion : sale APPROVED

**Cas 3 — Exposition cumulative (MAX_STAKE_EXPOSURE)**
- [ ] Configurer `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` scope DRAW_CHANNEL, limite = 1 000 HTG, sélection "12"
- [ ] Vendre 9 tickets de 100 HTG sur "12" → OK (cumul = 900 HTG)
- [ ] Vendre 1 ticket de 200 HTG sur "12" → REJECTED (cumul dépasserait 1 000)
- [ ] Vérifier que le compteur DRAW_CHANNEL pour "12" est bien à 900 (pas 1 100) en base
- [ ] (optionnel) Rejouer un événement de vente déjà projeté → compteur inchangé (idempotence)

## Slice 14 — Tests e2e web (Playwright)

Fichiers dans `tchalanet-web/apps/web-e2e/src/admin-portal/`.

**Navigation limits (`limits-nav-simplification.spec.ts`)**
- [ ] Après simplification du nav : `/app/admin/limits/global` absent du sidenav
- [ ] `/app/admin/limits/draw` absent du sidenav
- [ ] `/app/admin/limits/seller-terminal` absent du sidenav (section limits ET section sellers)
- [ ] `/app/admin/limits` (overview) présent et actif
- [ ] `/app/admin/limits/number` présent et actif
- [ ] Les routes `/limits/global` et `/limits/draw` restent routables (navigation directe → 200, pas de redirect)
- [ ] Tester sur viewport 360 dp (mobile) ET 1280 dp (desktop) — même sidenav, même résultat
- Sélecteurs : `href` stable, pas de label text (convention existante)

**LimitPolicyBlockComponent (`limit-policy-block.spec.ts`)**
- [ ] Champ vide → placeholder affiche la valeur héritée et sa provenance (`Hérite du tenant · 500 G`)
- [ ] Saisie d'une valeur → placeholder disparaît, composant émet `LimitPolicyOverride`
- [ ] Effacer la valeur → revient à l'état hérité
- [ ] Sur viewport 360 dp : chaque ruleKey sur une ligne, pas d'overflow horizontal

**Draw detail — Bloke nimero (`draw-detail-block-number.spec.ts`)**
- [ ] Draw OPEN : bouton "Bloke nimero" visible dans le header
- [ ] Clic → dialog ouvre avec channel pré-sélectionné (pas de picker draw)
- [ ] Draw CLOSED : bouton absent

**Draw detail — Numéros à risque (`draw-detail-exposure.spec.ts`)**
- [ ] Section "Numéros à risque" visible quand `exposureAlerts` non vide dans la réponse BFF
- [ ] Section absente quand `exposureAlerts` vide ou `limitConfigured: false`
- [ ] Ratio affiché avec barre/chip pour chaque entrée

## Slice 15 — Tests e2e Python (pytest, `testing/e2e/`)

Fichier à créer : `tests/business_critical/test_limit_policy_scenarios.py`

Marqueurs pytest : `@pytest.mark.L2`, `@pytest.mark.business_critical`, `@pytest.mark.slow`

Pattern : même base que `test_business_day_scenarios.py` — provision tenant + seller terminal via API, ouvre un draw, puis appelle les endpoints REST directement via `ApiClient` / `httpx`.

**Cas 1 — Bloquer un numéro via API admin**
- [ ] Provision tenant + 1 seller terminal, ouvre un draw
- [ ] Admin : `POST /admin/policies/limits/assignments` → `BLOCK_SELECTION_PER_DRAW` scope TENANT, sélection "34"
- [ ] Seller terminal tente de vendre "34" → réponse 422 ou outcome REJECTED
- [ ] Vérifier : aucun ticket créé (liste tickets vide ou ticket absent)
- [ ] Admin : `GET /admin/draws/{drawId}/overview` → `exposureAlerts` ne contient pas "34" (pas de stake)

**Cas 2 — Override de scope TENANT → DRAW_CHANNEL → SELLER_TERMINAL**
- [ ] Configurer `MAX_STAKE_PER_LINE` : TENANT=500, DRAW_CHANNEL=300, SELLER_TERMINAL=100 via assignments API
- [ ] Seller terminal vend avec mise 150 HTG → REJECTED, `breachRuleKey = MAX_STAKE_PER_LINE`, `effectiveLimit = 100`
- [ ] Seller terminal vend avec mise 50 HTG → APPROVED
- [ ] Admin : `GET /admin/draws/{drawId}/overview` → `effectiveLimits.MAX_STAKE_PER_LINE.resolvedValue = 300`, `resolvedScope = DRAW_CHANNEL`
  - Le BFF admin n'est pas contextualisé à un terminal ; la résolution s'arrête à DRAW_CHANNEL (valeur 300)
  - La limite SELLER_TERMINAL=100 n'est visible que via `GET /tenant/cashier/draws/{drawId}/detail` (BFF POS, contextualisé au terminal)

**Cas 3 — Exposition cumulative + BFF POS detail**
- [ ] Configurer `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` scope DRAW_CHANNEL, limite = 1 000 HTG, sélection "12"
- [ ] Vendre 9 tickets à 100 HTG sur "12" → tous APPROVED
- [ ] `GET /tenant/cashier/draws/{drawId}/detail` → `topSelections[0].selectionKey = "12"`, `exposure.active = true`, `alerts[0].ratio ≈ 0.90`
- [ ] Tenter une 10e vente à 200 HTG → REJECTED
- [ ] `GET /tenant/cashier/draws/{drawId}/detail` → compteur inchangé à 900 (pas 1 100)
- [ ] Vérifier qu'aucun `sellerTerminalId` en query param n'est nécessaire (Request Context)

## Slice 16 — Perf Locust (nouvelles tâches dans `testing/e2e/loadtest/`)

**`CashierUser`** (`loadtest/users.py`) :
- [ ] Ajouter tâche `@tag("read") @task(1) def read_draw_detail(self) -> None`
  - appel `GET /tenant/cashier/draws/{drawId}/detail` pour un draw ouvert aléatoire
  - tagué `read` (même bucket que `list_available_draws`)
  - objectif : mesurer p95 < 300 ms sous la charge nominale (20 CashierUsers)

**`AdminUser`** (`loadtest/users.py`) — à créer si absent :
- [ ] Classe `AdminUser(User)` avec `weight = 1`, `wait_time = between(1.0, 3.0)`
- [ ] Tâche `@tag("admin_read") @task(1) def read_draw_overview(self) -> None`
  - appel `GET /admin/draws/{drawId}/overview` pour un draw du jour
  - objectif : p95 < 500 ms (BFF agrège plusieurs domaines)

**Seuils à valider avant merge** :
- `GET /tenant/cashier/draws/{drawId}/detail` : p95 < 300 ms, error rate < 0.1 %
- `GET /admin/draws/{drawId}/overview` : p95 < 500 ms, error rate < 0.1 %
- Aucune régression sur `sell_basket` (p95 avant/après ± 10 %)
