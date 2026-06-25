# Feature PlatformAdmin

> **Surface** : Interface administration plateforme  
> **Scope** : `features.platformadmin` — vue globale plateforme, onboarding tenants  
> **Audience** : SUPER_ADMIN, PLATFORM_ADMIN  
> **Spec** : `openspec/specs/platform-admin-runtime/spec.md`

---

## Rôle

BFF de l'administrateur plateforme. Fournit :
- Vue globale de tous les tenants (santé, KPIs, alertes)
- Onboarding de nouveaux tenants (preview + provisioning)
- Overview structurel de la plateforme

---

## Surfaces

| Surface | Source | Notes |
|---|---|---|
| Dashboard | PageModel `DASHBOARD_SUPERADMIN` · source `platform_admin_dashboard` | Santé, tenants, subscriptions, alertes |
| Overview | `GET /platform/overview` | Vue structurelle, pas de KPIs |
| Onboarding | `POST /platform/tenant-onboarding/*` | Preview + provisioning |

---

## Navigation (sidenav fixe V1)

```
Dashboard
Aperçu plateforme

Plateforme
  Tenants                     /app/platform/tenants
  Onboarding tenants          /app/platform/onboarding
  Plans / Subscriptions       /app/platform/plans

Configuration globale
  Paramètres globaux          /app/platform/settings
  Traductions globales        /app/platform/i18n
  Thèmes / presets            /app/platform/themes
  Jeux / référentiels         /app/platform/games
  Draw channels globaux       /app/platform/draw-channels

Opérations
  Santé plateforme            /app/platform/health
  Jobs / schedulers           /app/platform/jobs
  Cache                       /app/platform/cache
  Audit                       /app/platform/audit
  Notifications / comms       /app/platform/notifications

Rapports
  Rapports plateforme         /app/platform/reports
```

---

## Endpoints

### Dashboard

Servi par PageModel — source `platform_admin_dashboard`.

Contenu :
- **Health** : statut API, DB, Keycloak, cache, scheduler, notifications
- **Tenant KPIs** : total, actifs, suspendus, onboarding en cours / action requise
- **Subscriptions** : active, trial, past due, expired
- **Onboarding alerts** : tenants récents ou incomplets
- **Platform alerts** : services dégradés, échecs scheduler/cache/notification
- **Quick actions** : créer tenant, voir overview, ouvrir ops health

### Overview plateforme

```http
GET /platform/overview
```

Vue structurelle — ne contient pas les KPIs temps-réel du dashboard.

### Onboarding tenant

```http
POST /platform/tenant-onboarding/preview    ← simulation read-only
POST /platform/tenant-onboarding/provision  ← provisioning effectif
```

Voir flow complet : [`tenant-onboarding`](../../../../../../tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md)

**Profils V1** : `MINIMAL` / `DEFAULT_HAITI_LOTTERY` / `DEMO`

**Preview** : retourne domaines provisionnés, warnings, données non copiées, readiness attendue. Aucune donnée écrite.

**Provision** : orchestre 11 domaines dans l'ordre (tenant base, identity, theme, settings, i18n, games, pricing, draw channels, promotions, limitpolicy, pagemodels). Retourne statut par domaine + readiness + next steps.

---

## Providers PageModel

| Source | Provider | Surface |
|---|---|---|
| `platform_admin_dashboard` | `features.pagemodel.dynamic.providers.platformadmin.PlatformAdminDashboardProvider` | Dashboard platform |

Les providers PageModel ne vivent pas dans `features.platformadmin`. Cette feature garde les endpoints platform classiques (`overview`, onboarding, tenant management), tandis que `features.pagemodel` compose le runtime PageModel.

---

## Frontières

`features.platformadmin` ne doit pas :
- contenir de logique de validation métier des domaines
- appeler directement les tables `core.*` ou `catalog.*`
- inclure des KPIs temps-réel dans l'overview (dashboard uniquement)
- contenir de providers/assemblers PageModel dashboard

---

## Références

- Spec : `openspec/specs/platform-admin-runtime/spec.md`
- Provisioning : `tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md`
- Spec provisioning : `openspec/specs/tenant-provisioning/spec.md`
- Dashboard providers : `FEATURE_PAGEMODEL.md` dans `features.pagemodel`
