# Change: Superadmin Sidenav V0

## Status

Proposal

## Why

Le menu superadmin était fragmenté sans vision produit claire. Les routes `/platform/catalog/*` et `/platform/ops/*` existent déjà, mais la sidenav ne les expose pas de manière cohérente. Plusieurs sections importantes sont manquantes (Tchala, Tests communication, Synchronisation identité, Support tenant, Rapports platform) et des sections techniques (Providers) apparaissent dans le menu alors qu'elles sont vides.

Ce change réorganise la sidenav superadmin complète, corrige l'organisation des routes existantes, et ajoute les sections et pages manquantes — en partant du principe fondamental : **le superadmin gère la plateforme, pas les tenants directement**.

## Périmètre absorbé

Ce change absorbe **tous les Slices restants** (1–4) de `platform-superadmin-and-tenant-admin-pages`. La raison : les pages platform (ops-batch, ops-draws, ops-draw-results, ops-cache, audit) existent déjà comme routes. Le travail réel était la sidenav, l'infrastructure transverse (SupportAccessStore, AdminOverrideBanner) et les quelques pages manquantes — tout ça est couvert ici.

`platform-superadmin-and-tenant-admin-pages` peut être archivé ou fermé après le Slice 0.

## Deux modes superadmin

| Mode | Description | Accès |
|---|---|---|
| Platform global | Actions globales sans tenant ciblé | `/app/platform/**` |
| Support tenant | Réutilise les pages admin avec tenant header | via `Support tenant` > sélection tenant |

La règle fondamentale :

```
Le superadmin ne doit pas avoir un menu admin tenant dupliqué.
Il accède aux fonctionnalités tenant via Support tenant,
avec un contexte tenant explicite et un bandeau permanent.
```

## Sidenav cible

```
Platform
├── Vue d'ensemble
│   ├── Tableau de bord             /platform
│   └── Santé système               /platform/ops/health
│
├── Tenants
│   ├── Liste des tenants           /platform/tenants
│   ├── Onboarding tenant           /platform/tenants/onboarding
│   ├── Admins tenant               /platform/tenant-admins
│   └── Support tenant              /platform/support-tenant
│
├── Référentiels
│   ├── Jeux                        /platform/catalog/games
│   ├── Canaux de tirage            /platform/catalog/draw-channels
│   ├── Jeux par canal              /platform/catalog/draw-channel-games
│   ├── Slots de résultats          /platform/catalog/result-slots
│   ├── Calendriers des slots       /platform/catalog/result-slot-calendars
│   ├── Plans                       /platform/catalog/plans
│   ├── Pricing                     /platform/catalog/pricing
│   ├── Paramètres globaux          /platform/catalog/settings
│   ├── Thèmes                      /platform/catalog/themes
│   ├── Traductions                 /platform/catalog/translations
│   └── Templates de pages          /platform/catalog/page-model-templates
│
├── Opérations
│   ├── Tirages                     /platform/ops/draws
│   ├── Résultats                   /platform/ops/draw-results
│   ├── Tâches planifiées           /platform/ops/batch
│   ├── Cache                       /platform/ops/cache
│   ├── Archives                    /platform/ops/archives
│   ├── Tests communication         /platform/ops/communication-tests
│   ├── Synchronisation identité    /platform/ops/identity-sync
│   └── Audit                       /platform/ops/audit
│
├── Support & contenu
│   ├── Messages de contact         /platform/contact-requests
│   ├── News publiques              /platform/news
│   ├── Notifications               /platform/notifications
│   └── Configuration contact       /platform/contact-config
│
├── Tchala
│   ├── Suggestions                 /platform/tchala/suggestions
│   ├── Import                      /platform/tchala/import
│   └── Nettoyage                   /platform/tchala/cleanup
│
├── Accès & sécurité
│   ├── Permissions                 /platform/access/permissions
│   ├── Rôles                       /platform/access/roles
│   ├── Super admins                /platform/super-admins
│   ├── Utilisateurs                /platform/access/users
│   └── Clés publiques backend      /platform/access/backend-keys
│
└── Rapports platform               /platform/reports
```

## État actuel des routes et pages

Avant ce change, la situation est :

| Section | Routes | Pages | Sidenav |
|---|---|---|---|
| Tableau de bord | ✓ `/platform` | PrivateDashboardPage (placeholder) | ✓ |
| Tenants | ✓ `/platform/tenants/**` | Partielles (onboarding ok, list WIP) | ✓ |
| Référentiels catalog | ✓ `/platform/catalog/**` | ✓ Toutes existent | Non organisé |
| Ops Tirages/Résultats/Cache | ✓ `/platform/ops/**` | ✓ Toutes existent | ✓ |
| Ops Batch/Archives/Audit | ✓ `/platform/ops/**` | ✓ Toutes existent | ✓ |
| Ops Tests comm / Sync identité | ✗ manquant | ✗ manquant | ✗ |
| Support & contenu (Contact/News) | ✓ routes existantes | ✓ pages existantes | Scattered |
| Notifications | ✓ route existante | ✓ page existante | Scattered |
| Config contact | ✗ manquant | ✗ manquant | ✗ |
| Tchala | ✗ manquant | ✗ manquant | ✗ |
| Accès : Permissions/Rôles | ✓ routes | Placeholder | ✓ |
| Super admins | ✓ routes | ✓ pages | ✓ |
| Accès : Utilisateurs / Clés | ✗ manquant | ✗ manquant | ✗ |
| Support tenant shell | ✗ manquant | ✗ manquant | ✗ |
| Rapports platform | Route `/platform/reports` → audit | ✗ manquant | ✓ (mauvaise page) |
| Plans séparé / Pricing séparé | `plans-pricing` fusionné | Une seule page | ✗ |
| Providers | Route `/platform/ops/providers` | Placeholder | Dans menu Ops |

