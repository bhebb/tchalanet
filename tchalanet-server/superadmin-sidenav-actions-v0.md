# Plan — Superadmin Sidenav & Actions internes V0

## Statut

Proposé — basé sur l’inventaire actuel des 185 endpoints backend.

## Objectif

Le superadmin n’est pas un simple admin tenant avec plus de droits. C’est l’acteur qui gère la plateforme Tchalanet : référentiels globaux, tenants, opérations, support, contenu public, sécurité et diagnostics.

La règle produit :

```text
Un controller non exposé côté web = fonctionnalité morte.
Un endpoint exposé sans bonne page UX = fonctionnalité confuse.
Un endpoint tenant-scoped peut être réutilisé par le superadmin en contexte tenant.
```

Le superadmin a donc deux modes :

| Mode | Description | Exemple |
|---|---|---|
| Platform global | Actions globales sans tenant ciblé | catalog, ops, archive, audit, news, Tchala, super admins |
| Support tenant | Le superadmin choisit un tenant et utilise les écrans admin avec tenant header | limites, promotions, vendeurs, tirages, commissions, rapports |

---

# 1. Sidenav superadmin cible

```text
Platform
├── Vue d’ensemble
│   ├── Tableau de bord
│   └── Santé système
│
├── Tenants
│   ├── Liste des tenants
│   ├── Onboarding tenant
│   ├── Admins tenant
│   └── Support tenant
│
├── Référentiels
│   ├── Jeux
│   ├── Canaux de tirage
│   ├── Jeux par canal
│   ├── Slots de résultats
│   ├── Calendriers des slots
│   ├── Plans
│   ├── Pricing
│   ├── Paramètres globaux
│   ├── Thèmes
│   ├── Traductions
│   └── Templates de pages
│
├── Opérations
│   ├── Tirages
│   ├── Résultats
│   ├── Tâches planifiées
│   ├── Cache
│   ├── Archives
│   ├── Tests communication
│   ├── Synchronisation identité
│   └── Audit
│
├── Support & contenu
│   ├── Messages de contact
│   ├── News publiques
│   ├── Notifications
│   └── Configuration contact
│
├── Tchala
│   ├── Suggestions
│   ├── Import
│   └── Nettoyage
│
├── Accès & sécurité
│   ├── Permissions
│   ├── Rôles
│   ├── Super admins
│   ├── Utilisateurs
│   └── Clés publiques backend
│
└── Rapports platform
```

Décisions importantes :

```text
- Pas de menu Providers en V0.
- Providers/diagnostics provider peuvent être rattachés à Résultats ou Slots de résultats plus tard.
- Pas de page Créer tenant séparée : Onboarding tenant fait preview + provision.
- Plans et Pricing sont séparés.
- Tout tchalanet-catalog doit être exposé dans Référentiels.
- Les écrans tenant-scoped sont réutilisés dans Support tenant.
```

---

# 2. Vue d’ensemble

## 2.1 Tableau de bord

| Élément | Valeur |
|---|---|
| Lien | `Vue d’ensemble > Tableau de bord` |
| Page Angular | `PlatformDashboardPage` |
| But | Voir l’état global de la plateforme |
| Endpoints | `GET /platform/dashboard`, `GET /platform/overview` |
| Controllers | `PlatformPageModelController`, `PlatformAdminOverviewController` |

### Contenu utile

```text
- Nombre de tenants actifs
- Tenants récemment créés
- Tirages du jour par tenant
- Jobs en erreur
- Résultats manquants / provisoires
- Messages de contact récents
- Alertes archive/cache/audit
```

### Actions

```text
[Voir tenants]
[Voir opérations]
[Voir messages support]
[Voir jobs en erreur]
```

---

## 2.2 Santé système

| Élément | Valeur |
|---|---|
| Lien | `Vue d’ensemble > Santé système` |
| Page Angular | `PlatformHealthPage` |
| But | Diagnostic technique simple |
| Endpoints actuels | À vérifier côté web/app, pas clairement visible dans l’extraction backend métier |

### Contenu utile

```text
- API up/down
- DB up/down
- Cache up/down
- Scheduler actif/inactif
- Derniers batch failures
- TraceId si erreur
```

---

# 3. Tenants

