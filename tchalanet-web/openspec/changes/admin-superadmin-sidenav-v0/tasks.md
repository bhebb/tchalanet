# Tasks — admin-superadmin-sidenav-v0

> Convention : inline templates/styles (pas de .html/.scss séparés).
> Signals + store service (pas NgRx). Un service API par domaine.
> Cocher [ ] → [x] en temps réel après chaque tâche.
> Checkpoint de session : lire ce fichier en premier pour savoir où on en est.

---

## Slice 0 — Coordination OpenSpec ✓

- [x] Marquer tous les Slices restants (1–4) de `platform-superadmin-and-tenant-admin-pages/tasks.md` comme `Blocked / superseded by admin-superadmin-sidenav-v0`
- [x] Localiser le composant qui définit les `NavigationSection[]` — `private-navigation.model.ts` (fallback statique) + `private_shell_superadmin.json` (source backend réelle via `/platform/dashboard`)
- [x] Inventorier les pages platform déjà existantes vs manquantes (grille dans proposal.md — État actuel)

---

## Slice 1 — Infrastructure transverse

> Absorbe Slice 1 de `platform-superadmin-and-tenant-admin-pages`

- [ ] `core/tenant-admin-access/support-access.store.ts` — signal store
  - Champs : `sessionId`, `tenantId`, `tenantCode`, `mode: 'SUPPORT_OVERRIDE' | 'SUPPORT_READONLY' | null`
  - Méthodes : `enter(session)`, `exit()`, `isActive: Signal<boolean>`
- [ ] `features/private/shared/sensitive-data-mask.pipe.ts` — masque phone/email/amounts en mode support
- [ ] `features/private/shared/admin-override-banner.ts` — bannière SUPPORT_OVERRIDE / SUPPORT_READONLY
  - Affiche : tenant actif, mode, boutons [Changer tenant] [Quitter contexte]
  - Visible dans tout `/app/admin/**` quand `SupportAccessStore.isActive()`
- [ ] Intégrer `AdminOverrideBanner` dans le shell privé (`PrivateShellPage` ou `PrivateShellComponent`)
- [ ] Désactiver actions mutantes dans les pages admin quand `mode === 'SUPPORT_READONLY'`
- [x] Ajouter un polling shell privé léger
  - Notifications privées : refresh silencieux régulier pour la cloche
  - Runtime/session : refresh régulier pour readiness, entitlements, rôles et permissions
  - Intervalles configurés via `environment.privateShellPolling` (défaut : notifications 20 min, session/runtime 30 min)
  - Si le refresh session retire l'accès, redirection login

---

## Slice 2 — Sidenav & routes platform ✓

- [x] Restructurer `platform.routes.ts` selon la sidenav cible (proposal.md)
  - Conserver tous les redirects existants (`tenant-provisioning`, `tenant-onboarding`, `health`, `draws`, `draw-channels`, `audit`, etc.)
  - Ajouter : `/platform/support-tenant`
  - Ajouter : `/platform/tchala/**`
  - Ajouter : `/platform/ops/communication-tests`, `/platform/ops/identity-sync`
  - Ajouter : `/platform/contact-config`
  - Ajouter : `/platform/access/users`, `/platform/access/backend-keys`
  - Ajouter : `/platform/reports` (placeholder dédié, plus de redirect audit)
  - `/platform/ops/providers` : route conservée (placeholder), absent du menu
- [x] Restructurer `platform-catalog.routes.ts`
  - `/catalog/plans` (nouvelle route), redirect `plans-pricing` → `plans`
  - Garder `/catalog/pricing` séparé
  - Ajouter : `/catalog/draw-channel-games` (placeholder)
  - Ajouter : `/catalog/result-slot-calendars` (placeholder)
- [x] Restructurer `platform-operations.routes.ts`
  - Ajouter : `communication-tests` (placeholder)
  - Ajouter : `identity-sync` (placeholder)
  - Ajouter : `resources` (placeholder détail infra depuis le dashboard Ops)
