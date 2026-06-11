# Change: private-bootstrap-v1

## Why

After Keycloak login the frontend has a valid token but nothing else: no space, no theme, no
entitlements, no page content. Today this state is resolved by multiple ad-hoc frontend calls
with no defined contract or ordering guarantee.

This change introduces a **single `GET /runtime/bootstrap` endpoint** in a new
`features/runtime` module. One controller, one service. The service receives the
`TchRequestContext`, resolves the space and tenant from the authenticated context, and
dispatches/filters the response accordingly — no role-specific branching at the controller level.

The sidenav is **not duplicated** in the bootstrap response. It is part of the PageModel
(`shell.navigationDrawer`), which is already resolved by `features.pagemodel` via JSON fragments
(`private_sidebar_cashier`, etc.). Bootstrap returns a `pageModelRef` pointing to the correct
existing page-model endpoint so the frontend knows where to call next.

The result is a clean 2-call sequence: `GET /runtime/bootstrap` → page-model endpoint.

## What changes

### New module — `features/runtime`

Single controller: `GET /runtime/bootstrap`

Single service: `RuntimeBootstrapService`
- resolves space from `TchRequestContext` roles (SUPER_ADMIN → PLATFORM, TENANT_ADMIN → ADMIN,
  CASHIER/OPERATOR → CASHIER)
- resolves tenant context from `TchRequestContext` (null for PLATFORM, required for others)
- assembles `RuntimeBootstrapResponse` — dispatching and filtering based on space
- soft failures (theme, i18n, notifications) populate `notices` without aborting

Shared models live in `features/runtime/model/`.

### Notification summary polling endpoint

The `GET /runtime/notifications/summary` polling endpoint belongs in `platform.notification`
(alongside existing `TenantNotificationController`, `AdminNotificationController`,
`OpsNotificationController`) because it delegates only to `NotificationService` without
composing other services. Add or confirm a `/summary` endpoint on each existing notification
controller — **not** in `features/runtime`.

### Page-model endpoints

No new page-model endpoints. `PageModelRefResolver` maps the resolved space to the correct
existing endpoint:

| Space    | Existing endpoint              |
|----------|--------------------------------|
| PLATFORM | `GET /platform/dashboard`      |
| ADMIN    | `GET /tenant/dashboard`        |
| CASHIER  | `GET /tenant/cashier/home` (confirm) |

The sidenav comes from the page-model response (`shell.navigationDrawer`) — not from bootstrap.

## Impact

- New module `features/runtime` — no existing module modified for bootstrap.
- `platform.notification` — confirm or add `/summary` endpoint on existing controllers.
- `platform.identity`, `platform.accesscontrol`, `platform.tenantconfig`, `platform.tenanttheme`,
  `catalog.settings`, `catalog.i18n` — consumed via `api/` only.
- `features.pagemodel` — no change.
- `features/platformadmin`, `features/tenantadmin`, `features/cashier` — no change.
- No DB migration required for V1.

## Non-goals

- No navigation model in the bootstrap response — sidenav comes from PageModel.
- No separate controller per role or space — one controller, context-based dispatch.
- No new page-model endpoints.
- No new notification service or controller — only a `/summary` endpoint if absent.
- No websocket or SSE notifications.
- No cashier operational context resolution in V1 (cashier readiness → PARTIAL, explicit).
- No multi-tab sync or offline bootstrap.
