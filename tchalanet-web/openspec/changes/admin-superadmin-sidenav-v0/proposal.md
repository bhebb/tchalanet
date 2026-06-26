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
├── Tableau de bord
│   ├── Ops plateforme              /platform
│   └── Tableau de bord commercial  /platform/dashboard
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

**Tableau de bord** : reste rendu par le moteur PageModel runtime. `/platform/dashboard` retourne un `PageRuntimeResponse` issu du template `private.dashboard.superadmin`, avec les `dynamic` résolus côté backend. Le frontend ne compose pas le cockpit avec des appels Angular parallèles.

`/platform/dashboard` exige un paramètre `logicalId`. Le provider `platform_admin_dashboard` délègue à un service de résolution qui sélectionne l'assembler correspondant :

- `private.dashboard.superadmin` → dashboard commercial superadmin V0 ;
- `private.dashboard.superadmin.ops` → dashboard Ops temporaire.

Le dashboard Ops utilise le même moteur PageModel et le même provider, mais un assembler séparé. Si d'autres pages platform arrivent, elles ajoutent un `logicalId` et un assembler dédié ; si la liste grandit, le switch pourra devenir un registry.

Le dashboard commercial ne construit pas les données Ops (`health`, alertes système). La route `/platform` est l'entrée "Ops plateforme" par défaut.

Dans l'UI superadmin, Ops est l'accueil par défaut (`/app/platform`) et la première entrée du groupe "Tableau de bord". Le commercial reste disponible via `/app/platform/dashboard`. Les anciens chemins comme `/app/platform/ops/health` peuvent rediriger vers l'accueil Ops, mais ne doivent pas être l'URL canonique de la sidenav.

**Overview platform** : retiré de l'UI V0. `GET /platform/overview` peut rester côté backend comme endpoint structurel, mais la route web `/platform/overview` et l'entrée sidenav "Vue d'ensemble" ne doivent pas servir le cockpit. Le groupement de navigation s'appelle "Dashboard".

**Tchala** : section dédiée, même si les endpoints sont en `/admin/tchala/**`. Le superadmin y accède globalement.

## PageModel runtime — séparation bootstrap / dashboard

Le runtime privé conserve deux appels conceptuels :

| Appel | Responsabilité | Ne doit pas contenir |
|---|---|---|
| Bootstrap privé (`/tenant/runtime/bootstrap`) | Initialisation session/app : utilisateur, rôles, espace, tenant actif, thème, locale, settings, notifications résumé, `pageModelRef` | KPI, charts, listes métier, dashboard data |
| Dashboard PageModel (`/platform/dashboard`, `/tenant/dashboard`) | Résolution d'une page runtime : `meta`, `shell`, `content`, `dynamic` | Initialisation session/app, logique auth, settings globaux |

Le moteur est unique : public, dashboard superadmin et dashboard admin tenant utilisent tous le contrat `PageRuntimeResponse`. La différence vient du `logicalId`, du template effectif et des `PageModelDynamicProvider`.

```text
Template/PageModelDoc
  -> binding.mode/source
  -> PageModelDynamicResolver
  -> PageModelDynamicProvider
  -> PageRuntimeAssembler
  -> PageRuntimeResponse
  -> Frontend PageModel Engine
```

Les fragments `jsonFile` servent aux morceaux de structure/configuration (`header`, `footer`, `shell`, fragments publics). Les sources métier (`tenant_admin_dashboard`, `platform_admin_dashboard`, `public_draw_results`, etc.) alimentent `dynamic.widgets`.

Les providers/assemblers PageModel doivent vivre dans `features.pagemodel.dynamic.providers.*`. Ils peuvent consommer `core.*.api`, `catalog.*.api` et `platform.*.api`, mais ne doivent pas appeler `features.tenantadmin`, `features.platformadmin`, `features.reporting` ou `features.stats`.

`features.stats` est legacy pour les nouveaux dashboards. Les KPI/charts business viennent de `core.analytics.api`. `features.reporting` reste réservé aux rapports filtrables/exportables.

### Charte cockpit

La charte UI fournie pour les dashboards admin est une direction visuelle, pas du HTML à copier. Le web doit réutiliser `libs/ui/theme`, `libs/ui/styles`, `libs/ui/components` et les widgets PageModel existants.

Principes à reprendre :

- surface claire, fond `--tch-color-background`, widgets sur `--tch-color-surface-container-lowest`;
- grille compacte type bento, sections KPI puis charts/listes/actions;
- typographie Plus Jakarta Sans via tokens `--tch-*`;
- accent gold via `--tch-color-accent` / containers existants, pas Tailwind;
- Material Symbols uniquement via les composants existants;
- pas d'assets externes, pas de script Tailwind, pas de background décoratif hors tokens;
- charts/tableaux seulement quand un widget PageModel dédié existe ou est ajouté dans `libs/widgets`.
- les listes de classement utilisent `RankingListWidget` avec un `items` binding (ex. `topTenants`), pas une page Angular dédiée.
- les séries temporelles utilisent `TrendChartWidget`; les répartitions utilisent `BreakdownListWidget`.

### Templates privés canoniques V0

Deux templates dashboard privés sont dans le périmètre de ce change :

| Template | Endpoint runtime | Source dynamic | Usage |
|---|---|---|---|
| `private.dashboard.tenant_admin` | `/tenant/dashboard` | `tenant_admin_dashboard` | Dashboard admin tenant |
| `private.dashboard.superadmin` | `/platform/dashboard` | `platform_admin_dashboard` | Dashboard superadmin/platform |

`seller-terminal` / `cashier` ne fait pas partie de ces deux templates dashboard. Le cashier web garde son runtime dédié `features.pos.home` via `/tenant/cashier/home`.

