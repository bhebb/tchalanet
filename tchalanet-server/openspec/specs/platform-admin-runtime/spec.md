# platform-admin-runtime Specification

## Purpose
TBD - created by archiving change dashboard-overview-runtime-v1. Update Purpose after archive.
## Requirements
### Requirement: Platform admin sidenav is fixed for V1

The platform admin sidenav SHALL contain:

```text
Dashboard
Aperçu plateforme

Plateforme
- Tenants
- Onboarding tenants
- Plans / Subscriptions

Configuration globale
- Paramètres globaux
- Traductions globales
- Thèmes / presets
- Jeux / référentiels
- Draw channels globaux

Opérations
- Santé plateforme
- Jobs / schedulers
- Cache
- Audit
- Notifications / communications

Rapports
- Rapports plateforme
```

### Requirement: Platform dashboard uses PageModel source

Platform dashboard SHALL use provider source:

```text
platform_admin_dashboard
```

Dashboard content table:

| Category | Displayed on dashboard | Source |
|---|---|---|
| Health | API, DB, Keycloak, cache, scheduler, notifications | `platform_admin_dashboard` |
| Tenant KPI | total, active, suspended, onboarding/action required | `platform_admin_dashboard` |
| Subscriptions | active, trial, past due, expired | `platform_admin_dashboard` |
| Onboarding alerts | recent/incomplete tenants | `platform_admin_dashboard` |
| Platform alerts | degraded services, scheduler/cache/notification failures | `platform_admin_dashboard` |
| Quick actions | create tenant, view platform overview, open ops health | `platform_admin_dashboard` |

### Requirement: Platform overview is a feature endpoint

Platform overview SHALL be served by:

```http
GET /platform/overview
```

It SHALL NOT be a PageModel provider.

### Requirement: Platform overview does not repeat dashboard KPIs

Platform overview SHALL show structural/operational section diagnosis and SHALL NOT become a dashboard bis.

### Requirement: Platform overview section table

| Section | Group | Route | Owner |
|---|---|---|---|
| Tenants | Plateforme | `/app/platform/tenants` | platform tenant owner |
| Onboarding tenants | Plateforme | `/app/platform/tenant-onboarding` | `features.platformadmin.tenantonboarding` |
| Plans / Subscriptions | Plateforme | `/app/platform/subscriptions` | subscription/plan owner |
| Paramètres globaux | Configuration globale | `/app/platform/settings` | `catalog.settings` / platform config |
| Traductions globales | Configuration globale | `/app/platform/i18n` | `catalog.i18n` |
| Thèmes / presets | Configuration globale | `/app/platform/theme-presets` | `catalog.theme` |
| Jeux / référentiels | Configuration globale | `/app/platform/referentials` | catalog owners |
| Draw channels globaux | Configuration globale | `/app/platform/draw-channels` | catalog/draw channel owner |
| Santé plateforme | Opérations | `/app/platform/ops/health` | platform ops / features |
| Jobs / schedulers | Opérations | `/app/platform/ops/jobs` | batch/scheduler ops |
| Cache | Opérations | `/app/platform/ops/cache` | platform cache ops |
| Audit | Opérations | `/app/platform/audit` | `platform.audit` |
| Notifications / communications | Opérations | `/app/platform/communications` | `platform.notification` / `platform.communication` |
| Rapports plateforme | Rapports | `/app/platform/reports` | `features.platformadmin.reports` |