## 3.1 Liste des tenants

| Élément | Valeur |
|---|---|
| Lien | `Tenants > Liste des tenants` |
| Page Angular | `PlatformTenantsPage` |
| Endpoints | `GET /platform/tenants`, `GET /platform/tenants/by-code` |
| Controller | `TenantAdminController` |

### Table

| Colonne | Description |
|---|---|
| Code | Code tenant |
| Nom | Nom affiché |
| Statut | Actif / suspendu / archivé |
| Pays / timezone | Contexte opérationnel |
| Admins | Nombre/admin principal |
| Dernière activité | Dernier signal utile |
| Actions | Voir, ouvrir support tenant, ouvrir admin |

### Actions ligne

```text
Voir détail
Ouvrir Support tenant
Ouvrir Admin tenant
Copier code tenant
Suspendre / réactiver — si endpoint disponible plus tard
Archiver — si endpoint disponible plus tard
```

---

## 3.2 Onboarding tenant

| Élément | Valeur |
|---|---|
| Lien | `Tenants > Onboarding tenant` |
| Page Angular | `TenantOnboardingPage` |
| Endpoints | `POST /platform/tenant-onboarding/preview`, `POST /platform/tenant-onboarding/provision` |
| Controller | `TenantProvisioningController` |

### But utilisateur

Créer un tenant complet avec ses éléments initiaux : identité, admin tenant, configuration initiale, éventuellement jeux/tirages par défaut.

### Actions

```text
[Prévisualiser]
[Provisionner]
[Ouvrir le tenant créé]
[Ouvrir Support tenant]
```

### Décision

Ne pas créer une page séparée `Créer tenant`. L’onboarding couvre déjà ce besoin.

---

## 3.3 Admins tenant

| Élément | Valeur |
|---|---|
| Lien | `Tenants > Admins tenant` |
| Page Angular | `PlatformTenantAdminsPage` ou réutilisation `IdentityUserAdminPage` |
| Endpoints | `GET /admin/identity/users`, `POST /admin/identity/users`, superadmin avec tenant header |
| Controller | `IdentityUserAdminController` |
| Tenant context | Requis pour agir sur un tenant |

### Actions

```text
Lister admins du tenant
Créer admin tenant
Réinitialiser accès — si endpoint disponible
Désactiver / réactiver — si endpoint disponible
```

---

## 3.4 Support tenant

| Élément | Valeur |
|---|---|
| Lien | `Tenants > Support tenant` |
| Page Angular | `PlatformTenantSupportShell` |
| But | Réutiliser les pages admin avec tenant header |
| Tenant context | Obligatoire |

### UX obligatoire

Afficher un bandeau permanent :

```text
Contexte tenant actif : HAITILOTO
Vous agissez comme support platform sur ce tenant.
[Changer tenant] [Quitter contexte]
```

### Pages réutilisées

```text
Support tenant
├── Vue d’ensemble tenant
├── Tirages
├── Vendeurs
├── Limites
├── Contrôles de vente
│   ├── Jeux & tarifs
│   ├── Gains à payer
│   └── Commissions
├── Promotions
├── Rapports
├── Tickets
├── Mon entreprise
├── Subscription
└── Configuration Haiti Lottery
```

### Endpoints réutilisés

Les endpoints `/admin/**` et `/tenant/**` peuvent être utilisés par le superadmin avec tenant header lorsque la sécurité l’autorise.

Exemples :

```text
GET  /admin/draws
POST /admin/draws/{drawId}/cancel
POST /admin/draws/{drawId}/manual-result
GET  /admin/seller-terminals
POST /admin/seller-terminals
GET  /admin/policies/limits/assignments
PUT  /admin/policies/limits/assignments
GET  /admin/promotions/campaigns
POST /admin/promotions/campaigns
GET  /tenant/reports/sales-by-period-and-game
GET  /tenant/subscription
```

---

# 4. Référentiels

Tout ce qui vient de `tchalanet-catalog` doit être visible ici, car ce sont des données globales de plateforme.