- [x] Mettre à jour `private-navigation.model.ts` (`PLATFORM_NAVIGATION`) — fallback statique
- [x] Mettre à jour `private_shell_superadmin.json` `sections[]` — source backend réelle
- [x] `nx build` green

---

## Slice 3 — Registry d'actions platform

- [ ] Créer `features/private/platform/shared/platform-action.model.ts`
  - Interface `PlatformActionDefinition` (id, labelKey, icon, surface, endpoint, method, superadminOnly, tenantContextRequired, dangerous?, requiresReason?, bulk?)
  - Registres par domaine : `DRAW_RESULTS_OPS_ACTIONS`, `ARCHIVE_ACTIONS`, `TENANT_ACTIONS`, `TCHALA_ACTIONS`
  - Exemples : `platform.ops.drawResults.override` (dangerous, requiresReason), `tenant.draw.cancel` (tenantContextRequired, dangerous)

---

## Slice 4 — Vue d'ensemble PageModel (Dashboard + Santé)

> Décision : le dashboard superadmin utilise le moteur PageModel runtime existant.
> Pas de `PlatformHomePage` qui orchestre plusieurs appels Angular.
> `/platform/dashboard?logicalId=...` résout le PageModel demandé et le provider `platform_admin_dashboard`.
> `private.dashboard.superadmin` est le dashboard commercial ; `/platform` est l'entrée Ops par défaut.
> Ops est l'accueil par défaut de l'espace superadmin.

- [x] Mettre à jour le template backend `private.dashboard.superadmin.template.json`
  - Tous les widgets cockpit utilisent `binding: { mode: "dynamic", source: "platform_admin_dashboard" }`
  - Widget ids alignés avec `PlatformAdminDashboardProvider` :
    - `dashboard.superadmin.tenants`
    - `dashboard.superadmin.platformSales`
    - `dashboard.superadmin.salesTrend`
    - `dashboard.superadmin.gameBreakdown`
    - `dashboard.superadmin.subscriptions`
    - `dashboard.superadmin.onboarding`
    - `dashboard.superadmin.publicContent`
    - `dashboard.superadmin.topTenants`
    - `dashboard.superadmin.quickActions`
  - Shell via `jsonFile` + `private_shell_superadmin`
  - Aucune donnée fake hardcodée dans le template
- [x] Séparer le dashboard commercial de l'Ops côté provider
  - `logicalId` obligatoire pour `platform_admin_dashboard`
  - service de dispatch `logicalId -> assembler`
  - `private.dashboard.superadmin` route vers l'assembler commercial
  - l'assembler commercial ne construit plus `health` ni les alertes système
- [x] Créer le dashboard Ops temporaire
  - template `private.dashboard.superadmin.ops`
  - `PlatformAdminOpsDashboardPayloadAssembler`
  - `/app/platform` utilise le host PageModel Ops
  - `/app/platform/ops/health` redirige vers `/app/platform`
  - `/app/platform` charge le logicalId Ops par défaut
  - frontend appelle `/platform/dashboard?logicalId=private.dashboard.superadmin.ops`
- [x] Vérifier le template backend `private.dashboard.tenant_admin.template.json`
  - Widgets admin tenant sur `tenant_admin_dashboard`
  - Shell via `jsonFile` + `private_shell_tenant_admin`
  - Sert de référence canonique admin tenant
