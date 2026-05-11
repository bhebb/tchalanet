# Provider Query Cards

Use this checklist for each stub.

```text
Provider:
Source:
Widget:
Owner domain:
Existing query? yes/no/name
New query:
Handler:
Reader port:
SQL adapter:
DTO:
Props:
Limit clamp:
Fallback:
Cache:
Security:
```

## Cashier recent tickets

```text
Provider: CashierRecentTicketsProvider
Source: cashier_recent_tickets
Widget: RecentTicketsWidget
Owner domain: core.sales
Existing query: probably no
New query: ListCashierRecentTicketsQuery
Handler: ListCashierRecentTicketsQueryHandler
Reader port: TicketSummaryReaderPort.findRecentByCashier
SQL adapter: JdbcTicketSummaryReaderAdapter
DTO: CashierRecentTicketView
Props: limit default 5
Limit clamp: 1..20
Fallback: []
Cache: no MVP
Security: tenantId/cashierId from TchRequestContext
```

## Public draw results

```text
Provider: PublicDrawResultsProvider
Source: public_draw_results
Widget: PublicDrawResultsWidget / DrawResultsPageWidget
Owner domain: core.drawresult
Existing query: verify
New query: ListPublicDrawResultsBySlotQuery
Handler: ListPublicDrawResultsBySlotQueryHandler
Reader port: PublicDrawResultReaderPort
SQL adapter: JdbcPublicDrawResultReaderAdapter
DTO: PublicDrawResultsBySlotView
Props: include_latest_by_slot, include_next_by_slot, include_history, history_limit
Limit clamp: history 0..10
Fallback: slots=[]
Cache: later in core.drawresult
Security: public-safe masked response
```

## Admin KPIs

```text
Provider: AdminKpisProvider
Source: admin_kpis
Widget: AdminKpisWidget
Owner domain: core.sales
Existing query: verify
New query: GetAdminDashboardKpisQuery
Handler: GetAdminDashboardKpisQueryHandler
Reader port: AdminSalesKpiReaderPort
SQL adapter: JdbcAdminSalesKpiReaderAdapter
DTO: AdminDashboardKpisView
Props: period default today
Limit clamp: n/a
Fallback: zeroed KPI view if no tickets
Cache: later if expensive
Security: tenant admin/operator only
```
