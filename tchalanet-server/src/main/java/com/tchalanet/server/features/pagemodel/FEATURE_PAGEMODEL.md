# Feature pagemodel

`features.pagemodel` is the BFF/composition layer for public and private PageModel
responses. It resolves the effective PageModel through core queries, then enriches
dynamic widgets and shell sections through `PageModelDynamicProvider`.

## Dynamic source rules

- `binding.source` values are stable `snake_case` identifiers.
- `providerKey()` must equal the `binding.source` handled by the provider.
- Providers compose payloads through `QueryBus` or stable application services.
- Providers must not access repositories, JPA entities, SQL, or business tables directly.
- Tenant and actor data comes from `TchRequestContext`; clients must not provide tenant data
  inside dynamic widget props.

## JSON fragments

Reusable navigation and shell payloads use the generic `json_file` source.
Templates provide `props.file_key`; the provider resolves that key through
`PageModelJsonFragmentRegistry`.

Raw paths are forbidden. Unknown keys, including path traversal strings, are rejected before
any classpath resource is loaded.

Fragments live under:

```text
src/main/resources/pagemodel/fragments/
```

Fragments use `label_key` only. Do not put translated labels in fragment JSON.

## Current fragment registry

| `file_key`                      | Resource                                                       |
| ------------------------------- | -------------------------------------------------------------- |
| `public_header_links`           | `pagemodel/fragments/public/header.links.json`                 |
| `public_footer_links`           | `pagemodel/fragments/public/footer.links.json`                 |
| `private_footer_links`          | `pagemodel/fragments/private/footer.links.json`                |
| `private_header_cashier`        | `pagemodel/fragments/private/cashier/header.links.json`        |
| `private_sidebar_cashier`       | `pagemodel/fragments/private/cashier/sidebar.links.json`       |
| `private_cashier_quick_actions` | `pagemodel/fragments/private/cashier/quick_actions.links.json` |

## Current dynamic sources

| Source                     | Provider                         |
| -------------------------- | -------------------------------- |
| `json_file`                | `JsonFileProvider`               |
| `public_news`              | `PublicNewsProvider`             |
| `public_draw_results`      | `PublicDrawResultsProvider`      |
| `public_features`          | `PublicFeaturesProvider`         |
| `public_tchala`            | `PublicTchalaProvider`           |
| `public_testimonials`      | `PublicTestimonialsProvider`     |
| `public_plans`             | `PlansProvider`                  |
| `cashier_overview`         | `CashierOverviewProvider`        |
| `cashier_quick_sale`       | `CashierQuickSaleProvider`       |
| `cashier_recent_tickets`   | `CashierRecentTicketsProvider`   |
| `cashier_session`          | `CashierSessionProvider`         |
| `cashier_next_draws`       | `CashierNextDrawsProvider`       |
| `cashier_limits`           | `CashierLimitsProvider`          |
| `admin_kpis`               | `AdminKpisProvider`              |
| `admin_draw_operations`    | `AdminDrawOperationsProvider`    |
| `admin_approval_queue`     | `AdminApprovalQueueProvider`     |
| `admin_alerts`             | `AdminAlertsProvider`            |
| `superadmin_system_health` | `SuperAdminSystemHealthProvider` |
| `superadmin_batch_status`  | `SuperAdminBatchStatusProvider`  |
| `superadmin_tenants`       | `SuperAdminTenantsProvider`      |
| `superadmin_version`       | `SuperAdminVersionProvider`      |
