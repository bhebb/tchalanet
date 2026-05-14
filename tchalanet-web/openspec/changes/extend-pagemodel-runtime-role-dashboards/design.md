# Design: PageModel Runtime and Role Dashboards

## 1. Current architecture

The current runtime architecture is already close to the target:

```text
TenantPageModelController / PlatformPageModelController
  -> PageModelTypeResolver
  -> DashboardPageModelService
  -> ResolveEffectivePageModelQuery
  -> PageModelDynamicResolver
  -> PageModelDynamicProvider[]
  -> DashboardPageModelResponse
```

This change keeps that architecture and completes the runtime behavior.

## 2. Runtime response model

The base PageModel document remains separate from dynamic payloads.

Expected response shape:

```json
{
  "currentLang": "fr",
  "langs": ["fr", "en", "ht"],
  "doc": {
    "meta": {},
    "theme": {},
    "shell": {},
    "content": {
      "layout": {},
      "widgets": {}
    }
  },
  "dynamic": {
    "widgets": {
      "home.hero": {},
      "dashboard.cashier.overview": {}
    },
    "errors": []
  },
  "notifications": {}
}
```

Rules:

- Template JSON defines shell, layout, widget config, static props, and `binding.source`.
- Providers return dynamic payloads for `dynamic.widgets[widgetId]`.
- Static widgets are not passed to providers.
- Dynamic widget failures do not discard the full response.
- `dynamic.errors` reports widget-level provider failures.

## 3. Provider source naming

`binding.source` must use functional namespaces.

### Public

```text
public.hero
public.draws.today
public.features
public.news.latest
public.tchala.featured
public.testimonials
public.plans
```

### Cashier

```text
cashier.overview
cashier.quick_sale
cashier.open_draws
cashier.recent_tickets
cashier.session
```

### Tenant admin

```text
tenant_admin.overview
tenant_admin.sales_summary
tenant_admin.draws_today
tenant_admin.outlets
tenant_admin.users
tenant_admin.limits
tenant_admin.alerts
```

### Platform

```text
platform.overview
platform.tenants
platform.services.health
platform.flags
platform.jobs
platform.audit
platform.release_notes
```

## 4. Dashboard resolution

Dashboard resolution must use `TchRequestContext`, not `TchRole` alone.

### Rules

```text
ApiScope.PLATFORM + SUPER_ADMIN
  -> platform.dashboard.super_admin

ApiScope.TENANT + SUPER_ADMIN + tenantOverridden=true
  -> private.dashboard.tenant_admin

ApiScope.TENANT + TENANT_ADMIN
  -> private.dashboard.tenant_admin

ApiScope.TENANT + CASHIER
  -> private.dashboard.cashier
```

Future extension:

```text
ApiScope.TENANT + OPERATOR
  -> private.dashboard.operator
```

`OPERATOR` is not mandatory for this MVP unless the template and providers are added in the same implementation pass.

## 5. SUPER_ADMIN modes

### 5.1 Platform mode

```text
scope = PLATFORM
dashboard = platform.dashboard.super_admin
```

Use cases:

- tenants overview;
- platform service health;
- feature flags;
- batch/jobs ops;
- platform audit;
- release notes.

### 5.2 Tenant administration mode

```text
scope = TENANT
tenantOverridden = true
dashboard = private.dashboard.tenant_admin
```

The SUPER_ADMIN sees the same dashboard as TENANT_ADMIN for the selected tenant.

The runtime context should preserve:

```text
actorRole = SUPER_ADMIN
effective dashboard role = TENANT_ADMIN
effectiveTenant = overridden tenant
tenantOverridden = true
```

The frontend should display a tenant override banner when the runtime response/context indicates tenant override.

## 6. Controllers

### 6.1 Tenant runtime

```http
GET /tenant/pagemodel/dashboard
```

Rules:

- authenticated user required;
- use `contextResolver.currentOrThrow()`;
- resolve dashboard via `PageModelTypeResolver.forDashboard(ctx)`;
- call `DashboardPageModelService.resolveTenantDashboard(...)`;
- do not accept tenant id from request body or ordinary query parameter;
- SUPER_ADMIN with tenant override uses this endpoint and receives `private.dashboard.tenant_admin`.

### 6.2 Platform runtime

```http
GET /platform/pagemodel/dashboard
```

Rules:

- SUPER_ADMIN required;
- use `contextResolver.currentOrThrow()`;
- resolve dashboard via `PageModelTypeResolver.forDashboard(ctx)`;
- call `DashboardPageModelService.resolvePlatformDashboard(...)`;
- returns `platform.dashboard.super_admin`.

### 6.3 Logical id endpoints

Free `/{logicalId}` runtime endpoints are not the main path for tenant users.

Preferred future shape:

```http
GET /admin/pagemodel/{logicalId}/preview
GET /platform/pagemodel/{logicalId}/preview
```

Preview endpoints can be added later. For this MVP, existing logical-id endpoints should be reviewed and restricted if needed.

