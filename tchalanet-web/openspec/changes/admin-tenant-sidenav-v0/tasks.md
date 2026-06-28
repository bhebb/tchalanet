# Tasks — admin-tenant-sidenav-v0

> Convention : inline templates/styles (pas de .html/.scss séparés).
> Signals + store service (pas NgRx). Un service API par domaine.
> Cocher [ ] → [x] en temps réel après chaque tâche.
> Checkpoint de session : lire ce fichier en premier pour savoir où on en est.

---

## Tranche prioritaire — activation tenant + vente admin-as-seller

> Objectif : permettre à un tenant admin de rendre son espace vendable, créer un vendeur,
> agir avec un terminal vendeur, vendre un ticket et imprimer. Les autres pages admin restent
> importantes, mais ne bloquent pas cette boucle métier.

- [ ] Vérifier/compléter l'onboarding tenant en 7 étapes
  - Les 7 étapes doivent être visibles, persistées et relues depuis readiness/config existants.
  - Chaque étape doit afficher état done/missing/error, CTA clair, loading/error/empty.
  - Le tenant admin doit pouvoir revenir à la checklist sans perdre les saisies.
  - La fin de l'onboarding doit rendre le tenant prêt pour génération/ouverture/vente.
- [ ] Compléter la page Maryaj gratis
  - Page dédiée `/admin/promotions/maryaj-gratis` non placeholder.
  - Expliquer l'effet métier sans jargon technique : Maryaj gratuit généré selon règle.
  - Afficher statut actif/inactif, période, règles principales, dernière modification.
  - Action d'activation via template default Maryaj gratis si endpoint disponible.
  - Si endpoint absent ou erreur, afficher état clair et action désactivée, pas de call mort.
- [ ] Créer un seller terminal depuis admin
  - Page `/admin/sellers/new` fonctionnelle.
  - Champs minimum : code terminal, nom affiché, téléphone optionnel, PIN initial, commission optionnelle.
  - Après création : retour liste vendeurs + feedback succès + possibilité d'ouvrir le mode vendeur.
  - La liste `/admin/sellers` doit afficher au moins nom/code/statut/actions.
- [ ] Permettre au tenant admin d'agir avec un seller terminal
  - Sélecteur de terminal vendeur pour l'admin tenant.
  - Contexte actif visible dans le shell ou la page vente.
  - Les appels POS doivent envoyer le contexte seller-terminal attendu par le backend.
  - Sortie du mode vendeur claire.
  - Ne pas réintroduire le rôle CASHIER ; seller terminal reste un actor/context.
- [ ] Brancher la vente admin sur les fonctions POS existantes
  - Route `/admin/tickets/sell` réutilise le flux POS web, pas une implémentation parallèle.
  - Le flux appelle les endpoints `features.pos` que le mobile utilisera : jeux, tirages vendables, preview/sell, print.
  - Test manuel minimum : sélectionner terminal, charger tirages ouverts, saisir une sélection, vendre, afficher reçu.
- [ ] Imprimer le ticket
  - Après vente, afficher le ticket/receipt avec action imprimer.
  - Utiliser le mécanisme print existant POS/receipt si disponible.
  - Vérifier que l'impression contient tenant, vendeur/terminal, code ticket, tirage, jeux, montants, date.
- [ ] Test bout-en-bout prioritaire
  - Provisionner tenant + admin.
  - Compléter onboarding 7 étapes.
  - Activer/configurer Maryaj gratis.
  - Générer et ouvrir des tirages.
  - Créer seller terminal.
  - Entrer en mode vendeur avec ce terminal.
  - Vendre un ticket Maryaj/Bolet simple.
  - Imprimer le ticket.

---

## Slice 0 — Coordination OpenSpec ✓

- [x] Marquer Slices 5–6 de `platform-superadmin-and-tenant-admin-pages/tasks.md` comme `Blocked / superseded by admin-tenant-sidenav-v0`
- [x] Localiser la source de nav — `private-navigation.model.ts` (fallback statique) + `private_shell_tenantadmin.json` (migré de `primary[]` vers `sections[]` pour être backend-driven)

---

## Slice 1 — Sidenav & routes ✓

- [x] Restructurer `admin.routes.ts` avec les chemins cibles (voir proposal.md)
  - Tous les redirects existants préservés (`onboarding`, `complete-config`, `appearance`, `more/*`, `support/*`, etc.)
  - Ajouté : `/sellers` (alias `seller-terminals`), `/sellers/new`
  - Ajouté : `/draws/matrix`, `/draws/channels` (dans `adminGeneratedDrawsRoutes`)
  - Ajouté : `/limits` (avec `?scope=` query params), `/controls/games`, `/controls/gains`, `/controls/commissions`
  - Ajouté : `/reports/sales`, `/reports/sellers`, `/reports/draws`, `/reports/exports`
  - Ajouté : `/tickets`, `/tickets/sell`, `/tickets/verify`
  - Ajouté : `/company/identity`, `/company/address`, `/company/appearance`, `/company/settings`, `/company/support`
  - Ajouté : `/help`
  - Legacy paths redirigent vers les nouveaux chemins canoniques
