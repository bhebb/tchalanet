# Tasks — private-bootstrap-v1 (web)

## 0. Pre-flight

- [ ] Confirm backend `private-bootstrap-v1` change is active and endpoint contracts are stable
      enough to start frontend integration (at minimum: models and endpoint URLs agreed).
- [ ] Read `docs/conventions/auth.md` — confirm AuthGuard pattern before extending it.
- [ ] Read `docs/conventions/state-management.md` — confirm signal store pattern before writing
      `PrivateBootstrapStore`.
- [ ] Read `docs/conventions/http-api.md` — confirm `TchBackendClient` usage pattern.
- [ ] Read `libs/api/README.md` — confirm `TchBackendClient` method signatures.

## 1. Model contracts

- [ ] Create `core/runtime/private-bootstrap.model.ts` with TypeScript interfaces:
      `RuntimeBootstrapResponse`, `PrivateBootstrapStatus`, `PrivateSpace`, `PageModelRef`,
      `AuthenticatedUserView`, `TenantContextView`, `RuntimeSettingsView`, `RuntimeThemeView`,
      `RuntimeI18nBundle`, `EntitlementsView`, `RuntimeReadinessView`, `RuntimeReadinessCheck`,
      `NotificationSummaryView`, `NotificationPreview`, `RuntimeBootstrapNotice`.
      **No navigation interfaces** — navigation comes from `PageRuntimeResponse.shell`.
- [ ] Export all types from `core/runtime/index.ts`.
- [ ] Confirm `PrivatePageModelResponse` aligns with `PageRuntimeResponse` from `libs/page-model`
      (no parallel contract — wrap or alias only).
- [ ] Confirm notification polling endpoint path from `platform.notification` (pre-flight §0).

## 2. Bootstrap service and store

- [ ] Create `core/runtime/private-bootstrap.service.ts` — calls `GET /runtime/bootstrap`
      via `TchBackendClient` (single endpoint, no space param).
- [ ] Create `core/runtime/private-bootstrap.store.ts` — signal store:
      `status`, `bootstrap`, computed `user`, `space`, `readiness`, `notifications`,
      `pageModelRef`. No `navigation` computed — navigation from page-model store.
- [ ] Remove or delete `private-navigation.model.ts` from `features/private/shell/` if it
      exists — superseded by `PageRuntimeResponse.shell.navigationDrawer`.

## 3. Runtime services

- [ ] Create `core/runtime/runtime-theme.service.ts` — applies `RuntimeThemeView` to `ThemeStore`.
- [ ] Create `core/runtime/runtime-i18n.service.ts` — applies `RuntimeI18nBundle` to `I18nFacade`.
- [ ] Create `core/runtime/notification-polling.service.ts`:
  - [ ] 10-minute polling interval.
  - [ ] 30-minute forced refresh (one-shot timer after `startAfterLogin()`).
  - [ ] Stop on logout (clear interval + timer).
  - [ ] No duplicate pollers (running-flag guard).
  - [ ] Failure → `unreadCount=0`, no crash.

## 4. Runtime orchestration

- [ ] Create `core/runtime/private-runtime-initializer.ts` — `initialize(url)`:
      resolve space → call bootstrap → apply theme → apply i18n → store state → start polling.
- [ ] Extend `AppRuntimeStore.initPrivateRuntime()` to delegate to `PrivateRuntimeInitializer`.
      Keep `initPublicRuntime()` untouched.

## 5. AuthGuard extension

- [ ] Extend `core/auth/auth.guard.ts` to trigger `PrivateRuntimeInitializer.initialize(url)`
      after Keycloak session confirmation on private routes.
- [ ] Public routes must not trigger bootstrap.
- [ ] Bootstrap is not re-triggered if store is already `ready`.

## 6. PrivateShell wiring

- [ ] Wire `PrivateShell` to read `space`, `user`, `readiness`, `notifications` from
      `PrivateBootstrapStore`.
- [ ] Wire sidenav from `PageRuntimeResponse.shell.navigationDrawer` (from page-model store),
      not from bootstrap store.
- [ ] Implement status-driven rendering:
  - [ ] `loading` → skeleton / loading shell.
  - [ ] `ready` → shell + page.
  - [ ] `partial` → shell + notice banner.
  - [ ] `blocked` → shell + blocked state panel.
  - [ ] `error` (401) → redirect to Keycloak.
  - [ ] `error` (403) → no-access page.
  - [ ] `error` (other) → private error page.

## 7. Private PageModel integration

- [ ] Create `features/private/page-model/private-page-model.model.ts` — alias/wrapper of
      `PageRuntimeResponse` from `libs/page-model`.
- [ ] Create `features/private/page-model/private-page-model.service.ts` — `load(ref)` via
      `TchBackendClient`.
- [ ] Create `features/private/page-model/private-page-renderer.component.ts` — delegates to
      existing `WidgetHostComponent` / widget renderer from `libs/widgets`.
- [ ] PageModel failure → page-level error state only; shell stays intact.

## 8. Tests

- [ ] `private-bootstrap.service.spec.ts` — calls correct endpoint per space.
- [ ] `private-runtime-initializer.spec.ts` — theme applied, i18n applied, store set, polling
      started.
- [ ] `runtime-theme.service.spec.ts` — applies RuntimeThemeView to ThemeStore.
- [ ] `notification-polling.service.spec.ts` — 10-min interval, 30-min forced refresh, stops on
      logout, no duplicate pollers, failure does not crash.
- [ ] `private-page-model.service.spec.ts` — calls ref.endpoint, maps response.
- [ ] `private-shell.component.spec.ts`:
  - [ ] Private route triggers bootstrap.
  - [ ] Public route does not trigger bootstrap.
  - [ ] All six status states render correctly.
  - [ ] PageModel failure does not break shell.
  - [ ] Navigation is populated from bootstrap store.