- [x] Garder `/platform` et `/platform/dashboard` branchés sur le renderer PageModel (`PrivateDashboardPage` ou successeur équivalent)
- [x] Retirer `/platform/overview` de la route web et de la sidenav V0 ; `GET /platform/overview` reste backend structurel seulement
- [x] Ne pas migrer seller-terminal/cashier dans ces templates : cashier web reste sur `/tenant/cashier/home` et `features.pos.home`
- [x] Adapter/ajouter uniquement les widgets frontend nécessaires pour afficher les payloads retournés par `platform_admin_dashboard`
  - `KpiGridWidget` accepte les valeurs `0` et les bindings dynamiques imbriqués
  - `NewsTickerWidget` accepte le payload public content admin (`sourceType`, `sourceUrl`, `publishedAt`)
  - `RankingListWidget` affiche `topTenants` sans nouveau call Angular
  - `TrendChartWidget` existe pour les séries temporelles PageModel
  - `BreakdownListWidget` existe pour les répartitions PageModel
  - `AlertsWidget`, `QuickActionsWidget`, `KpiGridWidget` utilisent les surfaces/tokens cockpit
- [x] Déplacer les providers/assemblers PageModel privés vers `features.pagemodel.dynamic.providers.*`
  - `tenant_admin_dashboard` → `features.pagemodel.dynamic.providers.tenantadmin`
  - `platform_admin_dashboard` → `features.pagemodel.dynamic.providers.platformadmin`
  - Pas d'appel entre features depuis ces providers
- [x] Marquer `features.stats` comme legacy pour les nouveaux dashboards ; KPI/charts via `core.analytics.api`
- [x] La route `/platform/ops/health` conserve `PlatformOpsPage` existante
- [x] Documenter en follow-up le mode différé si un widget dashboard devient trop lent (`runtime.loadStrategy = deferred`, hors V0 sauf besoin immédiat)
- [x] Ajouter au dashboard Ops un bloc P1 `Ressources services`
  - KPI compact `Ressources critiques` / `Ressources à surveiller`
  - Détail compact des services critiques ou dégradés
  - Provider abstrait sans accès Docker socket direct ; V0 basé sur `PlatformHealthProbe`
  - Erreur de collecte dégradée dans le widget, sans casser le dashboard
- [x] Ajouter au dashboard Ops un résumé P0 `Jobs & schedulers`
  - `Jobs failed` alimenté par un provider app basé sur Spring Batch history
  - `Gates désactivés` alimenté par `BatchGate`
  - `Jobs suivis` alimenté par `TchJobRegistry`
  - Le PageModel dépend d'un provider abstrait, pas de Spring Batch directement
- [x] Enrichir le dashboard Ops avec des signaux runtime exploitables
  - Mémoire JVM : OK / warning / critical sans appel réseau
  - Caches critiques plans : `active_plans`, `plan_by_code`, `plan_by_id`
  - Identité : `firebase`, `firebase-emulator`, `local-jwt` avec garde prod/emulator
  - Action rapide vers la synchronisation identité
- [x] Ajouter les cache groups Ops V0
  - Endpoint backend `DELETE /platform/ops/cache/groups/plans?reason=...`
  - Groupe `plans` résolu côté serveur vers les cache names techniques
  - Page cache : bouton confirmé `Vider caches plans`
- [x] Enrichir la lecture Jobs & schedulers du dashboard Ops
  - Widget compact `OpsJobStatusListWidget`
  - Affiche les 5 derniers jobs batch issus du payload `schedulerSummary.items`
  - Les jobs failed/disabled restent résumés dans les KPI
  - Aucun appel Angular supplémentaire
  - Source officielle : tables Spring Batch `batch.BATCH_*`, pas les événements `@TchJob`
  - `draw:lifecycle:generate`, `draw:lifecycle:open`, `draw:lifecycle:close`, `draw:lifecycle:settle`, `results:external:fetch`, `results:external:apply` alimentent l'historique via `BatchJobStarter`
  - Restart natif Spring Batch : `POST /platform/ops/batch/executions/{executionId}:restart`
  - Purge prévue : `POST /platform/ops/batch/executions:purge` + purge automatique hebdo configurable
- [x] Limiter le bloc `Ressources services` au résumé dashboard
  - Top 3 services triés par gravité
  - Mémoire utilisée / limite, restart count et OOM si présents
  - Lien `Voir détails infra` vers `/app/platform/ops/resources`
  - Les métriques indisponibles dégradent le widget sans casser le dashboard
