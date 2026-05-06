# Design — PageModel Dynamic Providers + JSON Fragment Provider

## Existing mechanism to keep

Current contract:

```java
public interface PageModelDynamicProvider {
  boolean supports(String logicalId, String widgetType, String source);

  Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx
  );

  String providerKey();
}
```

Current resolver behavior:

```text
PageModelDoc.content.widgets[*].binding.mode == dynamic
  -> resolver reads binding.source
  -> finds PageModelDynamicProvider.supports(logicalId, widgetType, source)
  -> provider.load(...)
  -> PageDynamicPayload.widgets[widgetId] = payload
```

Keep this mechanism.

## Naming rules

Use snake_case provider sources.

Do this:

```text
json_file
public_news
public_draw_results
cashier_overview
cashier_quick_sale
cashier_recent_tickets
cashier_session
admin_kpis
superadmin_system_health
```

Avoid this:

```text
news
public-news
cashier.overview
quick_sale mixed with cashier.overview
session
```

Reason: `binding.source` becomes an API-like key. It should be stable, unique, and predictable.

## JSON fragment decomposition

### Target resource folder

```text
src/main/resources/pagemodel/fragments/
  public/
    header.links.json
    footer.links.json
    main_menu.links.json
    legal.links.json
    support.links.json

  private/
    common/
      footer.links.json
      profile_menu.links.json
    cashier/
      sidebar.links.json
      header.links.json
      quick_actions.links.json
    admin/
      sidebar.links.json
      header.links.json
      management_menu.links.json
    superadmin/
      sidebar.links.json
      header.links.json
      platform_menu.links.json
```

### Registry keys

Do not allow raw paths. Use logical keys:

```text
public_header_links
public_footer_links
public_main_menu_links
public_legal_links
public_support_links

private_footer_links
private_profile_menu_links
private_header_cashier
private_sidebar_cashier
private_cashier_quick_actions
private_header_admin
private_sidebar_admin
private_admin_management_menu
private_header_superadmin
private_sidebar_superadmin
private_superadmin_platform_menu
```

## Generic JSON provider

### Class

```text
com.tchalanet.server.features.pagemodel.dynamic.providers.json.JsonFileProvider
```

### Registry

```text
com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry
```

### Source

```text
json_file
```

### Props

```json
{
  "file_key": "private_sidebar_cashier"
}
```

Optional future props:

```json
{
  "file_key": "public_footer_links",
  "localized": false
}
```

For now prefer `label_key` inside fragments and keep translations in frontend i18n files.

## JsonUtils requirement

The provider must use the existing non-deprecated `JsonUtils` class in `common`.

Preferred patterns depending on the actual `JsonUtils` API available in the repo:

```java
Object payload = JsonUtils.fromJson(inputStream, Object.class);
```

or:

```java
String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
Object payload = JsonUtils.fromJson(json, Object.class);
```

or if `JsonUtils` exposes `JsonNode` helpers:

```java
JsonNode node = JsonUtils.readTree(json);
Object payload = JsonUtils.convertValue(node, Object.class);
```

Use the non-deprecated method names that exist in the current `common` package. Do not introduce a second JSON utility.

## Provider source registry

