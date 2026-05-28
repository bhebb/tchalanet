# Design — dashboard-overview-runtime-v1

## 1. Vocabulary

| Term | Meaning |
|---|---|
| PageModelTemplate | Reference document in catalog, versioned, structure of page. |
| PageModel | Tenant instance materialized from template; draft/publish/merge lifecycle; `model` JSON is structure-only. |
| Dashboard | Post-login landing page, PageModel-based, for a role/surface. |
| Overview / Aperçu | Feature endpoint for structural diagnosis and navigation. |
| Provider source | Dynamic source consumed by PageModel widgets, e.g. `tenant_admin_dashboard`. |
| Payload assembler | Feature/app service that loads the grouped payload for one source. |
| Static fragment | JSON loaded by `json_file`, usually header/sidenav/footer/actions. |
| Management page | Detailed list/action page owned by core/catalog/platform. |
| Widget registry | Allowed `widgetId` list by source and `schema_version`. |
| Readiness | Structural completeness diagnosis, computed once and projected differently by surface. |

## 2. Template -> instance system

```text
catalog.pagemodeltemplate
  STRUCTURE of reference, versioned, slots and bindings

        | onboarding / merge / upsert

core.pagemodel
  Tenant instance, DRAFT -> PUBLISHED,
  customizable, structure-only

        | ResolvePageModelQuery / FindPublishedPageModelQuery

PageModelView
  shell + layout + widget slots

        | dynamic source resolution

features.* providers
  fill slots with fresh data
  grouped read, memoized per request
```

Invariant:

```text
PageModel instance never stores volatile data.
Dynamic data is resolved on every read from owning domains/catalog/platform.
```

Regeneration means re-materializing structure from template. Runtime data is not regenerated.

## 3. Architectural placement

Recommended shape:

```text
core.pagemodel
  api/
    query/
      ResolvePageModelQuery
      FindPublishedPageModelQuery
    model/
      PageModelView
      PageModelType
      WidgetData
      WidgetDynamicError
    dynamic/
      DynamicWidgetProvider
      PageModelResolutionContext
  internal/
    application/
      ResolvePageModelHandler
      DynamicWidgetProviderRegistry
    domain/
      PageModelDocument
    infra/web/
      PageModelController

features.pagemodel
  onboarding/
    PageModelOnboardingService
    PageModelOnboardingRunner
  dynamic/providers/
    JsonFileDynamicProvider
    PublicHomeProvider
    PublicDrawResultsProvider

features.tenantadmin
  dashboard/
    TenantAdminDashboardProvider
    TenantAdminDashboardPayloadAssembler
  overview/
    TenantAdminOverviewController
    TenantAdminOverviewService
  readiness/
    GetTenantReadinessQueryHandler

features.cashier
  home/
    CashierHomeController
    CashierHomeService
  dashboard/
    CashierWebDashboardProvider
    CashierDashboardPayloadAssembler

features.platformadmin
  dashboard/
    PlatformAdminDashboardProvider
    PlatformAdminDashboardPayloadAssembler
  overview/
    PlatformAdminOverviewController
    PlatformAdminOverviewService
  tenantonboarding/
    TenantProvisioningController
    TenantProvisioningOrchestrator
```

## 4. Dependency guard

`core.pagemodel.api.dynamic.DynamicWidgetProvider` may be implemented by `features.*`.

This is acceptable only with the following hard rules:

```text
core.pagemodel does not import features.*
features may import core.pagemodel.api.*
features must not import core.pagemodel.internal.*
Spring/app assembly wires provider implementations at runtime.
```

## 5. Provider grouping

A dashboard may expose many widgets, but a provider source loads one grouped payload once per request.

Example:

```text
source tenant_admin_dashboard
  -> TenantAdminDashboardPayloadAssembler.assemble(ctx)
  -> <= 5 grouped reads
  -> switch(widgetId) extracts slice
```

### Failure-aware memoization

Use memoization that caches both success and failure, so one failing grouped read is not retried for every widget.

```java
public final class PageModelResolutionContext {

  private final Map<String, Object> memo = new ConcurrentHashMap<>();

  public <T> T getOrLoad(String key, Supplier<T> loader) {
    Object cached = memo.computeIfAbsent(key, ignored -> {
      try {
        return new Loaded<>(loader.get());
      } catch (RuntimeException e) {
        return new Failed(e);
      }
    });

    if (cached instanceof Failed failed) {
      throw failed.cause();
    }

    @SuppressWarnings("unchecked")
    T value = ((Loaded<T>) cached).value();
    return value;
  }

  private record Loaded<T>(T value) {}
  private record Failed(RuntimeException cause) {}
}
```

Key is normally `source`. If one source needs multiple payload variants later, key becomes `source + ":" + payloadKey`.

## 6. Widget registry

The template declares `widgetId` strings in JSON; provider code switches on the same strings.

A registry must declare, for each `schema_version` and source:

- legal `widgetId`;
- owning provider source;
- expected widget type if useful.

