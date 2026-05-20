# Source Registry — Dynamic PageModel Providers

## Naming convention

All dynamic sources use `snake_case`.

Avoid legacy mixed names:

| Avoid              | Use                      |
| ------------------ | ------------------------ |
| `news`             | `public_news`            |
| `quick_sale`       | `cashier_quick_sale`     |
| `recent_tickets`   | `cashier_recent_tickets` |
| `session`          | `cashier_session`        |
| `cashier.overview` | `cashier_overview`       |

## MVP public sources

| Source                | Provider                    | Owner domain                     | Query/service                                | SQL owner           | MVP   |
| --------------------- | --------------------------- | -------------------------------- | -------------------------------------------- | ------------------- | ----- |
| `public_news`         | `PublicNewsProvider`        | `features.news`                  | `ListPublicNewsQuery` or `PublicNewsService` | news feature        | Yes   |
| `public_draw_results` | `PublicDrawResultsProvider` | `core.drawresult`                | `ListPublicDrawResultsBySlotQuery`           | drawresult adapter  | Yes   |
| `public_tchala`       | `PublicTchalaProvider`      | `catalog.tchala` or static       | `ListPublicTchalaEntriesQuery`               | catalog/static      | Later |
| `public_plans`        | `PublicPlansProvider`       | `catalog.plan` or static         | `ListPublicPlansQuery`                       | catalog/static      | Later |
| `json_file`           | `JsonFileProvider`          | `features.pagemodel` config only | none                                         | classpath fragments | Yes   |

## MVP cashier sources

| Source                   | Provider                       | Owner domain                   | Query                                               | Reader port                  | SQL owner             | MVP |
| ------------------------ | ------------------------------ | ------------------------------ | --------------------------------------------------- | ---------------------------- | --------------------- | --- |
| `cashier_overview`       | `CashierOverviewProvider`      | `core.sales`                   | `GetCashierDashboardOverviewQuery`                  | `CashierDashboardReaderPort` | sales adapter         | Yes |
| `cashier_quick_sale`     | `CashierQuickSaleProvider`     | `core.sales` composition       | `GetCashierQuickSaleModelQuery`                     | compose read APIs            | mixed via APIs        | Yes |
| `cashier_recent_tickets` | `CashierRecentTicketsProvider` | `core.sales`                   | `ListCashierRecentTicketsQuery`                     | `TicketSummaryReaderPort`    | sales adapter         | Yes |
| `cashier_session`        | `CashierSessionProvider`       | session owner / core.sales MVP | `GetCashierSessionSummaryQuery`                     | `CashierSessionReaderPort`   | session/sales adapter | Yes |
| `cashier_next_draws`     | `CashierNextDrawsProvider`     | `core.draw`                    | `ListCashierNextDrawsQuery` or `ListNextDrawsQuery` | `DrawSummaryReaderPort`      | draw adapter          | Yes |

## MVP admin sources

| Source                  | Provider                      | Owner domain                  | Query                                | Reader port                | SQL owner       | MVP                |
| ----------------------- | ----------------------------- | ----------------------------- | ------------------------------------ | -------------------------- | --------------- | ------------------ |
| `admin_kpis`            | `AdminKpisProvider`           | `core.sales`                  | `GetAdminDashboardKpisQuery`         | `AdminSalesKpiReaderPort`  | sales adapter   | Yes                |
| `admin_approval_queue`  | `AdminApprovalQueueProvider`  | `core.sales`                  | `ListPendingSalesApprovalsQuery`     | `SalesApprovalReaderPort`  | sales adapter   | Yes                |
| `admin_draw_operations` | `AdminDrawOperationsProvider` | `core.draw`                   | `GetAdminDrawOperationsSummaryQuery` | `DrawOperationsReaderPort` | draw adapter    | Yes                |
| `admin_alerts`          | `AdminAlertsProvider`         | `features.notification` / ops | `ListAdminDashboardAlertsQuery`      | notification/ops reader    | feature adapter | Yes, empty allowed |

## Post-MVP superadmin sources

| Source                     | Provider                         | Owner domain                | Query                              | MVP |
| -------------------------- | -------------------------------- | --------------------------- | ---------------------------------- | --- |
| `superadmin_system_health` | `SuperAdminSystemHealthProvider` | `features.ops`              | `GetPlatformSystemHealthQuery`     | No  |
| `superadmin_batch_status`  | `SuperAdminBatchStatusProvider`  | `features.ops/common.batch` | `GetPlatformBatchStatusQuery`      | No  |
| `superadmin_tenants`       | `SuperAdminTenantsProvider`      | `catalog.tenant/platform`   | `ListPlatformTenantSummariesQuery` | No  |
| `superadmin_version`       | `SuperAdminVersionProvider`      | `features.ops`              | `GetPlatformVersionSummaryQuery`   | No  |
