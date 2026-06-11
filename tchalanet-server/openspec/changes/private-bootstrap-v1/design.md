# Design — private-bootstrap-v1

## Module structure

```
features/runtime/
  web/
    RuntimeBootstrapController.java
  app/
    RuntimeBootstrapService.java
    PageModelRefResolver.java        ← pure mapping space → (route, endpoint)
    ReadinessFacade.java             ← context-aware; cashier → PARTIAL (V1)
  model/
    RuntimeBootstrapResponse.java
    PrivateBootstrapSpace.java
    AuthenticatedUserView.java
    TenantContextView.java
    RuntimeSettingsView.java
    RuntimeThemeView.java
    RuntimeI18nBundle.java
    EntitlementsView.java
    RuntimeReadinessView.java
    RuntimeReadinessCheck.java
    NotificationSummaryView.java
    NotificationPreview.java
    PageModelRef.java
    RuntimeBootstrapNotice.java
  mapper/
    RuntimeBootstrapMapper.java
```

Rule of 3: all four packages (`web/`, `app/`, `model/`, `mapper/`) have ≥ 3 elements — valid.

No `NavigationFactory` — navigation lives in PageModel, not in bootstrap.

## Controller

```java
@RestController
@RequestMapping("/runtime/bootstrap")
@RequiredArgsConstructor
class RuntimeBootstrapController {
    private final RuntimeBootstrapService service;

    @GetMapping
    ApiResponse<RuntimeBootstrapResponse> bootstrap(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.ok(service.bootstrap(ctx));
    }
}
```

One endpoint. No space parameter — space is resolved from the authenticated context.

## Service dispatch

```java
RuntimeBootstrapResponse bootstrap(TchRequestContext ctx) {
    PrivateBootstrapSpace space = resolveSpace(ctx);   // from JWT roles
    assertSpaceAccessible(ctx, space);

    AuthenticatedUserView user        = identityFacade.currentUser(ctx);
    TenantContextView tenantCtx       = resolveTenantContext(ctx, space); // null if PLATFORM
    EntitlementsView entitlements     = accessFacade.entitlements(ctx, space);
    RuntimeSettingsView settings      = assembleSettings(ctx, space);     // tenantconfig + catalog.settings
    RuntimeThemeView theme            = themeFacade.runtimeTheme(ctx, space);
    RuntimeI18nBundle i18n            = i18nFacade.privateBundle(ctx, settings.locale());
    RuntimeReadinessView readiness    = readinessFacade.readiness(ctx, space);
    NotificationSummaryView notifs    = notificationFacade.summary(ctx);
    PageModelRef pageModelRef         = pageModelRefResolver.resolve(space);

    return mapper.toResponse(space, user, tenantCtx, settings, theme, i18n,
                             entitlements, readiness, notifs, pageModelRef, notices);
}
```

Space resolution from roles:

| Role(s) in JWT       | Resolved space |
|----------------------|----------------|
| `SUPER_ADMIN`        | PLATFORM       |
| `TENANT_ADMIN`       | ADMIN          |
| `CASHIER`, `OPERATOR`| CASHIER        |
| Other / none         | → 403          |

If a user holds multiple roles, priority: PLATFORM > ADMIN > CASHIER.

## Response contract

```java
record RuntimeBootstrapResponse(
    PrivateBootstrapSpace space,
    AuthenticatedUserView user,
    @Nullable TenantContextView tenantContext,
    RuntimeSettingsView settings,
    RuntimeThemeView theme,
    RuntimeI18nBundle i18n,
    EntitlementsView entitlements,
    RuntimeReadinessView readiness,
    NotificationSummaryView notifications,
    PageModelRef pageModelRef,
    @Nullable List<RuntimeBootstrapNotice> notices
) {}
```

`PrivateNavigationModel` is **absent** — navigation comes from the page-model response
(`shell.navigationDrawer`), which `features.pagemodel` already resolves via JSON fragments
(`private_sidebar_cashier`, `private_footer_links`, etc.).

## Settings assembly

```java
RuntimeSettingsView assembleSettings(TchRequestContext ctx, PrivateBootstrapSpace space) {
    // locale, timezone, currency — from platform.tenantconfig.api
    TenantConfigView config = tenantConfigFacade.runtimeConfig(ctx, space);
    // feature flags — from catalog.settings.api (effective BOOLEAN values for tenant/global)
    Map<String, Boolean> features = settingsFacade.effectiveBooleanSettings(ctx);
    return new RuntimeSettingsView(config.locale(), config.timezone(), config.currency(), features);
}
```

## PageModelRefResolver

```java
PageModelRef resolve(PrivateBootstrapSpace space) {
    return switch (space) {
        case PLATFORM -> new PageModelRef("/app/platform", "/platform/dashboard", null);
        case ADMIN    -> new PageModelRef("/app/admin",    "/tenant/dashboard",   null);
        case CASHIER  -> new PageModelRef("/app/cashier",  /* confirm endpoint */, null);
    };
}
```

Cashier page-model endpoint to be confirmed in pre-flight (likely `/tenant/cashier/home`
from FEATURE_PAGEMODEL.md — note that is a BFF, not a PageModel; confirm whether a
`/tenant/cashier/dashboard` page-model endpoint exists or needs to be added to
`features.pagemodel` as a gap-fill).

## Notification summary polling

Polling endpoint stays in `platform.notification` — no new controller in `features/runtime`.
`RuntimeBootstrapService` calls `platform.notification.api` during bootstrap assembly.
Frontend polls existing `platform.notification` summary endpoint directly.

If `/summary` is absent on any notification controller, it is added there:
- `TenantNotificationController` → `GET /tenant/me/notifications/summary`
- Equivalent for admin/platform scopes (confirm existing paths).

## Security

| Space    | Required role(s)              | Tenant context       |
|----------|-------------------------------|----------------------|
| PLATFORM | `SUPER_ADMIN`                 | null allowed         |
| ADMIN    | `TENANT_ADMIN`                | required             |
| CASHIER  | `CASHIER` or `OPERATOR`       | required             |

Tenant resolved from `TchRequestContext` only — never from client body.
Unresolvable space or missing entitlements → `403 ProblemDetail`.

## Soft failures

| Source        | Behaviour                                                  |
|---------------|------------------------------------------------------------|
| theme         | `notices` WARNING; response includes fallback theme hint   |
| i18n          | `notices` WARNING; response omits bundle                   |
| notifications | `notices` WARNING; `unreadCount = 0`                       |
| identity      | hard 401/403                                               |
| entitlements  | hard 403                                                   |

## Cashier readiness V1

`ReadinessFacade` returns `PARTIAL` with `MISSING` checks for: terminal binding, open session,
seller assignment. Expected — operational context is a future slice.
