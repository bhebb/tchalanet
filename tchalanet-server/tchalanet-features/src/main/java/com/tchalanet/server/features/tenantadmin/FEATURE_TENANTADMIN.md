# Feature TenantAdmin

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
  Utilisateurs           /app/admin/users               → core.tenantuser / platform.identity
  Seller-terminals       /app/admin/seller-terminals     → core.sellerterminal
  Points de vente        /app/admin/outlets              → core.outlet (groupement optionnel)

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
  Financials             /app/admin/financials     → core.analytics
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

### Draw overview

```http
GET /admin/draws/{drawId}/overview
```

Agrège en une seule réponse :
- **Draw metadata** : drawId, label, status, drawChannelId, cutoff
- **topSelections** : top 5 sélections par mise totale sur ce tirage
- **effectiveLimits** : résolution LimitResolver sur scopes TENANT + DRAW_CHANNEL uniquement
- **exposureAlerts** : lignes draw_exposure avec ratio stakeTotal/limitCents

L'admin BFF ne résout **pas** au niveau SELLER_TERMINAL — la requête admin ne porte pas de sellerTerminalId ; résoudre à ce scope sans terminal identifié n'aurait pas de sens.

`effectiveLimits` représente donc le plancher tenant + canal : la vue la plus large applicable sans contexte terminal.

C'est la source de données pour la section **"Numéros à risque"** du draw detail web (`/app/admin/draws/:drawId`).

### Limits management

Ces endpoints exposent la gestion des règles de limite (première documentation dans FEATURE_TENANTADMIN — les détails du moteur sont dans `DOMAIN_LIMITPOLICY.md` section 32).

```http
GET    /admin/policies/limits/rules
GET    /admin/policies/limits/assignments
PUT    /admin/policies/limits/assignments
DELETE /admin/policies/limits/assignments/{id}
GET    /admin/policies/limits/exposure
```

| Endpoint | Rôle |
|---|---|
| `GET /admin/policies/limits/rules` | Catalogue JSON des règles disponibles (RuleKey + metadata UI) |
| `GET /admin/policies/limits/assignments` | Liste les LimitAssignment actifs du tenant |
| `PUT /admin/policies/limits/assignments` | Créer ou modifier un LimitAssignment (upsert) |
| `DELETE /admin/policies/limits/assignments/{id}` | Soft-delete d'un LimitAssignment |
| `GET /admin/policies/limits/exposure` | Vue paginée brute des lignes draw_exposure |

Portées configurables depuis l'UI : `TENANT`, `DRAW_CHANNEL`, `SELLER_TERMINAL`. `TENANT` est la portée par défaut.

Le controller ne contient pas de logique métier — il construit des commands/queries et repose sur `TchRequestContext` pour le tenantId (jamais fourni par le client).

---

## Readiness tenant

La readiness est structurelle — elle vérifie la complétude de la configuration :

| Section | Ce qui est vérifié |
|---|---|
| Utilisateurs | Au moins un admin actif |
| Seller-terminals | Au moins un SellerTerminal actif |
| Points de vente | Outlet configuré si utilisé (optionnel) |
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
- contenir de providers/assemblers PageModel dashboard

Les providers PageModel `tenant_admin_dashboard` vivent dans
`features.pagemodel.dynamic.providers.tenantadmin`. Ils consomment les API `core`, `catalog` et
`platform`, mais ne doivent pas appeler `features.tenantadmin`.

### Financials

Les drilldowns d'exploitation financière pour l'admin tenant vivent dans
`features.reporting.financials`. Ils consomment
`core.analytics.api.GetTenantFinancialBreakdownQuery` via `QueryBus` et ne lisent jamais les tables
analytics directement.

La vue sépare:

- commissions seller-terminal snapshotées;
- charges buyer/seller/tenant/waived;
- métriques promotions;
- net revenue estimé et paid-basis.

V1 expose les axes `jour`, `tirage`, `seller-terminal/jour` et `seller-terminal × tirage`.
Le croisement exact vient de `analytics_seller_terminal_draw`, pas d'une extrapolation UI.

---

## Références

- Spec : `openspec/specs/tenant-admin-runtime/spec.md`
- Readiness : `openspec/specs/tenant-readiness/spec.md`
- Provisioning (setup initial) : `tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md`
- Dashboard (KPIs) : providers PageModel `tenant_admin_dashboard`
