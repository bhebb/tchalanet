# Tasks — admin-superadmin-sidenav-v0

> Convention : inline templates/styles (pas de .html/.scss séparés).
> Signals + store service (pas NgRx). Un service API par domaine.
> Cocher [ ] → [x] en temps réel après chaque tâche.
> Checkpoint de session : lire ce fichier en premier pour savoir où on en est.

---

## Slice 0 — Coordination OpenSpec

- [ ] Marquer tous les Slices restants (1–4) de `platform-superadmin-and-tenant-admin-pages/tasks.md` comme `Blocked / superseded by admin-superadmin-sidenav-v0`
- [ ] Localiser le composant qui définit les `NavigationSection[]` de la sidenav platform (shell ou nav service)
- [ ] Inventorier les pages platform déjà existantes vs manquantes (grille dans proposal.md — État actuel)

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

---

## Slice 2 — Sidenav & routes platform

- [ ] Restructurer `platform.routes.ts` selon la sidenav cible (proposal.md)
  - Conserver tous les redirects existants (`tenant-provisioning`, `tenant-onboarding`, `health`, `draws`, `draw-channels`, `audit`, etc.)
  - Ajouter : `/platform/support-tenant`
  - Ajouter : `/platform/tchala/**`
  - Ajouter : `/platform/ops/communication-tests`, `/platform/ops/identity-sync`
  - Ajouter : `/platform/contact-config`
  - Ajouter : `/platform/access/users`, `/platform/access/backend-keys`
  - Ajouter : `/platform/reports` (nouvelle page dédiée, pas redirect audit)
  - Retirer `/platform/ops/providers` du rendu actif (garder route → placeholder caché)
- [ ] Restructurer `platform-catalog.routes.ts`
  - Splitter `plans-pricing` → `/catalog/plans` + garder `/catalog/pricing`
  - Ajouter : `/catalog/draw-channel-games`
  - Ajouter : `/catalog/result-slot-calendars`
  - Conserver redirect `plans-pricing` → `plans`
- [ ] Restructurer `platform-operations.routes.ts`
  - Ajouter : `communication-tests`
  - Ajouter : `identity-sync`
- [ ] Mettre à jour les `NavigationSection[]` platform pour correspondre au menu cible
- [ ] Vérifier `nx build` green après ce slice

---

## Slice 3 — Registry d'actions platform

- [ ] Créer `features/private/platform/shared/platform-action.model.ts`
  - Interface `PlatformActionDefinition` (id, labelKey, icon, surface, endpoint, method, superadminOnly, tenantContextRequired, dangerous?, requiresReason?, bulk?)
  - Registres par domaine : `DRAW_RESULTS_OPS_ACTIONS`, `ARCHIVE_ACTIONS`, `TENANT_ACTIONS`, `TCHALA_ACTIONS`
  - Exemples : `platform.ops.drawResults.override` (dangerous, requiresReason), `tenant.draw.cancel` (tenantContextRequired, dangerous)

---

## Slice 4 — Vue d'ensemble (Dashboard + Santé)

- [ ] Créer `platform-home.page.ts` — cockpit superadmin
  - Widgets : tenants actifs, tirages du jour (tous tenants), jobs en erreur, résultats manquants, messages contact récents, alertes archive/cache
  - Appels parallèles vers endpoints existants, skeleton/error par bloc
  - Actions directes : [Voir tenants] [Voir opérations] [Voir messages support] [Voir jobs en erreur]
- [ ] Brancher `/platform` et `/platform/dashboard` sur `PlatformHomePage` (remplace `PrivateDashboardPage`)
- [ ] La route `/platform/ops/health` conserve `PlatformOpsPage` existante

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

- [ ] Vérifier que `PlatformContactRequestsPage` et `PlatformNewsPage` et `PlatformNotificationsPage` sont fonctionnelles
- [ ] Créer `pages/contact-config/platform-contact-config.page.ts` — configuration contact global
  - Placeholder V0 avec message "Endpoint à venir" si gap backend confirmé
  - Champs attendus : email support, téléphone, canaux de réception, message affiché page contact
- [ ] Organiser les routes `contact-requests`, `news`, `notifications`, `contact-config` sous une section cohérente dans la sidenav (conserver anciens paths comme redirects)

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

## Slice 10 — Accès & sécurité

> Permissions/Rôles = placeholders existants ; Super admins = pages existantes ; Users/Keys = manquants

- [ ] Implémenter `pages/access/platform-permissions.page.ts` (remplace placeholder)
  - `GET /admin/access-control/permissions`
- [ ] Implémenter `pages/access/platform-roles.page.ts` (remplace placeholder)
  - `GET /admin/access-control/roles`
- [ ] Vérifier état pages `PlatformSuperAdminsPage` et create — si placeholders, implémenter
- [ ] Créer `pages/access/platform-users.page.ts`
  - `GET /identity/users` ou `/admin/identity/users` selon contexte
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
