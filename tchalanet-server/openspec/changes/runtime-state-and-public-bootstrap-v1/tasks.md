# Tasks

> Backend half of the runtime split. Keep `RuntimeController` + `RuntimeService` (no new feature/
> controller). Check `[ ]` → `[x]` in real time. See `design.md` for payload detail.

## 1. Controller + service surface
- [x] Rename/serve `/tenant/runtime/private-bootstrap` (from existing `/runtime/bootstrap`); thin controller
      (`RuntimeBootstrapController` → `RuntimeController`, per-method `@PreAuthorize`).
- [x] Add `GET /tenant/runtime/private-state` → `RuntimeBootstrapService.privateState(context)`.
- [x] Add `GET /public/runtime/public-bootstrap` (no auth, `permitAll()` + SecurityConfig whitelist) →
      `RuntimeBootstrapService.publicBootstrap(locale)`.
- [x] i18n surface: public uses fixed public surface set; private uses surface derived from space.
      Public bootstrap accepts `?locale=`. (Header-based surface override not needed for V1.)

## 2. Models
- [x] Add `PrivateRuntimeStateResponse`, `RuntimeBlockingState`, `RuntimeBlockingAction`,
      `RuntimeVersionHints`, `PrivateRuntimeStatus`. (Reused existing `RuntimeNotificationSummary` +
      `RuntimeBootstrapNotice` for notifications/notices.)
- [x] Add `PublicBootstrapResponse`, `PublicSettingsView`, `PublicThemeView`, `PublicI18nBundle`,
      `PublicNavigationModel`/`PublicNavigationItem`, `PublicReadinessView`/`PublicReadinessCheck`.
      (`PageModelRef` reused.)
- [x] `RuntimeI18nBundle` already in private bootstrap response (from `private-bootstrap-v1`).

## 3. Private state logic
- [x] Resolve user + private space from `TchRequestContext`.
- [x] Lightweight readiness + notification summary + version hints.
- [x] Enforce lightweight contract: no full i18n/theme/navigation/settings/profile/page-model/dashboard.
- [ ] Map blocking sources (cashier session closed, terminal locked, tenant suspended, role revoked,
      maintenance) → `BLOCKED`. **V1: blocking returns null** — requires operational context (not yet
      implemented, see `RuntimeReadinessFacade`).
- [ ] `FORCE_RELOAD` when entitlements/navigation version changed. **V1: static `boot-v1`** — needs
      real version sources.

## 4. Public bootstrap logic
- [x] Public settings/theme/i18n/navigation/light readiness + `pageModelRef`.
- [x] No auth required; expose only public-safe data (no user/entitlements/private nav/notifications/
      internal readiness).
- [x] Language: `?locale=` else default `fr` (URL/browser resolution is the frontend's job).

## 5. Tests
- [ ] `RuntimePrivateStateControllerTest` / `RuntimePrivateStateServiceTest`: requires auth; returns
      notifications/readiness/blocking/version hints; does NOT return full nav/theme/i18n/settings;
      `BLOCKED` on cashier session closed and terminal locked; `FORCE_RELOAD` on entitlements/nav
      version change.
- [ ] `RuntimePublicBootstrapControllerTest` / `RuntimePublicBootstrapServiceTest`: no auth; returns
      public settings/theme/i18n/navigation/readiness/pageModelRef; does NOT return user/entitlements/
      private nav/notification summary/internal readiness.

## 6. Acceptance
- [ ] `RuntimeController` exposes `/tenant/runtime/private-state` and `/public/runtime/public-bootstrap`.
- [ ] `RuntimeService` has `privateState(context)` and `publicBootstrap(context)`.
- [ ] private-state is lightweight and does not return the full bootstrap payload.
- [ ] public-bootstrap works without authentication and is public-safe.
- [ ] Coordinated with web `web-runtime-bootstrap-state-i18n` consumption.