- [x] Mettre à jour `TENANT_ADMIN_NAVIGATION` dans `private-navigation.model.ts` — fallback statique
- [x] Mettre à jour `private_shell_tenantadmin.json` `sections[]` — source backend réelle (migré depuis `primary[]`)
- [x] `nx build` green

---

## Slice 2 — Registry d'actions

- [ ] Créer `features/private/admin/shared/domain-action.model.ts`
  - Interface `DomainActionDefinition<TStatus extends string>`
    - `id`, `labelKey`, `icon`, `endpoint?`, `bulk: boolean`, `dangerous?`, `requiresReason?`
    - `allowedStatuses?: TStatus[]`, `requiredPermissions?: string[]`
  - Type `DrawAction = 'view' | 'manualResult' | 'cancel' | 'lock' | 'unlock' | 'archive' | 'export'`
  - Type `SellerAction = 'view' | 'editCommission' | 'configureLimits' | 'resetPin' | 'block' | 'activate' | 'viewPerformance' | 'viewDailySales'`
  - Type `LimitAction = 'edit' | 'disable' | 'delete' | 'export'`
  - Registry `DRAW_ACTIONS: DomainActionDefinition<DrawAction>[]` avec `allowedStatuses` par action
  - Registry `SELLER_ACTIONS: DomainActionDefinition<SellerAction>[]`
  - Registry `LIMIT_ACTIONS: DomainActionDefinition<LimitAction>[]`

---

## Slice 3 — Accueil

- [ ] Créer `admin-home-api.service.ts` — appels parallèles :
  - `GET /tenant/reports/tenant-kpis`
  - `GET /admin/draws/today`
  - `GET /admin/draws/latest-with-results`
  - `GET /admin/commission/overview`
- [ ] Créer `admin-home.page.ts` — widgets indépendants avec skeleton/error par bloc :
  - Ventes aujourd'hui, Tickets vendus, Vendeurs actifs, Tirages ouverts
  - Résultats manquants (bandeau "À traiter")
  - Top vendeurs
  - Alertes de limites (si données disponibles)
  - Derniers tirages passés

---

## Slice 4 — Vendeurs

- [ ] Créer `admin-sellers-api.service.ts` :
  - `GET /admin/seller-terminals`
  - `POST /admin/seller-terminals`
  - `GET /admin/commission/sellers`
  - `GET /admin/policies/limits/assignments`
- [ ] Créer `sellers/admin-sellers.page.ts` — table avec colonnes : nom, code, téléphone, statut, ventes jour, tickets jour, commission, limite active, dernière activité, actions
  - Menu contextuel par ligne avec `SELLER_ACTIONS` (actions désactivées si gap backend)
  - Actions bulk : activer, bloquer, modifier commission, définir limite, exporter
- [ ] Créer `sellers/admin-seller-new.page.ts` — formulaire : code terminal, nom affiché, prénom, nom, téléphone, commission optionnelle, PIN initial

---

## Slice 5 — Tirages

- [ ] Créer `admin-draws-api.service.ts` :
  - `GET /admin/draws` + filtres (status, date)
  - `GET /admin/draws/today`
  - `GET /admin/draws/upcoming`
  - `GET /admin/draws/{drawId}`
  - `POST /admin/draws/{drawId}/lock|unlock|cancel|archive|manual-result`
- [ ] Créer `draws/admin-draws.page.ts` — table avec colonnes : tirage, date/heure, statut, total vendu, tickets, vendeurs actifs, résultat, actions
  - Actions ligne depuis `DRAW_ACTIONS` filtrées par statut
  - Actions bulk activées/désactivées selon mix de statuts sélectionnés
- [ ] Créer `draws/admin-draw-detail.page.ts` — 6 onglets :
  - **Résumé** (défaut) : KPIs + ventes par vendeur + top sélections + actions contextuelle
  - **Vendeurs** : tableau ventes/tickets/commission par vendeur
  - **Sélections** : top sélections avec exposition
  - **Résultat** : formulaire saisie ou affichage résultat
  - **Tickets** : liste tickets du tirage (secondaire)
  - **Historique** : log d'actions sur ce tirage
  - Placeholder si données non disponibles (gaps backend)

---

## Slice 6 — Limites

- [ ] Créer `admin-limits-api.service.ts` :
  - `GET /admin/policies/limits/rules`
  - `GET /admin/policies/limits/assignments`
  - `PUT /admin/policies/limits/assignments`
- [ ] Créer `limits/admin-limits.page.ts` — vue unique filtrée par `scope` (query param) :
  - `system` : lecture seule, règles imposées par la plateforme
  - `global` : limite générale de l'espace, modifiable
  - `seller` : par vendeur, modifiable
  - `number` : par numéro/sélection, modifiable
  - `game` : par jeu, modifiable
  - `draw` : par tirage, modifiable
  - Migrer contenu de `AdminLimitsPage` si réutilisable

---

## Slice 7 — Contrôles de vente