Consequences:

- template re-seed validates widget ids;
- runtime returns `dynamic.error` for unknown ids;
- adding a widget requires registry update;
- typos never result in silent empty widgets.

## 7. Static fragments

Static fragments are loaded by `json_file`.

Allowed:

- public header;
- public footer;
- private sidenav;
- private footer;
- profile menu;
- static quick actions.

Forbidden:

- dashboard KPI;
- readiness;
- tenant/user-specific volatile state;
- live counts;
- backend API endpoints.

The `json_file` provider validates `file_key` against an allowlist.

## 8. Readiness

Readiness is structural, computed on read, not persisted.

One logical calculation serves three projections:

```text
GetTenantReadinessQuery
  -> effective tenant comes from context/RLS
  -> handler aggregates read-only core/catalog/platform data

TenantReadinessSummary
  -> dashboard short version

TenantReadinessView
  -> tenant overview detailed version
  -> provisioning result / next steps
```

Dashboard receives summary only. Overview receives sections/issues/routes. Provisioning result receives readiness/next steps.

Readiness must not include dashboard KPI:

- no salesToday;
- no ticketCountToday;
- no activeSessions as real-time KPI;
- no openDraws as dashboard KPI.

## 9. Runtime flows

```text
Public home
  GET /public/page-models/home
    -> public.home
    -> json_file header/footer
    -> PublicHomeProvider source=public_home
    -> PublicDrawResultsProvider source=public_draw_results when used

Public draw results
  GET /public/page-models/results
    -> public.draw_results
    -> json_file header/footer
    -> PublicDrawResultsProvider source=public_draw_results

Tenant admin dashboard
  GET /page-models/dashboard?surface=WEB_ADMIN
    -> private.dashboard.admin
    -> json_file shell
    -> TenantAdminDashboardProvider source=tenant_admin_dashboard

Tenant overview
  GET /admin/overview
    -> features.tenantadmin.overview
    -> no PageModel provider

Cashier POS/mobile
  GET /tenant/cashier/home
    -> compact endpoint
    -> no full PageModel

Cashier web dashboard
  GET /page-models/dashboard?surface=CASHIER_WEB
    -> private.dashboard.cashier.web
    -> CashierWebDashboardProvider source=cashier_dashboard

Platform dashboard
  GET /page-models/dashboard?surface=WEB_PLATFORM_ADMIN
    -> private.dashboard.platform
    -> PlatformAdminDashboardProvider source=platform_admin_dashboard

Platform overview
  GET /platform/overview
    -> features.platformadmin.overview
    -> no PageModel provider
```

## 10. Error handling

Provider errors are surfaced:

```json
{
  "widgetId": "dashboard.admin.sales",
  "providerKey": "tenant_admin_dashboard",
  "code": "TENANT_ADMIN_DASHBOARD_LOAD_FAILED",
  "message": "Unable to load tenant admin dashboard payload"
}
```

No silent fallback to empty payload. Shell and other widgets may still render.

## 11. Ownership

| Page / data | Owner |
|---|---|
| Public home composition | `features.pagemodel` providers |
| Public latest results | draw/result owner via public query/catalog |
| Public check-ticket action | sales/ticket verification owner |
| Tenant admin dashboard | PageModel provider + feature assembler |
| Tenant overview | `features.tenantadmin.overview` |
| Tenant readiness | shared readiness handler/query |
| Outlets management | `core.outlet` |
| Terminals management | `core.terminal` |
| Sessions management | `core.session` |
| Sales/tickets management | `core.sales` |
| Draws/results management | `core.draw` / draw result owner |
| Limits management | `core.limitpolicy` |
| Promotions management | `core.promotion` |
| Settings management | `catalog.settings` / `platform.tenantconfig` |
| I18n management | `catalog.i18n` |
| Appearance/theme management | `catalog.theme` / `platform.tenanttheme` |
| Platform dashboard | PageModel provider + feature assembler |
| Platform overview | `features.platformadmin.overview` |
| Platform tenants | platform tenant owner |
| Platform ops | platform ops/features |

## 12. Performance targets

| Surface | Grouped reads target |
|---|---:|
| Public home | <= 4 |
| Public draw results | <= 2 |
| Tenant admin dashboard | <= 5 |
| Cashier web dashboard | <= 4 |
| Platform dashboard | <= 4 |
| Tenant overview | <= 6 |
| Platform overview | <= 5 |

Tests should count grouped `ask()` / reader calls by surface where practical.

## 13. Tenant provisioning framing

Provisioning is not a dashboard feature.

Definitions:

```text
Provisioning creates initial tenant state.
Overview verifies completeness.
Management pages correct and evolve data.
```

Provisioning V1 profiles:

- `MINIMAL`
- `DEFAULT_HAITI_LOTTERY`
- `DEMO`

Out of V1:

- `CUSTOM_FROM_TENANT`

Provisioning calls owning domains and never copies transactional data.

It returns readiness/next steps.
