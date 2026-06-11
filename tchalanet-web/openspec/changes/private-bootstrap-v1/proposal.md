# Change: private-bootstrap-v1

## Why

The current `AppRuntimeStore.initPrivateRuntime()` resolves theme and settings via independent
calls with no defined ordering, no space concept, and no entitlement/navigation/readiness data.
The private shell renders before the runtime contract is complete, and there is no coordination
between bootstrap state and page content loading.

This change introduces a **single `GET /runtime/bootstrap` flow** (server resolves space from
JWT — no space param on the frontend). The flow is:

1. `AuthGuard` confirms Keycloak session.
2. `PrivateRuntimeInitializer` calls `GET /runtime/bootstrap`.
3. Theme and i18n are applied before the shell renders.
4. `PrivateShell` renders from the bootstrap store — no additional HTTP calls for shell chrome.
5. `PrivatePageModelService` loads page content (including sidenav) from the `pageModelRef`.
6. `NotificationPollingService` starts after bootstrap completes.

This replaces the current multi-call approach with a single predictable sequence while keeping the
public bootstrap path (`initPublicRuntime`) fully unchanged.

## What changes

### `apps/tch-portal/src/app/core/runtime/`

New files alongside the existing `AppRuntimeStore`:

- `private-bootstrap.model.ts` — TypeScript interfaces: `RuntimeBootstrapResponse`,
  `PrivateBootstrapStatus`, `PrivateSpace`, `PageModelRef`, `AuthenticatedUserView`,
  `TenantContextView`, `RuntimeSettingsView`, `RuntimeThemeView`, `RuntimeI18nBundle`,
  `EntitlementsView`, `RuntimeReadinessView`, `RuntimeReadinessCheck`,
  `NotificationSummaryView`, `NotificationPreview`, `RuntimeBootstrapNotice`.
  **No navigation interfaces** — sidenav comes from `PageRuntimeResponse.shell.navigationDrawer`.
- `private-bootstrap.service.ts` — calls single `GET /runtime/bootstrap` via `TchBackendClient`.
- `private-bootstrap.store.ts` — Angular signal store: status, user, space, readiness,
  notifications, pageModelRef. No navigation signal.
- `private-runtime-initializer.ts` — orchestrates: call bootstrap → apply theme → apply i18n →
  store state → start notification polling.
- `runtime-theme.service.ts` — applies `RuntimeThemeView` to `ThemeStore`.
- `runtime-i18n.service.ts` — applies `RuntimeI18nBundle` to `I18nFacade`.
- `notification-polling.service.ts` — polls the notification summary endpoint (from
  `platform.notification`) every 10 min; forced refresh at 30 min; stops on logout; no
  duplicate pollers.

`AppRuntimeStore.initPrivateRuntime()` becomes a thin delegate to `PrivateRuntimeInitializer`.
The `initPublicRuntime()` path is untouched.

### `apps/tch-portal/src/app/features/private/shell/`

`PrivateShell` already exists. Changes:

- Read `space`, `user`, `readiness`, `notifications` from `PrivateBootstrapStore`.
- Read sidenav from `PageRuntimeResponse.shell.navigationDrawer` (page-model store), not from
  bootstrap store. `private-navigation.model.ts` is deleted — superseded by page-model shell.
- Implement all six status states: `idle` / `loading` / `ready` / `partial` / `blocked` / `error`.
- Hard failure (401 → redirect Keycloak; 403 → no-access page; entitlements missing → error).
- Soft failure (theme/i18n/notifications missing → `partial` with notice, shell still renders).
- `PrivateShell` does not load page content; page content is loaded by route resolver or
  page-model renderer.

### `apps/tch-portal/src/app/features/private/page-model/` (new)

- `private-page-model.model.ts` — `PrivatePageModelResponse` (aligned with `libs/page-model`
  `PageRuntimeResponse`; no parallel contract — wrap or extend the existing type).
- `private-page-model.service.ts` — `load(ref: PageModelRef)` via `TchBackendClient`.
- `private-page-renderer.component.ts` — delegates to existing widget renderer from `libs/widgets`.

### `apps/tch-portal/src/app/core/auth/auth.guard.ts`

Extended to trigger `PrivateRuntimeInitializer.initialize(url)` after Keycloak session is
confirmed. Private routes → bootstrap triggered. Public routes → not triggered.

### `libs/api/contracts` (or `apps/tch-portal/src/app/shared/types` as staging)

Bootstrap contracts are initially local to the app (`core/runtime/private-bootstrap.model.ts`).
Migration to `libs/api/contracts` is a separate slice, done only once contracts are stable.

## Impact

- Touches only `tchalanet-web`.
- `AppRuntimeStore` public path unchanged.
- Existing `PrivateShell`, `auth.guard.ts` extended — not replaced.
- `libs/page-model` not changed in this slice; `PrivatePageModelService` consumes
  `PageRuntimeResponse` shape via `TchBackendClient`.
- No new Nx lib created; no Nx boundary changes.
- Backend counterpart: `tchalanet-server/openspec/changes/private-bootstrap-v1/`.

## Non-goals

- No navigation model in the bootstrap store — sidenav comes from PageModel shell.
- No separate bootstrap call per space — single endpoint, server resolves from JWT.
- No websocket or SSE notifications.
- No `libs/shared-auth` or `libs/shared-i18n` extraction (future slice).
- No full i18n system replacement — `RuntimeI18nService` wraps existing `I18nFacade`.
- No offline bootstrap or multi-tab sync.
- No new Nx lib unless a stable boundary is proven during implementation.
- No change to public PageModel or public shell.