- [ ] Route `/admin/controls/games` → rework ou remap de `AdminGamesPricingPage`/`AdminGamesPage` sous nouveau path
  - Label : "Jeux & tarifs"
- [ ] Route `/admin/controls/gains` → rework ou remap de `AdminBaremesPage`
  - Label : "Gains à payer"
  - Commentaire inline : "Gains à payer = odds globales du tenant + exceptions par seller-terminal. Pas de prime."
- [ ] Route `/admin/controls/commissions` → rework ou remap de `AdminCommissionPage`
  - Label : "Commissions"
  - Commission générale tenant + commission par vendeur
- [ ] Vérifier que les anciens paths (`controls/baremes`, `controls/commission`, `controls/odds`) ont des redirects

---

## Slice 8 — Promotions

- [ ] Créer `admin-promotions-api.service.ts` :
  - `GET /admin/promotions/campaigns`
  - `POST /admin/promotions/campaigns`
  - `POST /admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate`
  - `POST /admin/promotions/campaigns/{campaignId}/rules`
- [ ] Créer `promotions/admin-promotions-maryaj.page.ts` — vue dédiée Maryaj gratis avec action d'activation
- [ ] Créer `promotions/admin-promotions.page.ts` — liste campagnes + créer campagne
- [ ] Remplacer les deux `AdminPlaceholderPage` existants (`promotions`, `promotions/maryaj-gratis`)

---

## Slice 9 — Rapports

- [ ] Créer `admin-reports-api.service.ts` :
  - `GET /tenant/reports/sales-by-period-and-game`
  - `GET /tenant/reports/tenant-kpis`
  - `GET /admin/commission/sellers`
  - `GET /admin/draws/latest-with-results`
- [ ] Créer `reports/admin-reports-sales.page.ts` — ventes par période et jeu
- [ ] Créer `reports/admin-reports-sellers.page.ts` — performance vendeurs (placeholder si gap backend)
- [ ] Créer `reports/admin-reports-draws.page.ts` — tirages avec totaux (placeholder partiel si gap backend)
- [ ] Créer `reports/admin-reports-exports.page.ts` — placeholder V0 avec liste des exports disponibles
- [ ] Remplacer `AdminTodayReportPage` sur `/reports` par la nouvelle page ventes ou rediriger

---

## Slice 10 — Tickets

- [ ] Restructurer routes tickets :
  - `/admin/tickets` → `AdminTicketsPage` (existant, remappe `support/tickets`)
  - `/admin/tickets/sell` → `AdminSellTicketPage` (existant, remappe `support/sell`) — feature flag `adminTicketSale`
  - `/admin/tickets/verify` → page verify (existante ou à créer)
- [ ] Conserver redirects `support/tickets` et `support/sell` vers nouveaux paths

---

## Slice 11 — Mon entreprise (absorbe platform-superadmin-and-tenant-admin-pages Slices 5–6)

- [ ] Créer `admin-company-api.service.ts` (absorbe `tenant-config-api.service.ts`) :
  - `GET/PUT /admin/tenant`
  - `GET/PUT /admin/tenant/address`
  - `GET/PUT /admin/tenant-config`
  - `GET /admin/tenant-config/communication`
  - `GET /admin/tenant-config/document`
  - `PUT /admin/tenant-config/internal-settings`
  - `GET/POST/PATCH /admin/theme` + presets + DELETE
- [ ] Créer `company/admin-company-identity.page.ts` — infos tenant + formulaire PUT /admin/tenant
- [ ] Créer `company/admin-company-address.page.ts` — adresse entreprise
- [ ] Créer `company/admin-company-appearance.page.ts` — thème / presets / reset (absorbe settings/appearance)
- [ ] Créer `company/admin-company-settings.page.ts` — config locale/communication/document/règles (absorbe `admin-config.page.ts`)
- [ ] Créer `company/admin-company-support.page.ts` — contact support / communication (gap `PUT /admin/tenant-config/communication`)
- [ ] Absorber `admin-runtime.page.ts` → `/company/settings/runtime` ou onglet dans settings
- [ ] Absorber `admin-games.page.ts` → déplacer sous `/controls/games` (Slice 7) ou créer lien croisé
- [ ] Absorber `admin-business-days.page.ts` → `/company/settings/business-days`
- [ ] Créer services absorbés : `runtime-api.service.ts`, `business-days-api.service.ts`, `games-admin-api.service.ts`

---

## Aide

- [ ] Route `/admin/help` → page statique ou placeholder avec liens vers articles + contact support

---

## Validation globale

- [ ] `nx build` green après chaque slice
- [ ] Aucune donnée fake hardcodée dans les templates
- [ ] Loading/error/empty states présents sur toutes les pages
- [ ] Actions désactivées ou absentes si gap backend (ne pas lancer d'appel vers endpoint inexistant)
- [ ] Redirects existants préservés (vérifier 0 lien cassé)
- [ ] Labels admin en français, aucun terme technique visible (`odds`, `limit policy`, `seller terminal`, `draw result`, `batch`, `tenant`)
- [ ] Tickets > Vendre masqué si feature flag `adminTicketSale` absent ou permission `ticket.sell` absente