## 4.1 Jeux

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Jeux` |
| Page Angular | `PlatformCatalogGamesPage` |
| Endpoints | `GET /platform/catalog/games`, `POST /platform/catalog/games` |
| Controller | `GameAdminController` |

### Actions

```text
Lister jeux
Créer jeu
Voir détail
Activer / désactiver — si endpoint disponible
```

---

## 4.2 Canaux de tirage

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Canaux de tirage` |
| Page Angular | `PlatformDrawChannelsPage` |
| Endpoints | `GET /platform/draw-channels`, `POST /platform/draw-channels` |
| Controller | `PlatformDrawChannelController` |

### Actions

```text
Lister canaux
Créer canal
Voir jeux associés
Configurer jeux par canal
```

---

## 4.3 Jeux par canal

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Jeux par canal` |
| Page Angular | `PlatformDrawChannelGamesPage` |
| Endpoints | `GET /platform/draw-channels/{channelId}/games`, `POST /platform/draw-channels/{channelId}/games`, `POST /platform/draw-channels/{channelId}/games/bulk` |
| Controller | `PlatformDrawChannelGameController` |

### But

Définir quels jeux peuvent être vendus sur chaque canal de tirage global.

---

## 4.4 Slots de résultats

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Slots de résultats` |
| Page Angular | `PlatformResultSlotsPage` |
| Endpoints | `GET /platform/result-slots/active`, `POST /platform/result-slots` |
| Controller | `ResultSlotAdminController` |

### But

Gérer les créneaux globaux de résultats : provider, slot, heure attendue, statut.

### Actions

```text
Lister slots actifs
Créer slot
Voir calendrier
Voir résultats associés
Fetch résultat via Ops > Résultats si nécessaire
```

---

## 4.5 Calendriers des slots

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Calendriers des slots` |
| Page Angular | `PlatformResultSlotCalendarPage` |
| Endpoints | `GET /platform/result-slots/{resultSlotId}/calendar`, `POST /platform/result-slots/{resultSlotId}/calendar` |
| Controller | `ResultSlotCalendarAdminController` |

### But

Définir les dates/heures attendues pour un slot de résultat.

---

## 4.6 Plans

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Plans` |
| Page Angular | `PlatformPlansPage` |
| Endpoints | `GET /platform/plans`, `POST /platform/plans` |
| Controller | `PlanAdminController` |

### Définition

Un plan est une offre commerciale ou un niveau d’abonnement disponible pour les tenants.

---

## 4.7 Pricing

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Pricing` |
| Page Angular | `PlatformPricingPage` |
| Endpoints | `GET /platform/pricing`, `POST /platform/pricing` |
| Controller | `PricingAdminController` |

### Définition

Pricing = tarifs globaux / prix de référence / règles tarifaires catalogue.

Décision : ne pas fusionner avec Plans.

---

## 4.8 Paramètres globaux

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Paramètres globaux` |
| Page Angular | `PlatformSettingsPage` |
| Endpoints | `GET /platform/settings`, `POST /platform/settings`, `GET /platform/settings/overview` |
| Controller | `PlatformSettingsController` |

---

## 4.9 Thèmes

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Thèmes` |
| Page Angular | `PlatformThemePresetsPage` |
| Endpoints | `GET /platform/catalog/theme-presets`, `POST /platform/catalog/theme-presets`, `GET /platform/catalog/theme-presets/overview` |
| Controller | `ThemeAdminController` |

---

## 4.10 Traductions

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Traductions` |
| Page Angular | `PlatformI18nOverridesPage` |
| Endpoints | `GET /platform/i18n-overrides`, `POST /platform/i18n-overrides`, `GET /platform/i18n-overrides/overview`, `GET /platform/i18n-overrides/resolve` |
| Controller | `PlatformI18nOverridesController` |

---

## 4.11 Templates de pages

| Élément | Valeur |
|---|---|
| Lien | `Référentiels > Templates de pages` |
| Page Angular | `PlatformPageModelTemplatesPage` |
| Endpoints | `GET /platform/page-model-templates`, `POST /platform/page-model-templates`, `GET /platform/page-model-templates/visible` |
| Controller | `PlatformPageModelTemplateController` |

---

# 5. Opérations

Ops reste technique, car c’est une surface superadmin.

