# Feature TenantAdmin — Guide Administrateur Tenant

> **Surface** : Interface administration tenant  
> **Scope** : `features.tenantadmin` — diagnostic structurel, readiness, policies  
> **Audience** : TENANT_ADMIN, SUPER_ADMIN  
> **Spec** : `openspec/specs/tenant-admin-runtime/spec.md`

---

## Rôle

BFF de l'administrateur tenant. Fournit une vue structurelle du tenant (readiness, sections de navigation, overview), distincte des KPIs temps-réel du dashboard.

Le dashboard tenant utilise le PageModel (`tenant_admin_dashboard`).  
L'overview tenant est un endpoint feature dédié.

---

## Surfaces

| Surface | Source | Notes |
|---|---|---|
| Dashboard | PageModel `DASHBOARD_TENANT_ADMIN` · source `tenant_admin_dashboard` | KPIs temps-réel, alertes, résumés |
| Overview | `GET /admin/overview` | Diagnostic structurel, sections, readiness |
| Policies | `GET /admin/policies/overview` | Résumé limites, autonomy |

---

## Navigation (sidenav fixe V1)

```
Dashboard
Aperçu du tenant

Administration
  Utilisateurs           /app/admin/users         → core.tenantuser / platform.identity
  Points de vente        /app/admin/outlets        → core.outlet
  Terminaux              /app/admin/terminals      → core.terminal
  Sessions               /app/admin/sessions       → core.session

Jeux & ventes
  Tickets / Ventes       /app/admin/sales          → core.sales
  Tirages                /app/admin/draws          → core.draw
  Jeux & prix            /app/admin/games-pricing  → catalog / pricing

Règles commerciales
  Limites                /app/admin/limits         → core.limitpolicy
  Promotions             /app/admin/promotions     → core.promotion

Personnalisation
  Paramètres             /app/admin/settings       → catalog.settings / platform.tenantconfig
  Traductions            /app/admin/i18n           → catalog.i18n
  Apparence              /app/admin/appearance     → catalog.theme / platform.tenanttheme

Rapports
  Rapports               /app/admin/reports        → features.tenantadmin.reports
```

---

## Endpoints

### Overview tenant

```http
GET /admin/overview
```

Retourne :
- **Header** : tenantId, nom, statut, plan, timezone
- **Status global** : `READY` / `PARTIAL` / `MISSING` / `UNKNOWN`
- **missingCount** : nombre de sections en défaut
- **Sections** : une par entrée sidenav — statut, résumé, issues, route

> Ne contient pas de KPIs temps-réel (salesToday, activeSessions, etc.).  
> Ces données sont dans le dashboard PageModel.

### Policies overview

```http
GET /admin/policies/overview
```

Retourne :
- `tenantAssignmentsCount` : nombre d'assignations actives
- `autonomyConfigured` : autonomy configurée ?
- `autonomyLevel` : niveau configuré

---

## Readiness tenant

La readiness est structurelle — elle vérifie la complétude de la configuration :

| Section | Ce qui est vérifié |
|---|---|
| Utilisateurs | Au moins un admin actif |
| Points de vente | Au moins un outlet actif |
| Terminaux | Au moins un terminal actif/bindable |
| Jeux & prix | Jeux activés, pricing couvert |
| Tirages | Draw channels configurés |
| Limites | Templates ou policies actives |
| PageModels | Documents publiés présents |
| Paramètres | Settings par défaut présents |
| Traductions | i18n minimum présent |
| Apparence | Thème présent |

**Statuts** : `READY` / `PARTIAL` / `MISSING` / `UNKNOWN`

La readiness est projetée différemment selon le contexte :
- `TenantReadinessSummary` → dashboard (résumé court)
- `TenantReadinessView` → overview (sections complètes)
- `TenantReadinessView` → résultat de provisioning

---

## Offline (V2+)

`FEATURE_TENANTADMIN_OFFLINE.md` — file d'attente review, dashboard risque, pénalités sellers, limites offline.  
Les stats de vente officielles restent dans `core.sales`.

---

## Frontières

`features.tenantadmin` ne doit pas :
- inclure des KPIs temps-réel dans l'overview
- dupliquer les données du dashboard PageModel
- posséder la logique de validation métier des domaines

---

## Références

- Spec : `openspec/specs/tenant-admin-runtime/spec.md`
- Readiness : `openspec/specs/tenant-readiness/spec.md`
- Provisioning (setup initial) : `tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md`
- Dashboard (KPIs) : providers PageModel `tenant_admin_dashboard`
