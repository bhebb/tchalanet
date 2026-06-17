# Design: Admin Dashboard & Stats V0

## Menu

Target V0 admin menu:

```text
Tableau de bord
Ventes
Terminaux / Vendeurs
Résultats
Gagnants / Paiements
Contrôle
  - Exposition
  - Limites
  - Terminaux bloqués
Rapports
  - Ventes
  - Vendeurs / Terminaux
  - Fiches vendues
  - Fiches gagnantes
  - Fiches payées
  - Fiches éliminées
Configuration
  - Tirages
  - Jeux
  - Odds
  - Limites
  - Commissions
  - Succursales optionnelles
Mon compte
```

## Dashboard KPIs

V0 top cards:

```text
Total ventes aujourd'hui
Montant à payer
Commissions vendeurs
Terminaux actifs / bloqués / désactivés
```

Secondary widgets:

```text
Top vendeurs/terminaux
Dernières ventes
Résultats à entrer
Tickets gagnants récents
Exposition critique
Terminaux bloqués
```

## Language

UI may use business vocabulary:

```text
Fiche = Ticket
Agent/Vendeur = SellerTerminal
Succursale = optional Outlet
```

Use combined labels where helpful:

```text
Fiches / Tickets
Vendeurs / Terminaux
```

## Backend Placement

Use:

```text
features.dashboard
features.reporting
```

because dashboards aggregate multiple domains.

Do not put dashboard aggregation logic in controllers.

Do not put dashboard read models inside `core.sales` unless they are single-domain reusable queries.

## Read Models

Suggested DTOs:

```text
AdminDashboardView
SalesKpiCard
TerminalStatusKpiCard
CommissionKpiCard
PayoutKpiCard
TerminalSalesSummaryRow
RecentSaleRow
ExposureSummaryRow
ResultPendingRow
UnpaidWinnerRow
```

## Data Sources

```text
core.sales        -> sales totals, recent tickets
core.terminal     -> terminal status and names
core.limitpolicy  -> exposure and limit warnings
core.drawresult   -> pending/manual result status
core.payout       -> unpaid winners / paid amounts
core.pricing      -> odds labels if needed
```

## API

```http
GET /admin/dashboard
GET /admin/reports/sales
GET /admin/reports/terminals
GET /admin/reports/tickets/sold
GET /admin/reports/tickets/winning
GET /admin/reports/tickets/paid
GET /admin/reports/tickets/eliminated
GET /admin/control/exposure
```

## Security

Admin dashboard:

```java
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@PreAuthorize("hasPermission('report.read')")
```

Some control pages may require:

```java
@PreAuthorize("hasPermission('limit.read')")
@PreAuthorize("hasPermission('terminal.read')")
```

## Query Rules

- Always use tenant context from `TchRequestContext`.
- Never accept tenant id from request body for tenant/admin dashboard.
- Use pagination for report tables.
- Use bounded result sets for dashboard widgets.
- Use DB aggregation, not in-memory aggregation.
- Avoid exposing raw `ticket_line` as the default admin view.

## Dashboard Freshness

V0 can use direct DB queries with indexes.

Future optimization may add projections/materialized stats, but V0 should not overbuild.

Recommended indexes:

```sql
ticket(tenant_id, sold_at)
ticket(tenant_id, seller_terminal_id, sold_at)
ticket(tenant_id, draw_id, sold_at)
ticket_line(tenant_id, draw_id, selection)
payout(tenant_id, status)
seller_terminal(tenant_id, status)
```