## 5.1 Tirages

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Tirages` |
| Page Angular | `PlatformOpsDrawsPage` |
| Endpoints | `POST /platform/ops/draws/generate`, `POST /platform/ops/draws/open-today`, `POST /platform/ops/draws/close-due`, `POST /platform/ops/draws/apply` |
| Controller | `DrawCalendarOpsController` |

### Actions

```text
Générer tirages
Ouvrir tirages du jour
Fermer tirages échus
Appliquer résultats aux tenants
Dry-run si disponible
Forcer avec raison si disponible
```

### Important

Les actions sur un draw précis restent sur les pages tenant-scoped :

```text
/admin/draws/{drawId}/cancel
/admin/draws/{drawId}/manual-result
/admin/draws/{drawId}/lock
/admin/draws/{drawId}/unlock
/admin/draws/{drawId}/archive
```

Le superadmin y accède via Support tenant avec tenant header.

---

## 5.2 Résultats

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Résultats` |
| Page Angular | `PlatformOpsDrawResultsPage` |
| Endpoints | `GET /platform/ops/draw-results`, `GET /platform/ops/draw-results/by-slot`, `POST /platform/ops/draw-results/fetch`, `POST /platform/ops/draw-results/manual`, `POST /platform/ops/draw-results/override`, `POST /platform/ops/draw-results/refresh` |
| Controller | `DrawResultsOpsController` |

### Définition métier

`draw_result` est global, non tenant-scoped.

```text
Override résultat
→ modifie le draw_result global
→ publie un event
→ l’event applique/réapplique les résultats aux draws tenant concernés
```

### Actions

```text
Lister résultats globaux
Fetch résultats par slot/période
Créer résultat global manuel
Corriger résultat global
Refresh résultats
Voir résultat par slot
```

### À éviter

Ne pas présenter override comme une action sur un tirage tenant. C’est une action sur un résultat global.

---

## 5.3 Tâches planifiées

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Tâches planifiées` |
| Page Angular | `PlatformOpsBatchPage` |
| Endpoints | `GET /platform/ops/batch/jobs`, `GET /platform/ops/batch/executions`, `GET /platform/ops/batch/gates/:effective` |
| Controllers | `OpsBatchJobController`, `OpsBatchExecutionController`, `OpsBatchGateController` |

### Actions attendues

```text
Voir jobs
Voir exécutions
Voir gates effectives
Démarrer job — si endpoint start exposé dans controller complet
Activer/désactiver gate — si endpoint PUT exposé dans controller complet
```

---

## 5.4 Cache

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Cache` |
| Page Angular | `PlatformOpsCachePage` |
| Endpoints | `GET /platform/ops/cache`, `DELETE /platform/ops/cache` |
| Controller | `CacheOpsController` |

### Actions

```text
Lister caches
Vider tous les caches
Vider un cache spécifique — si endpoint par cache disponible côté controller complet
```

---