| Source                     | Provider class                   | Query/service source                                     | Scope          | Critical          |
| -------------------------- | -------------------------------- | -------------------------------------------------------- | -------------- | ----------------- |
| `json_file`                | `JsonFileProvider`               | classpath JSON fragment via registry + JsonUtils         | public/private | depends on widget |
| `public_news`              | `PublicNewsProvider`             | `PublicNewsService`                                      | public         | no                |
| `public_draw_results`      | `PublicDrawResultsProvider`      | `ListPublicDrawResultsBySlotQuery`                       | public         | yes               |
| `public_tchala`            | `PublicTchalaProvider`           | future `ListPublicTchalaEntriesQuery` or catalog service | public         | no                |
| `public_plans`             | `PublicPlansProvider`            | `ListPublicPlansQuery` or catalog plan service           | public         | no                |
| `cashier_overview`         | `CashierOverviewProvider`        | `GetCashierDashboardOverviewQuery`                       | private tenant | yes               |
| `cashier_quick_sale`       | `CashierQuickSaleProvider`       | `GetCashierQuickSaleOptionsQuery`                        | private tenant | yes               |
| `cashier_recent_tickets`   | `CashierRecentTicketsProvider`   | `ListRecentTicketsQuery`                                 | private tenant | no                |
| `cashier_session`          | `CashierSessionProvider`         | `GetActiveCashierSessionQuery`                           | private tenant | yes               |
| `cashier_next_draws`       | `CashierNextDrawsProvider`       | `ListNextDrawsQuery`                                     | private tenant | yes               |
| `cashier_limits`           | `CashierLimitsProvider`          | `GetCashierLimitsSnapshotQuery`                          | private tenant | yes               |
| `admin_kpis`               | `AdminKpisProvider`              | `GetTenantDashboardKpisQuery`                            | tenant admin   | yes               |
| `admin_draw_operations`    | `AdminDrawOperationsProvider`    | `ListTenantDrawOperationsSummaryQuery`                   | tenant admin   | yes               |
| `admin_approval_queue`     | `AdminApprovalQueueProvider`     | `ListPendingSaleApprovalsQuery`                          | tenant admin   | yes               |
| `admin_alerts`             | `AdminAlertsProvider`            | `ListTenantAlertsQuery`                                  | tenant admin   | no                |
| `admin_agents`             | `AdminAgentsProvider`            | `ListTenantAgentsSummaryQuery`                           | tenant admin   | no                |
| `admin_outlets`            | `AdminOutletsProvider`           | `ListTenantOutletsSummaryQuery`                          | tenant admin   | no                |
| `superadmin_system_health` | `SuperAdminSystemHealthProvider` | ops/actuator health service                              | platform       | yes               |
| `superadmin_batch_status`  | `SuperAdminBatchStatusProvider`  | `ListBatchGateStatusQuery` / batch ops                   | platform       | yes               |
| `superadmin_tenants`       | `SuperAdminTenantsProvider`      | `ListPlatformTenantsSummaryQuery`                        | platform       | yes               |
| `superadmin_version`       | `SuperAdminVersionProvider`      | build/version info service                               | platform       | no                |

## Query naming guidance

Queries should live in the domain that owns the data, not in `features.pagemodel`.

Examples:

```text
core.draw.application.query.model.ListNextDrawsQuery
core.drawresult.application.query.model.ListPublicDrawResultsBySlotQuery
core.sales.application.query.model.ListRecentTicketsQuery
core.sales.application.query.model.GetCashierDashboardOverviewQuery
core.sales.application.query.model.GetCashierQuickSaleOptionsQuery
core.session.application.query.model.GetActiveCashierSessionQuery
core.audit.application.query.model.ListAuditSummaryQuery
features.ops.application.query.model.ListBatchGateStatusQuery
catalog.tenant.application.query.model.ListPlatformTenantsSummaryQuery
```

`features.pagemodel` providers call these through `QueryBus` or stable services. They do not own the rules.

## Error behavior

`PageModelDynamicResolver` already supports partial errors.

Keep:

```text
WidgetDynamicError(widgetId, providerKey, code, safeMessage)
```

Add/standardize error codes:

```text
NO_PROVIDER
PROVIDER_ERROR
JSON_FRAGMENT_NOT_FOUND
JSON_FRAGMENT_INVALID
MISSING_PROP
```

For critical widgets, the frontend can show an error placeholder. For non-critical widgets, it can hide the widget.

## Small resolver hardening

Change:

```java
Map<String, Object> widgets = new HashMap<>();
```

To:

```java
Map<String, Object> widgets = new LinkedHashMap<>();
```

This gives stable output order for debugging and snapshot tests.
