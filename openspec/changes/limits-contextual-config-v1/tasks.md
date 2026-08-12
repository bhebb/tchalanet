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

**Mobile** (`tchalanet-mobile`)
- Architecture MVVM feature-first : `data/services/` (stateless, appelle Dio) → `data/models/` (DTOs + domain models) → `presentation/view_models/` (Riverpod providers) → `presentation/views/` (widgets).
- `FutureProvider.autoDispose.family` pour les données par paramètre écran ; `FutureProvider` non-autoDispose pour les données session.
- Views ne contiennent aucune logique métier, pas de Dio, pas de parsing JSON.
- Tous les textes visibles passent par `translations.translate('...')` — aucun texte hardcodé.
- `flutter analyze` et `flutter test` doivent passer après chaque slice mobile.

**i18n (web et mobile)** — toute clé nouvelle doit exister dans **ht**, **fr** et **en**.

---

## Slice 0 — Backend : archivage draw_exposure

- [ ] Créer `core/limitpolicy/internal/infra/archive/DrawExposureArchiveDatasetProvider`
  - `key()` → `ArchiveDatasetKey.of("draw_exposure", "Draw Exposure")`
  - `plan()` → count par `last_event_at` dans la période (ou join sur `draw.scheduled_at`)
  - `export()` → stream rows + soft-delete (`deleted_at = now()`) après export
  - `generateLookupRows()` → liste vide (pas de lookup individuel sur l'exposition)
- [ ] Créer `DrawExposureArchiveJdbcRepository` (countByPeriod, streamByPeriod, softDelete)
- [ ] Vérifier que les index existants (`WHERE deleted_at IS NULL`) couvrent bien les queries actives après purge

## Slice 1 — Backend : projection SELLER_TERMINAL + BFF endpoints

- [ ] `ExposureProjectorAdapter.scopesFor()` : ajouter scope SELLER_TERMINAL
  - `if (event.context().sellerTerminalId() != null) scopes.add(LimitScopeRef.sellerTerminal(...))`
  - `sellerTerminalId` est déjà dans `TicketContextPayload`
- [ ] BFF admin : créer `features/tenantadmin/draw/TenantAdminDrawOverviewController`
  - `GET /admin/draws/{drawId}/overview`
  - agrège : draw (core/draw) + channel info (catalog) + résultat + top selections (core/sales) + exposure DRAW_CHANNEL (core/limitpolicy, seulement si OPEN)
  - retourne `AdminDrawOverviewResponse` (record)
- [ ] BFF POS : ajouter `@GetMapping("/{drawId}/detail")` dans `features/pos/draws/PosDrawsController` (controller existant)
  - `GET /tenant/cashier/draws/{drawId}/detail`
  - agrège : draw info + top selections scope SELLER_TERMINAL + exposure SELLER_TERMINAL
  - retourne `PosDrawDetailResponse` (record) — seulement si draw OPEN et sellerTerminalId présent dans le contexte

## Slice 2 — Web : composant LimitPolicyBlockComponent

Conventions : composant standalone `OnPush`, signals, librairie `libs/ui/console/`.

- [ ] Créer `libs/ui/console/src/lib/limit-policy-block/limit-policy-block.component.ts`
  - un champ nullable par ruleKey TICKET (`MAX_STAKE_PER_LINE`, `MAX_LINES_PER_TICKET`, `MAX_STAKE_PER_TICKET`) et pour `BLOCK_BET_TYPE`
  - valeur héritée affichée en grisé quand champ vide (`= hérite : 500 HTG`)
  - emits `LimitPolicyOverride` (map ruleKey → valeur | null)
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

Conventions : `DrawDetailActivityComponent` existant, ajouter une section `resource`-driven.

- [ ] Ajouter section "Numéros à risque" dans `DrawDetailActivityComponent`
  - appel `getExposureAlerts(drawId, channelId, 10)` via `rxResource`
  - masqué si aucune règle `MAX_STAKE_EXPOSURE` n'est configurée pour ce channel (ratio null)
  - afficher chips numéros avec barre de progression ou couleur (vert → orange → rouge)
  - bouton "Bloquer" à côté de chaque numéro à risque (ouvre quick dialog pré-rempli)
- [ ] Ajouter `getExposureAlerts(drawId, channelId, limit)` dans `AdminLimitsApi`

## Slice 8 — Web : simplification menu limits

- [ ] Retirer `global` et `draw` du sidenav `TENANT_ADMIN_NAVIGATION` (garder les routes actives)
- [ ] Menu limits : garder `overview` (lecture seule audit) + `number` (bloke nimero)
- [ ] Ajouter lien "Vue avancée →" sur la page overview vers `/limits/draw` pour accès admin

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
  - si `exposureAlerts` présent et non vide : afficher chip coloré ratio (< 50 % = vert, < 80 % = orange, ≥ 80 % = rouge)
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