## 5.5 Archives

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Archives` |
| Page Angular | `PlatformArchiveOpsPage` |
| Endpoints | `/platform/archive/**` |
| Controller | `PlatformArchiveController` |

### Endpoints

```text
GET  /platform/archive/runs
POST /platform/archive/runs
GET  /platform/archive/runs/failed
GET  /platform/archive/partition-cleanup/plan
POST /platform/archive/partition-cleanup/execute
POST /platform/archive/restore/audit-log
GET  /platform/archive/objects/invalid
GET  /platform/archive/ops-summary
```

### Actions

```text
Voir runs archive
Déclencher archive
Voir runs échoués
Voir plan cleanup partition
Exécuter cleanup
Restaurer audit log
Voir objets invalides
Voir résumé ops
```

---

## 5.6 Tests communication

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Tests communication` |
| Page Angular | `PlatformOpsCommunicationTestsPage` |
| Endpoints | `POST /platform/ops/communication/email-test`, `POST /platform/ops/communication/slack-test`, `POST /platform/ops/notifications/test` |
| Controllers | `PlatformCommunicationOpsController`, `OpsNotificationController` |

### Actions

```text
Tester email
Tester Slack
Tester notification in-app
Afficher statut
Copier traceId
```

---

## 5.7 Synchronisation identité

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Synchronisation identité` |
| Page Angular | `PlatformIdentitySyncPage` |
| Endpoint | `POST /platform/ops/sync/identity/firebase-bootstrap-users` |
| Controller | `PlatformIdentitySyncOpsController` |

### Actions

```text
Lancer sync Firebase bootstrap users
Voir résultat
Voir erreurs
Copier traceId
```

---

## 5.8 Audit

| Élément | Valeur |
|---|---|
| Lien | `Opérations > Audit` |
| Page Angular | `PlatformAuditPage` |
| Endpoint visible dans extraction | `POST /platform/audit/purge` |
| Controller | `AuditEventRestController` |

### Actions

```text
Voir audit logs — endpoint list à confirmer
Purger logs expirés
Exporter audit — si disponible plus tard
```

---

# 6. Support & contenu

## 6.1 Messages de contact

| Élément | Valeur |
|---|---|
| Lien | `Support & contenu > Messages de contact` |
| Page Angular | `PlatformContactRequestsPage` |
| Endpoint | `GET /platform/contact-requests` |
| Controller | `PlatformContactRequestAdminController` |

### Source publique

Les utilisateurs soumettent via :

```text
POST /public/contact-requests
```

### Actions V0

```text
Voir messages
Voir détail
Copier email
Copier message
Filtrer par date/sujet
```

### Gaps utiles

```text
PATCH /platform/contact-requests/{id}/status
POST  /platform/contact-requests/{id}/archive
POST  /platform/contact-requests/{id}/reply
```

---

## 6.2 News publiques

| Élément | Valeur |
|---|---|
| Lien | `Support & contenu > News publiques` |
| Page Angular | `PlatformPublicNewsPage` |
| Endpoints | `GET /platform/public-content/news`, `POST /platform/public-content/news`, `POST /platform/public-content/news/force-refresh` |
| Controller | `PlatformPublicContentAdminController` |

### Côté public

```text
GET /public/news
```

### Actions

```text
Lister news
Créer / modifier news
Forcer refresh public
Prévisualiser la news publique
```

---

## 6.3 Notifications

| Élément | Valeur |
|---|---|
| Lien | `Support & contenu > Notifications` |
| Page Angular | `PlatformNotificationsPage` ou `AdminNotificationsPage` selon scope |
| Endpoints tenant-scoped | `GET /admin/notifications`, `POST /admin/notifications`, `GET /admin/notifications/deliveries`, `GET /admin/notifications/summary` |
| Controller | `AdminNotificationController` |

### Décision

Les notifications peuvent être gérées :

```text
- par tenant via Support tenant
- globalement seulement si un endpoint platform dédié existe plus tard
```

---

## 6.4 Configuration contact

| Élément | Valeur |
|---|---|
| Lien | `Support & contenu > Configuration contact` |
| Page Angular | `PlatformContactConfigPage` |
| Endpoints actuels | À compléter côté backend si configuration globale nécessaire |

### But

Configurer les informations de contact affichées publiquement ou utilisées par la plateforme :

```text
Email support
Téléphone support
Adresse publique
Canaux de réception
Message affiché sur la page contact
```

### Gap backend probable

```text
GET /platform/contact-config
PUT /platform/contact-config
```

---

# 7. Tchala

Même si les endpoints commencent par `/admin/tchala`, la sécurité montre que c’est superadmin-only. Fonctionnellement, cette section appartient au superadmin.

## 7.1 Suggestions

| Élément | Valeur |
|---|---|
| Lien | `Tchala > Suggestions` |
| Page Angular | `PlatformTchalaSuggestionsPage` |
| Endpoints | `GET /admin/tchala/pending`, `POST /admin/tchala/approve`, `POST /admin/tchala/reject` |
| Controller | `AdminTchalaController` |

### UX

```text
Suggestions en attente
Rêve proposé
Numéro proposé
Langue
Source
Statut
Actions : Approuver, Rejeter, Fusionner
```

---

## 7.2 Import

| Élément | Valeur |
|---|---|
| Lien | `Tchala > Import` |
| Page Angular | `PlatformTchalaImportPage` |
| Endpoint | `POST /admin/tchala/import` |
| Controller | `AdminTchalaController` |

### Actions

```text
Importer fichier
Prévisualiser import — si disponible plus tard
Voir erreurs import
```

---

## 7.3 Nettoyage

| Élément | Valeur |
|---|---|
| Lien | `Tchala > Nettoyage` |
| Page Angular | `PlatformTchalaCleanupPage` |
| Endpoints | `POST /admin/tchala/merge`, `POST /admin/tchala/delete` |
| Controller | `AdminTchalaController` |

### Actions

```text
Fusionner entrées
Supprimer entrées
Nettoyer doublons
```

---

# 8. Accès & sécurité

## 8.1 Permissions

| Élément | Valeur |
|---|---|
| Lien | `Accès & sécurité > Permissions` |
| Page Angular | `PlatformPermissionsPage` |
| Endpoint actuel | `GET /admin/access-control/permissions` |
| Controller | `AccessControlAdminController` |

### Décision

Même si le path est `/admin`, fonctionnellement c’est une page sécurité. Elle peut être visible au superadmin globalement, ou en support tenant si les permissions sont tenant-scoped.

---

## 8.2 Rôles

| Élément | Valeur |
|---|---|
| Lien | `Accès & sécurité > Rôles` |
| Page Angular | `PlatformRolesPage` |
| Endpoint actuel | `GET /admin/access-control/roles` |
| Controller | `AccessControlAdminController` |

---

## 8.3 Super admins

| Élément | Valeur |
|---|---|
| Lien | `Accès & sécurité > Super admins` |
| Page Angular | `PlatformSuperAdminsPage` |
| Endpoints | `GET /platform/super-admins`, `POST /platform/super-admins` |
| Controller | `PlatformSuperAdminController` |

### Actions

```text
Lister super admins
Créer super admin
Désactiver super admin — si endpoint disponible plus tard
```

---

## 8.4 Utilisateurs

| Élément | Valeur |
|---|---|
| Lien | `Accès & sécurité > Utilisateurs` |
| Page Angular | `PlatformUsersPage` |
| Endpoints | `GET /identity/users`, `POST /identity/users`, ou `/admin/identity/users` en contexte tenant |
| Controllers | `IdentityUserCrudController`, `IdentityUserAdminController` |

### Décision

À clarifier :

```text
- /identity/users = gestion globale utilisateur avec permissions user.*
- /admin/identity/users = gestion d’utilisateurs tenant avec tenant context
```

---

## 8.5 Clés publiques backend

| Élément | Valeur |
|---|---|
| Lien | `Accès & sécurité > Clés publiques backend` |
| Page Angular | `BackendPublicKeysPage` |
| Endpoint | `GET /public/security/backend-signing-keys` |
| Controller | `BackendPublicKeysController` |

### Décision

Même si l’endpoint est public, la page superadmin peut l’utiliser pour diagnostic sécurité.

---

# 9. Rapports platform

| Élément | Valeur |
|---|---|
| Lien | `Rapports platform` |
| Page Angular | `PlatformReportsPage` |
| Endpoints actuels | Peu/pas spécifiques platform dans extraction |

### Contenu utile V0

```text
Ventes agrégées par tenant
Tenants actifs/inactifs
Top tenants par volume
Tirages traités
Résultats fetchés/corrigés
Jobs exécutés/échoués
Messages support reçus
```

### Gaps backend utiles

```text
GET /platform/reports/tenant-sales
GET /platform/reports/ops-health
GET /platform/reports/support-messages
GET /platform/reports/draw-results-quality
```

---

# 10. Pages réutilisées en contexte tenant

Ces pages ne doivent pas être dupliquées. Le superadmin y accède via tenant header.

| Domaine | Page admin | Page superadmin support | Endpoints |
|---|---|---|---|
| Tirages tenant | `AdminDrawsPage` | `TenantSupportDrawsPage` | `/admin/draws/**` |
| Vendeurs | `AdminSellerTerminalsPage` | `TenantSupportSellerTerminalsPage` | `/admin/seller-terminals/**` |
| Limites | `AdminLimitsPage` | `TenantSupportLimitsPage` | `/admin/policies/limits/**` |
| Gains à payer | `AdminPayoutOddsPage` | `TenantSupportPayoutOddsPage` | `/admin/controls/odds` |
| Commissions | `AdminCommissionsPage` | `TenantSupportCommissionsPage` | `/admin/commission/**` |
| Promotions | `AdminPromotionsPage` | `TenantSupportPromotionsPage` | `/admin/promotions/**` |
| Rapports | `AdminReportsPage` | `TenantSupportReportsPage` | `/tenant/reports/**` |
| Tickets | `AdminTicketsPage` | `TenantSupportTicketsPage` | `/tenant/tickets`, `/tenant/sales/preparations` |
| Mon entreprise | `AdminBusinessSettingsPage` | `TenantSupportBusinessSettingsPage` | `/admin/tenant/**`, `/admin/tenant-config/**`, `/admin/theme/**` |
| Subscription | `TenantSubscriptionPage` | `TenantSupportSubscriptionPage` | `/tenant/subscription`, `/platform/plans` |

---

# 11. Action registry recommandé côté web

Chaque page doit déclarer ses actions internes. Le sidenav ne porte pas les actions.

Exemple :

```ts
export interface PlatformActionDefinition {
  id: string;
  labelKey: string;
  icon: string;
  surface: 'platform' | 'tenant-support';
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  superadminOnly: boolean;
  tenantContextRequired: boolean;
  dangerous?: boolean;
  requiresReason?: boolean;
  bulk?: boolean;
}
```

Exemple d’action :

```ts
{
  id: 'platform.ops.drawResults.override',
  labelKey: 'platform.ops.drawResults.actions.override',
  icon: 'edit_note',
  surface: 'platform',
  endpoint: '/platform/ops/draw-results/override',
  method: 'POST',
  superadminOnly: true,
  tenantContextRequired: false,
  dangerous: true,
  requiresReason: true,
}
```

Exemple support tenant :

```ts
{
  id: 'tenant.draw.cancel',
  labelKey: 'admin.draws.actions.cancel',
  icon: 'cancel',
  surface: 'tenant-support',
  endpoint: '/admin/draws/{drawId}/cancel',
  method: 'POST',
  superadminOnly: false,
  tenantContextRequired: true,
  dangerous: true,
  requiresReason: true,
}
```

---

# 12. Gaps principaux à traiter

## Backend gaps

```text
/platform/contact-config GET/PUT
/platform/contact-requests/{id}/status PATCH
/platform/contact-requests/{id}/archive POST
/platform/reports/**
/admin/commission/sellers/{sellerTerminalId} PUT
/admin/controls/odds/default PUT
/admin/controls/odds/sellers/{sellerTerminalId} PUT/DELETE
/admin/seller-terminals/{id}/pin-reset POST
/admin/seller-terminals/{id}/status PATCH
```

## Web gaps

```text
Exposer toutes les pages Référentiels catalog
Créer Tchala pages
Créer Tests communication page
Créer Support & contenu pages
Créer Support tenant shell
Brancher Archives platform sur /platform/archive/**
Retirer Providers du menu Ops V0
Séparer Plans et Pricing dans Référentiels
```

---

# 13. Priorité d’implémentation V0

## P0 — Navigation et cohérence

```text
1. Nettoyer sidenav superadmin.
2. Retirer Providers du menu Ops.
3. Séparer Plans et Pricing.
4. Ajouter Tchala.
5. Ajouter Support & contenu.
6. Ajouter Support tenant.
```

## P1 — Pages déjà utiles avec endpoints existants

```text
1. Référentiels catalog : jeux, canaux, slots, plans, pricing, settings, themes, i18n, templates.
2. Ops : résultats, tirages, batch, cache, archives, audit, tests communication, sync identité.
3. Support contenu : messages contact, news publiques.
4. Tchala : suggestions, import, nettoyage.
```

## P2 — Support tenant réutilisé

```text
1. Tirages tenant.
2. Vendeurs.
3. Limites.
4. Commissions.
5. Gains à payer.
6. Promotions.
7. Rapports.
8. Subscription.
```

---

# 14. Résumé décisionnel

```text
Superadmin
= plateforme globale + ops + référentiels + support + sécurité.

Admin tenant
= gestion des ventes, vendeurs, tirages, limites, résultats, commissions.

Seller terminal
= vente, tickets, tirages disponibles, profil/PIN.
```

Le superadmin ne doit pas avoir un menu admin tenant dupliqué. Il doit avoir un **Support tenant** qui réutilise les pages admin avec un tenant context clair.