#### Admin tenant — modèle attendu

```json
{
  "logicalId": "private.dashboard.tenant_admin",
  "scope": "private",
  "slug": "dashboard",
  "model": {
    "meta": {
      "id": "private.dashboard.tenant_admin",
      "context": "private_dashboard_tenant_admin"
    },
    "shell": {
      "component": "PrivateShell",
      "binding": { "mode": "dynamic", "source": "jsonFile" },
      "props": { "fileKey": "private_shell_tenant_admin" }
    },
    "content": {
      "layout": {
        "component": "GridLayout",
        "rows": [
          { "id": "kpis", "columns": [{ "span": 12, "widgets": ["dashboard.tenantAdmin.kpis"] }] },
          { "id": "readinessAlerts", "columns": [
            { "span": 6, "widgets": ["dashboard.tenantAdmin.readiness"] },
            { "span": 6, "widgets": ["dashboard.tenantAdmin.alerts"] }
          ] },
          { "id": "commission", "columns": [{ "span": 12, "widgets": ["dashboard.tenantAdmin.commission"] }] },
          { "id": "quickActions", "columns": [{ "span": 12, "widgets": ["dashboard.tenantAdmin.quickActions"] }] }
        ]
      },
      "widgets": {
        "dashboard.tenantAdmin.kpis": {
          "type": "KpiGridWidget",
          "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
        },
        "dashboard.tenantAdmin.readiness": {
          "type": "ReadinessSummaryWidget",
          "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
        },
        "dashboard.tenantAdmin.alerts": {
          "type": "AlertsWidget",
          "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
        },
        "dashboard.tenantAdmin.commission": {
          "type": "CommissionSummaryWidget",
          "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
        },
        "dashboard.tenantAdmin.quickActions": {
          "type": "QuickActionsWidget",
          "binding": { "mode": "dynamic", "source": "tenant_admin_dashboard" }
        }
      }
    }
  }
}
```

#### Superadmin — modèle attendu

```json
{
  "logicalId": "private.dashboard.superadmin",
  "scope": "private",
  "slug": "dashboard",
  "model": {
    "meta": {
      "id": "private.dashboard.superadmin",
      "context": "private_dashboard_superadmin"
    },
    "shell": {
      "component": "PrivateShell",
      "binding": { "mode": "dynamic", "source": "jsonFile" },
      "props": { "fileKey": "private_shell_superadmin" }
    },
    "content": {
      "layout": {
        "component": "GridLayout",
        "rows": [
          { "id": "platformKpis", "columns": [
            { "span": 4, "widgets": ["dashboard.superadmin.tenants"] },
            { "span": 4, "widgets": ["dashboard.superadmin.platformSales"] },
            { "span": 4, "widgets": ["dashboard.superadmin.subscriptions"] }
          ] },
          { "id": "platformState", "columns": [
            { "span": 4, "widgets": ["dashboard.superadmin.health"] },
            { "span": 4, "widgets": ["dashboard.superadmin.onboarding"] },
            { "span": 4, "widgets": ["dashboard.superadmin.alerts"] }
          ] },
          { "id": "platformInsights", "columns": [
            { "span": 7, "widgets": ["dashboard.superadmin.topTenants"] },
            { "span": 5, "widgets": ["dashboard.superadmin.publicContent"] }
          ] },
          { "id": "contentActions", "columns": [
            { "span": 12, "widgets": ["dashboard.superadmin.quickActions"] }
          ] }
        ]
      },
      "widgets": {
        "dashboard.superadmin.health": {
          "type": "ReadinessSummaryWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.tenants": {
          "type": "KpiGridWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.platformSales": {
          "type": "KpiGridWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.subscriptions": {
          "type": "KpiGridWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.onboarding": {
          "type": "AlertsWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.alerts": {
          "type": "AlertsWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.publicContent": {
          "type": "NewsTickerWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.topTenants": {
          "type": "RankingListWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        },
        "dashboard.superadmin.quickActions": {
          "type": "QuickActionsWidget",
          "binding": { "mode": "dynamic", "source": "platform_admin_dashboard" }
        }
      }
    }
  }
}
```

Le premier rendu doit rester rapide. Si un widget devient coûteux, le template doit pouvoir le déclarer comme différé dans une évolution dédiée (`runtime.loadStrategy = deferred`) sans déplacer la composition dans Angular.

### Gap charts plateforme

`PlatformDashboardStatsView` expose aujourd'hui `summary` et `topTenants`. Il ne fournit pas encore :

- `dailyBreakdown` pour alimenter `TrendChartWidget`;
- `gameBreakdown` ou équivalent plateforme pour alimenter `BreakdownListWidget`.

Ces widgets existent côté web, mais ils ne doivent pas être branchés dans `private.dashboard.superadmin` tant que `core.analytics.api` n'a pas ajouté ces projections. À l'inverse, le dashboard admin tenant peut réutiliser ces widgets dès que son provider expose explicitement les séries/répartitions déjà disponibles côté `TenantDashboardStatsView`.

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
- Template `private.dashboard.superadmin` — cockpit superadmin via PageModel runtime

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

- `platform.routes.ts` — restructuration majeure + nouvelles routes, `/platform` et `/platform/dashboard` restent branchés sur le renderer PageModel
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
- Pas de nouvelle page Angular dashboard qui orchestre les domaines. Le cockpit utilise le BFF PageModel existant (`/platform/dashboard`) et `platform_admin_dashboard`.
- Pas d'audit viewer complet (pagination, filtres avancés)
- Pas de NgRx (signals + store service)
- Pas de gestion des gaps backend listés dans `gaps-backend.md`
- Pas de notify/webhook management