## Décisions validées

**Providers** : retirer du menu Ops V0. Route gardée comme redirect ou placeholder caché.

**Plans et Pricing** : séparés dans Référentiels. Route actuelle `plans-pricing` → split en `/catalog/plans` et `/catalog/pricing`.

**Jeux par canal / Calendriers des slots** : nouvelles entrées dans Référentiels (endpoints existent côté backend).

**Support tenant** : shell dédié qui charge les pages admin avec tenant header. Un bandeau permanent affiche le tenant actif. Les pages admin (`/app/admin/**`) sont réutilisées — pas dupliquées.

**Rapports platform** : nouvelle page dédiée, pas un redirect vers audit.

**Tableau de bord** : remplace `PrivateDashboardPage` par une vraie page cockpit superadmin.

**Tchala** : section dédiée, même si les endpoints sont en `/admin/tchala/**`. Le superadmin y accède globalement.

## Infrastructure transverse (absorbée de platform-superadmin-and-tenant-admin-pages)

- `SupportAccessStore` — signal store `core/tenant-admin-access/` — état du contexte tenant actif superadmin
- `SensitiveDataMaskPipe` — masque phone/email/amounts en mode support readonly
- `AdminOverrideBanner` — bannière visible dans tout `/app/admin/**` quand session support active

## Action registry

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

## What changes

### Nouveaux fichiers

**Infrastructure transverse** (`core/tenant-admin-access/`, `features/private/shared/`) :
- `support-access.store.ts`
- `sensitive-data-mask.pipe.ts`
- `admin-override-banner.ts`

**Sidenav** :
- `NavigationSection[]` platform restructuré (localiser et mettre à jour le composant existant)

**Registry d'actions** :
- `features/private/platform/shared/platform-action.model.ts`

**Vue d'ensemble** :
- `platform-home.page.ts` — cockpit superadmin avec widgets KPI

**Support tenant** :
- `platform/support-tenant/platform-support-tenant.page.ts` — sélecteur tenant + shell de réutilisation
- `platform/support-tenant/platform-support-tenant.routes.ts`

**Référentiels manquants** :
- `catalog/pages/draw-channel-games/platform-catalog-draw-channel-games.page.ts`
- `catalog/pages/result-slot-calendars/platform-catalog-result-slot-calendars.page.ts`

**Catalog split Plans/Pricing** :
- `catalog/pages/plans/platform-catalog-plans.page.ts` (renommer/splitter `plans-pricing`)
- `catalog/pages/pricing/platform-catalog-pricing.page.ts` (déjà existant dans catalog routes)

**Ops manquants** :
- `operations/pages/communication-tests/platform-ops-communication-tests.page.ts`
- `operations/pages/identity-sync/platform-ops-identity-sync.page.ts`

**Support & contenu** :
- `pages/contact-config/platform-contact-config.page.ts`

**Tchala** :
- `tchala/platform-tchala-suggestions.page.ts`
- `tchala/platform-tchala-import.page.ts`
- `tchala/platform-tchala-cleanup.page.ts`
- `tchala/platform-tchala.routes.ts`

**Accès & sécurité** :
- `pages/access/platform-users.page.ts`
- `pages/access/platform-backend-keys.page.ts`

**Rapports platform** :
- `pages/reports/platform-reports.page.ts`

### Fichiers modifiés

- `platform.routes.ts` — restructuration majeure + nouvelles routes
- `platform-catalog.routes.ts` — split plans-pricing, ajouter draw-channel-games, result-slot-calendars
- `platform-operations.routes.ts` — ajouter communication-tests, identity-sync, retirer providers du rendu actif
- Shell privé — intégrer `AdminOverrideBanner`

## Impact

- `apps/tch-portal/src/app/features/private/platform/` — refonte sidenav + nouvelles pages
- `apps/tch-portal/src/app/core/tenant-admin-access/` — SupportAccessStore
- `apps/tch-portal/src/app/features/private/shared/` — AdminOverrideBanner, SensitiveDataMaskPipe
- `platform-superadmin-and-tenant-admin-pages` : tous les slices superseded

## Non-goals V0

- Pas de gestion des Providers (section retirée du menu, route conservée en hidden)
- Pas de BFF platform dashboard (appels parallèles comme admin-tenant-sidenav-v0)
- Pas d'audit viewer complet (pagination, filtres avancés)
- Pas de NgRx (signals + store service)
- Pas de gestion des gaps backend listés dans `gaps-backend.md`
- Pas de notify/webhook management
