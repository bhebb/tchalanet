# Design — runtime-state-and-public-bootstrap-v1

Source: `RUNTIME_STATE_AND_PUBLIC_BOOTSTRAP_V1.md`. Extends the existing `features/runtime`.

## 1. Target structure

```text
features/runtime/
  web/   RuntimeController.java
  app/   RuntimeService.java
         RuntimeNavigationFactory.java
         RuntimeReadinessResolver.java
         RuntimeStateResolver.java
         PublicRuntimeResolver.java
  model/ PrivateBootstrapResponse / PrivateRuntimeStateResponse / PublicBootstrapResponse
         PrivateNavigationModel / PublicNavigationModel
         RuntimeSettingsView / RuntimeThemeView / RuntimeI18nBundle / RuntimeReadinessView
         NotificationSummaryView / RuntimeVersionHints / RuntimeBlockingState / PageModelRef
```

V1 may stay simpler (`RuntimeController` + `RuntimeService` + `model/*`); resolvers/factories can be
extracted later if `RuntimeService` grows.

## 2. Controller

```java
@RestController
@RequestMapping("/runtime")
@RequiredArgsConstructor
class RuntimeController {
  private final RuntimeService runtimeService;

  @GetMapping("/private-bootstrap")
  ApiResponse<PrivateBootstrapResponse> privateBootstrap(TchRequestContext context) {
    return ApiResponse.ok(runtimeService.privateBootstrap(context));
  }
  @GetMapping("/private-state")
  ApiResponse<PrivateRuntimeStateResponse> privateState(TchRequestContext context) {
    return ApiResponse.ok(runtimeService.privateState(context));
  }
  @GetMapping("/public-bootstrap")
  ApiResponse<PublicBootstrapResponse> publicBootstrap(TchRequestContext context) {
    return ApiResponse.ok(runtimeService.publicBootstrap(context));
  }
}
```

Rules: thin controller, no business logic, no `/api/v1` in mapping, service receives
`TchRequestContext`, backend resolves user/tenant/space from context. i18n surface comes via header
or query param.

## 3. Endpoints

| Endpoint | Auth | Purpose | Returns |
|---|---|---|---|
| `GET /tenant/runtime/private-bootstrap` | yes | init private shell after login | user, space, tenantContext, settings, theme, **i18n**, entitlements, private navigation, readiness, notifications summary, pageModelRef |
| `GET /tenant/runtime/private-state` | yes | lightweight in-session refresh | status, readiness, notifications summary, blocking, version hints, notices |
| `GET /public/runtime/public-bootstrap` | no | init public pages without login | public settings, theme, **i18n**, navigation, light readiness, pageModelRef |

## 4. Private runtime state

`PrivateRuntimeStateResponse`: `status` (`READY|PARTIAL|BLOCKED|FORCE_RELOAD|SESSION_EXPIRED`),
`readiness`, `notifications`, `blocking?`, `versions`, `notices?`.

`privateState(context)` resolves: current user, current private space, lightweight readiness,
notification summary, blocking state, runtime version hints. It must NOT load full i18n / theme /
navigation / settings / user profile / page model / dashboard data / large lists.

Polling policy: after bootstrap T+0 initial; every 10 min; forced at T+30 min; on tab focus/resume if
last check older than 2 min; after critical actions (sell completed, session opened/closed, terminal
heartbeat failed, tenant setting changed, role/permission changed, notification marked read).

Blocking examples: cashier session closed, terminal locked, seller disabled, tenant suspended, outlet
closed, role revoked, permission removed, maintenance mode. `BLOCKED` payload carries
`code/titleKey/messageKey/severity/action`. Cashier `BLOCKED` disables sell/print/payout;
admin/platform `BLOCKED` may disable mutations while read-only pages stay visible.

Version hints: `bootstrapVersion` + optional `navigation/entitlements/theme/i18n/settings` versions.
V1 rule: if any runtime version changed → call full private bootstrap once (with cooldown; no loop on
failed reload). Full bootstrap again on: browser refresh, manual reload, logout/login, space switch,
`FORCE_RELOAD`, entitlements/navigation/theme/settings/i18n version change. Not for: normal
notification count changes, readiness warning only, page data refresh, minor dashboard changes.

Notifications: `NotificationSummaryView` = `unreadCount`, `criticalCount`, `latest[]`, `urgent?`.
Severity → UI: INFO badge, WARN toast, ERROR persistent notice, CRITICAL prominent alert. Notification
alone does not block; blocking comes from readiness/blocking state.

## 5. Public bootstrap

`PublicBootstrapResponse`: `settings`, `theme`, `i18n`, `navigation`, `readiness`, `pageModelRef`,
`notices?`.

- Public settings: `locale`, `timezone`, `supportedLocales[]`, `defaultCurrency`, `features{}`.
- Public theme: global Tchalanet public theme (tenant theme not used unless tenant-branded public
  pages are introduced later).
- Public i18n: `lang`, `messages{}`, `loadedAt`. Language resolution: URL lang > browser language >
  default `fr`.
- Public navigation: `items[]` + optional `footerItems[]` (id, labelKey, route, external?).
- Public readiness: light/safe (`READY|PARTIAL` + checks). Must NOT expose internal platform health,
  private tenant/terminal/session state, provider credentials, or security-sensitive readiness.
- `PageModelRef`: `{ route, endpoint, version? }`. Public PageModel stays a separate call
  (`GET /public/page-model?route=...`) and must not contain private data.