## 7. Service split

Replace ambiguous runtime calls like:

```java
resolve(String logicalId, Optional<TenantId> tenantIdOverride, Optional<String> langFromUrl)
```

with explicit runtime methods:

```java
DashboardPageModelResponse resolveTenantDashboard(
    String logicalId,
    Optional<String> langFromUrl
)

DashboardPageModelResponse resolvePlatformDashboard(
    String logicalId,
    Optional<String> langFromUrl
)
```

An internal method can share the implementation:

```java
DashboardPageModelResponse resolveInternal(
    String logicalId,
    Optional<TenantId> tenantId,
    Optional<String> langFromUrl,
    TchRequestContext ctx
)
```

Tenant runtime uses `ctx.tenantId()`.

Platform runtime uses `Optional.empty()` for tenant unless a future preview explicitly requires tenant-aware behavior.

## 8. Dynamic resolver behavior

`PageModelDynamicResolver` must:

- ignore null docs;
- ignore pages with no widgets;
- ignore static widgets;
- match provider by `logicalId`, `widgetType`, and `binding.source`;
- pick provider deterministically if multiple providers match;
- isolate failures per widget;
- log internal provider exceptions server-side;
- return sanitized `WidgetDynamicError` to the frontend.

Provider matching should be deterministic:

```java
providers.stream()
  .filter(p -> p.supports(logicalId, widgetType, source))
  .min(Comparator.comparingInt(PageModelDynamicProvider::order))
```

Provider contract should expose:

```java
String providerKey();
boolean supports(String logicalId, String widgetType, String source);
Object load(PageModelDoc doc, String widgetId, WidgetConfig config, String lang, TchRequestContext ctx);
default int order() { return 100; }
```

If the existing `WidgetConfig` type is named differently, keep the existing project type.

## 9. Provider package layout

Recommended layout:

```text
features/pagemodel/dynamic/providers/public/
features/pagemodel/dynamic/providers/cashier/
features/pagemodel/dynamic/providers/tenantadmin/
features/pagemodel/dynamic/providers/platform/
```

If the current package is flat, implementation can keep it flat for minimal diff. If providers are split, apply the feature package rule pragmatically and avoid excessive micro-packages unless each group has enough classes.

## 10. Provider list

### 10.1 Public providers

```text
PublicHeroProvider
PublicDrawsProvider
PublicFeaturesProvider
PublicNewsProvider
PublicTchalaProvider
PublicTestimonialsProvider
PublicPlansProvider
```

Existing providers may be renamed/adapted:

```text
HeroProvider -> PublicHeroProvider
DrawsProvider -> PublicDrawsProvider
PlansProvider -> PublicPlansProvider
PublicNewsProvider -> keep or move under public package
```

### 10.2 Cashier providers

```text
CashierOverviewProvider
CashierQuickSaleProvider
CashierOpenDrawsProvider
CashierRecentTicketsProvider
CashierSessionProvider
```

### 10.3 Tenant admin providers

```text
TenantAdminOverviewProvider
TenantAdminSalesProvider
TenantAdminDrawsProvider
TenantAdminOutletsProvider
TenantAdminUsersProvider
TenantAdminLimitsProvider
TenantAdminAlertsProvider
```

### 10.4 Platform providers

```text
PlatformOverviewProvider
PlatformTenantsProvider
PlatformServicesHealthProvider
PlatformFlagsProvider
PlatformJobsProvider
PlatformAuditProvider
PlatformReleaseNotesProvider
```

## 11. Templates

Required templates:

```text
public.home
private.dashboard.cashier
private.dashboard.tenant_admin
platform.dashboard.super_admin
```

Optional later:

```text
private.dashboard.operator
```

Templates must remain declarative:

- layout;
- shell;
- static props;
- binding metadata;
- role/feature visibility metadata if supported.

Templates must not contain computed business data.

## 12. Error and partial behavior

MVP behavior:

- `dynamic.errors` contains provider errors;
- PageModel response still returns success if the base PageModel resolves;
- frontend renders widget-level fallback when a widget payload is missing or errored.

Future behavior:

- map dynamic errors to `ApiResponse` notices;
- return `PARTIAL` when non-critical providers fail.

## 13. Security and context

Rules:

- Tenant dashboard must use `TchRequestContext`, not a client-provided tenant ID.
- SUPER_ADMIN tenant override must come from canonical context/filter behavior.
- Providers must not enforce business permissions by themselves.
- Providers may hide optional UI data based on context/features, but must not make business decisions.
- Feature providers must not access repositories or JPA entities.

## 14. Cache

Cache is optional for MVP.

If cache is added:

- use functional cache names;
- declare TTL through `CacheSpecProvider`;
- do not cache money-critical computations;
- do not treat cache as source of truth.

Potential cache names:

```text
public.pagemodel.home
public.draw.latest
tenant.dashboard.runtime
platform.dashboard.runtime
```

Final cache names should follow existing project conventions.
