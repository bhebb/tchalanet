# PageModel Dynamic Source Registry

## Rules

- `binding.source` MUST be snake_case.
- `providerKey()` SHOULD equal `binding.source`.
- One source means one provider responsibility.
- Providers must call QueryBus/stable application services, never repositories/JPA/entities.
- JSON fragments must be loaded only through `json_file` + `file_key` registry.

## JSON fragment sources

| file_key                           | Classpath resource                                                | Purpose                         |
| ---------------------------------- | ----------------------------------------------------------------- | ------------------------------- |
| `public_header_links`              | `pagemodel/fragments/public/header.links.json`                    | Public header navigation links  |
| `public_footer_links`              | `pagemodel/fragments/public/footer.links.json`                    | Public footer links             |
| `public_main_menu_links`           | `pagemodel/fragments/public/main_menu.links.json`                 | Public main menu                |
| `public_legal_links`               | `pagemodel/fragments/public/legal.links.json`                     | Public legal links              |
| `public_support_links`             | `pagemodel/fragments/public/support.links.json`                   | Public support/help links       |
| `private_footer_links`             | `pagemodel/fragments/private/common/footer.links.json`            | Shared private footer links     |
| `private_profile_menu_links`       | `pagemodel/fragments/private/common/profile_menu.links.json`      | Shared private profile menu     |
| `private_header_cashier`           | `pagemodel/fragments/private/cashier/header.links.json`           | Cashier header links/actions    |
| `private_sidebar_cashier`          | `pagemodel/fragments/private/cashier/sidebar.links.json`          | Cashier sidebar links           |
| `private_cashier_quick_actions`    | `pagemodel/fragments/private/cashier/quick_actions.links.json`    | Cashier quick actions           |
| `private_header_admin`             | `pagemodel/fragments/private/admin/header.links.json`             | Admin header links/actions      |
| `private_sidebar_admin`            | `pagemodel/fragments/private/admin/sidebar.links.json`            | Admin sidebar links             |
| `private_admin_management_menu`    | `pagemodel/fragments/private/admin/management_menu.links.json`    | Admin management menu           |
| `private_header_superadmin`        | `pagemodel/fragments/private/superadmin/header.links.json`        | Superadmin header links/actions |
| `private_sidebar_superadmin`       | `pagemodel/fragments/private/superadmin/sidebar.links.json`       | Superadmin sidebar links        |
| `private_superadmin_platform_menu` | `pagemodel/fragments/private/superadmin/platform_menu.links.json` | Platform operations menu        |

## Dynamic data sources

| Source                     | Provider                         | Expected query/service                        | Props                                                                                |
| -------------------------- | -------------------------------- | --------------------------------------------- | ------------------------------------------------------------------------------------ |
| `json_file`                | `JsonFileProvider`               | `PageModelJsonFragmentRegistry` + `JsonUtils` | `file_key`                                                                           |
| `public_news`              | `PublicNewsProvider`             | `PublicNewsService`                           | `max_items`                                                                          |
| `public_draw_results`      | `PublicDrawResultsProvider`      | `ListPublicDrawResultsBySlotQuery`            | `include_history`, `history_limit`, `include_next_by_slot`, `include_latest_by_slot` |
| `public_tchala`            | `PublicTchalaProvider`           | `ListPublicTchalaEntriesQuery`                | `max_items`                                                                          |
| `public_plans`             | `PublicPlansProvider`            | `ListPublicPlansQuery`                        | none                                                                                 |
| `cashier_overview`         | `CashierOverviewProvider`        | `GetCashierDashboardOverviewQuery`            | none                                                                                 |
| `cashier_quick_sale`       | `CashierQuickSaleProvider`       | `GetCashierQuickSaleOptionsQuery`             | `max_games`, `include_next_draws`                                                    |
| `cashier_recent_tickets`   | `CashierRecentTicketsProvider`   | `ListRecentTicketsQuery`                      | `max_items`                                                                          |
| `cashier_session`          | `CashierSessionProvider`         | `GetActiveCashierSessionQuery`                | none                                                                                 |
| `cashier_next_draws`       | `CashierNextDrawsProvider`       | `ListNextDrawsQuery`                          | `max_items`, `include_countdown`                                                     |
| `cashier_limits`           | `CashierLimitsProvider`          | `GetCashierLimitsSnapshotQuery`               | none                                                                                 |
| `admin_kpis`               | `AdminKpisProvider`              | `GetTenantDashboardKpisQuery`                 | `period`                                                                             |
| `admin_draw_operations`    | `AdminDrawOperationsProvider`    | `ListTenantDrawOperationsSummaryQuery`        | `max_items`                                                                          |
| `admin_approval_queue`     | `AdminApprovalQueueProvider`     | `ListPendingSaleApprovalsQuery`               | `max_items`                                                                          |
| `admin_alerts`             | `AdminAlertsProvider`            | `ListTenantAlertsQuery`                       | `max_items`, `severity`                                                              |
| `admin_agents`             | `AdminAgentsProvider`            | `ListTenantAgentsSummaryQuery`                | `max_items`                                                                          |
| `admin_outlets`            | `AdminOutletsProvider`           | `ListTenantOutletsSummaryQuery`               | `max_items`                                                                          |
| `superadmin_system_health` | `SuperAdminSystemHealthProvider` | Ops health service                            | none                                                                                 |
| `superadmin_batch_status`  | `SuperAdminBatchStatusProvider`  | `ListBatchGateStatusQuery`                    | `max_items`                                                                          |
| `superadmin_tenants`       | `SuperAdminTenantsProvider`      | `ListPlatformTenantsSummaryQuery`             | `max_items`                                                                          |
| `superadmin_version`       | `SuperAdminVersionProvider`      | Build/version info service                    | none                                                                                 |