- [x] Ajouter les blocs Ops inbox V0
  - Notifications in-app non lues via `platform.notification.api`
  - Contacts admin reçus via `platform.contactrequest.api.ContactRequestAdminApi`
  - Listes limitées à 5 entrées, sans appel Angular supplémentaire

---

## Slice 5 — Tenants

> Pages partiellement existantes — vérifier état avant de créer

- [ ] Vérifier état de `PlatformTenantsPage` (liste) — si placeholder, implémenter table :
  - Colonnes : code, nom, statut, pays/timezone, admins, dernière activité, actions
  - Actions ligne : Voir détail, Ouvrir Support tenant, Copier code tenant
- [ ] Vérifier état de `TenantOnboardingPage` — si ok, conserver
- [ ] Créer ou vérifier `PlatformTenantAdminsPage` — liste admins d'un tenant, créer admin
- [ ] Créer service `platform-tenants-api.service.ts` si non existant (absorbe Slice 2 de platform-superadmin-and-tenant-admin-pages)
- [ ] Créer service `platform-tenant-admin-access-api.service.ts` — session support start/stop (absorbe Slice 2)

---

## Slice 6 — Référentiels (catalog)

> Routes et pages déjà existantes — travail = sidenav + 2 pages manquantes + split plans/pricing

- [ ] Vérifier que les 9 pages catalog existantes (`settings`, `themes`, `games`, `pricing`, `draw-channels`, `result-slots`, `plans-pricing`, `translations`, `page-model-templates`) sont fonctionnelles
- [ ] Créer `catalog/pages/plans/platform-catalog-plans.page.ts` — plans séparés de pricing
  - `GET /platform/plans`, `POST /platform/plans`
- [ ] S'assurer que `platform-catalog-pricing.page.ts` gère les tarifs (distinct des plans)
- [ ] Créer `catalog/pages/draw-channel-games/platform-catalog-draw-channel-games.page.ts`
  - `GET /platform/draw-channels/{channelId}/games`
  - `POST /platform/draw-channels/{channelId}/games`
  - `POST /platform/draw-channels/{channelId}/games/bulk`
- [ ] Créer `catalog/pages/result-slot-calendars/platform-catalog-result-slot-calendars.page.ts`
  - `GET /platform/result-slots/{resultSlotId}/calendar`
  - `POST /platform/result-slots/{resultSlotId}/calendar`
- [ ] Mettre à jour `platform-catalog.routes.ts` avec les nouvelles pages + redirect `plans-pricing` → `plans`

---

## Slice 7 — Opérations

> Pages déjà existantes — travail = 2 pages manquantes + vérification fonctionnelle

- [ ] Vérifier que `PlatformOpsDrawsPage`, `PlatformOpsDrawResultsPage`, `PlatformOpsBatchPage`, `PlatformOpsCachePage`, `PlatformArchivePage`, `PlatformAuditPage` sont fonctionnelles (vs placeholders)
- [ ] Créer `operations/pages/communication-tests/platform-ops-communication-tests.page.ts`
  - `POST /platform/ops/communication/email-test`
  - `POST /platform/ops/communication/slack-test`
  - `POST /platform/ops/notifications/test`
  - Formulaire test + affichage statut + copie traceId
- [ ] Créer `operations/pages/identity-sync/platform-ops-identity-sync.page.ts`
  - `POST /platform/ops/sync/identity/firebase-bootstrap-users`
  - Lancer sync + voir résultat + voir erreurs + copie traceId
- [ ] Retirer Providers du menu (route → placeholder caché, non listé dans NavigationSection[])

---

## Slice 8 — Support & contenu

> Contact-requests, News, Notifications — pages existantes à repositionner dans sidenav

