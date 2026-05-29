# tenant-admin-runtime Specification

## Purpose
TBD - created by archiving change dashboard-overview-runtime-v1. Update Purpose after archive.
## Requirements
### Requirement: Tenant admin sidenav is fixed for V1

The tenant admin sidenav SHALL contain:

```text
Dashboard
Aperçu du tenant

Administration
- Utilisateurs
- Points de vente
- Terminaux
- Sessions

Jeux & ventes
- Tickets / Ventes
- Tirages
- Jeux & prix

Règles commerciales
- Limites
- Promotions

Personnalisation
- Paramètres
- Traductions
- Apparence

Rapports
- Rapports
```

### Requirement: Tenant admin dashboard displays KPI and short summaries

Dashboard content table:

| Category | Displayed on dashboard | Source |
|---|---|---|
| Header | tenant name, status, plan, timezone/currency | `tenant_admin_dashboard` |
| KPI activity | sales today, tickets today, active sessions, open draws, pending approvals | `tenant_admin_dashboard` |
| Alerts | unread notifications, top warnings | `tenant_admin_dashboard` / notification summary |
| Readiness short | top missing setup issues only | `tenant_admin_dashboard` via readiness summary |
| Operations summary | users/outlets/terminals short counts | `tenant_admin_dashboard` |
| Commercial summary | limits/promotions/games-pricing/draw-channels short status | `tenant_admin_dashboard` |
| Quick actions | create outlet, create terminal, create seller, view tenant overview | `tenant_admin_dashboard` or static props |

#### Scenario: Tenant admin dashboard displays KPI but not overview

- **WHEN** tenant admin dashboard loads
- **THEN** it includes real-time KPI
- **AND** it does not include the full tenant overview section list.

### Requirement: Tenant overview is a feature endpoint

Tenant overview SHALL be served by:

```http
GET /admin/overview
```

It SHALL NOT be a PageModel provider.

### Requirement: Tenant overview does not repeat dashboard KPIs

Tenant overview SHALL show structural diagnosis/navigation.

It MUST NOT include:

- `salesToday`
- `ticketCountToday`
- `activeSessions` as real-time KPI
- `openDraws` as real-time KPI
- dashboard top KPI cards

### Requirement: Tenant overview section table

| Section | Group | Route | Owner |
|---|---|---|---|
| Utilisateurs | Administration | `/app/admin/users` | `core.tenantuser` / `platform.identity` |
| Points de vente | Administration | `/app/admin/outlets` | `core.outlet` |
| Terminaux | Administration | `/app/admin/terminals` | `core.terminal` |
| Sessions | Administration | `/app/admin/sessions` | `core.session` |
| Tickets / Ventes | Jeux & ventes | `/app/admin/sales` | `core.sales` |
| Tirages | Jeux & ventes | `/app/admin/draws` | `core.draw` |
| Jeux & prix | Jeux & ventes | `/app/admin/games-pricing` | catalog/pricing owner |
| Limites | Règles commerciales | `/app/admin/limits` | `core.limitpolicy` |
| Promotions | Règles commerciales | `/app/admin/promotions` | `core.promotion` |
| Paramètres | Personnalisation | `/app/admin/settings` | `catalog.settings` / `platform.tenantconfig` |
| Traductions | Personnalisation | `/app/admin/i18n` | `catalog.i18n` |
| Apparence | Personnalisation | `/app/admin/appearance` | `catalog.theme` / `platform.tenanttheme` |
| Rapports | Rapports | `/app/admin/reports` | `features.tenantadmin.reports` |

#### Scenario: Tenant overview contains sections

- **WHEN** tenant overview loads
- **THEN** it returns sections matching the sidenav
- **AND** each section contains status, route, summary and issues
- **AND** it does not return dashboard KPI fields.