- [x] Rendre `PlatformContactRequestsPage` fonctionnelle
  - Liste `GET /platform/contact-requests`
  - Filtres statut/type, pagination, détail `GET /platform/contact-requests/{id}`
  - Actions V0 : changer le statut, modifier les notes internes
- [x] Brancher `PlatformNewsPage`
  - Source : `platform.publiccontent`
  - Liste interne + RSS externe, internes affichées en priorité
  - Création/modification news interne, publier/archiver, masquer RSS, refresh RSS
- [x] Brancher `PlatformNotificationsPage`
  - Source : `platform.notification`
  - Liste persistée, filtres statut/sévérité/catégorie
  - Composer notification système `PLATFORM`, canaux `WEB` + `IN_APP`
  - Actions V0 : marquer comme lu, archiver
- [x] Créer `pages/contact-config/platform-contact-config.page.ts` — configuration contact global
  - Placeholder V0 dédié avec message "Endpoint à venir" car gap backend confirmé
  - Champs attendus documentés : email support, téléphone, canaux de réception, message affiché page contact
- [x] Organiser les routes `contact-requests`, `news`, `notifications`, `contact-config` sous une section cohérente dans la sidenav (conserver anciens paths comme redirects)

---

## Slice 9 — Tchala

- [ ] Créer `tchala/platform-tchala.routes.ts`
- [ ] Créer `tchala/platform-tchala-suggestions.page.ts`
  - `GET /admin/tchala/pending`
  - `POST /admin/tchala/approve`, `POST /admin/tchala/reject`
  - Table : rêve proposé, numéro, langue, source, statut — Actions : Approuver, Rejeter, Fusionner
- [ ] Créer `tchala/platform-tchala-import.page.ts`
  - `POST /admin/tchala/import` — upload fichier + voir erreurs
- [ ] Créer `tchala/platform-tchala-cleanup.page.ts`
  - `POST /admin/tchala/merge`, `POST /admin/tchala/delete` — fusionner / supprimer entrées
- [ ] Ajouter routes `/platform/tchala/**` dans `platform.routes.ts`

---

## Slice 10 — Contrôle d’accès

> Capability backend : `platform.accesscontrol`.
> Permissions/Rôles = placeholders existants ; Super admins = pages existantes ; Users/Keys = manquants.
> Le catalogue des permissions est en lecture côté REST admin ; ne pas exposer create/delete permission en UI V1.

- [x] Renommer le groupe sidenav `platform.nav.accessSecurity` en "Contrôle d’accès"
- [x] Implémenter `pages/access/platform-permissions.page.ts` (remplace placeholder)
  - Catalogue lecture seule : `GET /admin/access-control/permissions`
  - Recherche/filtre par code, domaine, description
- [x] Implémenter `pages/access/platform-roles.page.ts` (remplace placeholder)
  - `GET /admin/access-control/roles`
  - Détail permissions du rôle : `GET /admin/access-control/roles/{roleId}/permissions`
- [x] Implémenter la recherche utilisateur accès
  - Rechercher un `APP_USER` tenant/admin/superadmin via `/identity/users` ou `/admin/identity/users` selon contexte réel
  - Afficher rôles actifs et permissions effectives
  - Overrides : actions supportées en V1 ; affichage séparé bloqué tant qu'un endpoint de listing des overrides n'est pas exposé
  - Permissions effectives : `GET /admin/access-control/users/{userId}/permissions/effective`
- [x] Brancher les actions utilisateur supportées par `platform.accesscontrol`
  - Ajouter rôle : `POST /admin/access-control/users/{userId}/roles/{roleCode}`
  - Retirer rôle : `DELETE /admin/access-control/users/{userId}/roles/{roleCode}`
  - Grant override : `PUT /admin/access-control/users/{userId}/permissions/{permissionCode}/grant`
  - Deny override : `PUT /admin/access-control/users/{userId}/permissions/{permissionCode}/deny`
  - Retirer override : `DELETE /admin/access-control/users/{userId}/permissions/{permissionCode}/override`
  - Toutes les actions mutantes doivent demander une raison quand le backend l'accepte et refléter loading/error/success
- [ ] Vérifier état pages `PlatformSuperAdminsPage` et create — si placeholders, implémenter
- [x] Déplacer l'entrée "Comptes admin" dans Contrôle d'accès
  - Réutilise la page existante `/platform/tenant-admins`
  - `/platform/access/users` redirige vers `/platform/tenant-admins`
  - Liste des `APP_USER` admin/tenant-admin ; ne pas mélanger avec seller terminals
- [ ] Créer `pages/access/platform-backend-keys.page.ts`
  - `GET /public/security/backend-signing-keys` — diagnostic sécurité lecture seule
- [ ] S'assurer que toutes les routes `/platform/access/**` et `/platform/super-admins` sont connectées

---

## Slice 11 — Support & contenu : Rapports platform

- [ ] Créer `pages/reports/platform-reports.page.ts` — cockpit rapports superadmin
  - Widgets : ventes agrégées par tenant, tenants actifs/inactifs, tirages traités, résultats fetchés/corrigés, jobs exécutés/échoués, messages support reçus
  - Placeholder par widget si gap backend (voir gaps-backend.md)
- [ ] Brancher `/platform/reports` sur `PlatformReportsPage` (plus de redirect vers audit)

---

## Slice 12 — Support tenant shell

- [ ] Créer `platform/support-tenant/platform-support-tenant.page.ts`
  - Sélecteur de tenant (recherche par code ou nom)
  - Après sélection : chargement du contexte + redirection vers vue admin du tenant
  - Bandeau permanent : "Contexte tenant actif : {CODE} — [Changer tenant] [Quitter contexte]"
  - Utilise `SupportAccessStore` (Slice 1)
- [ ] Créer `platform/support-tenant/platform-support-tenant.routes.ts`
  - Réutiliser les routes admin (`/app/admin/**`) en injectant le tenant header via intercepteur ou `asTenantAdmin` sur `TchBackendClient`
- [ ] Créer `platform-tenant-admin-access-api.service.ts` si non existant (start/stop session support)
- [ ] Créer le dialog `StartTenantAdminAccessDialog` (raison + checkbox + mode détecté par statut tenant) — absorbe Slice 3 de `platform-superadmin-and-tenant-admin-pages`
- [ ] Pages accessibles depuis Support tenant (réutilisées, non dupliquées) :
  - Tirages, Vendeurs, Limites, Contrôles de vente, Promotions, Rapports, Tickets, Mon entreprise, Subscription

## Slice 13 — Tenant Admin Financials

- [x] Ajouter une page admin `reports/financials` pour les financials tenant.
- [x] Appeler `GET /admin/financials/breakdown` via `TchBackendClient`, sans `tenantId` côté UI ; le tenant vient du contexte backend.
- [x] Afficher résumé, lignes par tirage et lignes terminal vendeur × tirage.
- [x] Afficher un empty state explicite pour un nouveau tenant sans ventes/projections.
- [x] Ajouter les tests UI/API ciblés pour empty state et absence de `tenantId` client.
- [ ] Validation web globale verte.
  - Bloqué actuellement par dettes existantes hors tranche : tests navigation/auth/Firebase et warnings Angular globaux.

---

## Validation globale

- [ ] `nx build` green après chaque slice
- [ ] Aucune donnée fake hardcodée dans les templates
- [ ] Loading/error/empty states présents sur toutes les pages
- [ ] `AdminOverrideBanner` visible dans `/app/admin/**` quand session support active
- [ ] Actions mutantes désactivées en `SUPPORT_READONLY`
- [ ] Providers absent du menu (route gardée mais hors NavigationSection[])
- [ ] Redirects existants tous préservés
- [ ] Plans et Pricing séparés dans Référentiels
- [ ] Aucun terme technique visible dans les libellés sidenav (`draw_result`, `batch`, `provider`, `slot`, etc.)
