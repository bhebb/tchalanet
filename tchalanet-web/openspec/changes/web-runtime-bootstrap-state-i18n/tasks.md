# Tasks

> Frontend half of `runtime-state-and-public-bootstrap-v1`. Check `[ ]` → `[x]` in real time.
> Depends on the backend endpoints being available (mock/stub locally until then).

## 1. i18n loader rework (store-backed)
- [x] i18n delivered by bootstrap and overlaid via `TranslateService.setTranslation(lang, msgs, merge)`
      (the "store" is ngx-translate's own bundle; private + public initializers overlay it).
- [x] Replace `MergedTranslateLoader` HTTP merge with a **local-only** loader; local `fr/en/ht.json`
      stay as offline fallback only.
- [~] Surface selection: public passes `?locale=`; the surface set is fixed server-side per scope
      (no per-request surface header needed for V1).
- [x] Update `PORTAL_I18N_CONFIG` (dropped `backendPath`/`surfaces`); `app.config` options trimmed.
- [ ] Add a dedicated loader spec (local hit + missing-key fallback). `normalizeBackendTranslations`
      helper retained; existing `i18n-runtime.spec.ts` still green.

## 2. Public bootstrap
- [x] `PublicBootstrapService` → `GET /public/runtime/bootstrap` (anonymous, `TchBackendClient`).
- [x] `public-bootstrap.model.ts` + `public-bootstrap.store.ts` (settings/theme/navigation/readiness/
      pageModelRef) + `public-runtime-initializer.ts` (overlays i18n).
- [~] Wired into `AppRuntimeStore.initPublicRuntime()`; full PageModel-from-`pageModelRef` route flow
      still uses existing `getPublicPage()` (follow-up).
- [x] Public bootstrap failure captured on the store; local i18n fallback keeps the app usable.

## 3. Private bootstrap alignment
- [x] Point private bootstrap at `/tenant/runtime/bootstrap`.
- [x] i18n applied from bootstrap via `setTranslation` (no separate i18n call on login).

## 3b. Collapse to two calls (bootstrap + page)
- [x] Apply **theme** from bootstrap (`ThemeStore.applyBootstrapTheme`) — dropped `load{Public,Private}Theme`.
- [x] Apply **settings** from bootstrap (`RuntimeSettingsStore.applyBootstrapSettings`) — dropped
      `load{Public,Private}Settings`.
- [x] `AppRuntimeStore`: public → 1 public bootstrap; private(auth) → 1 private bootstrap;
      private(anon)/error → public bootstrap fallback. Net per page: **bootstrap + page** API calls.
- [x] Specs updated (`app-runtime.store.spec`, `runtime-paths.spec`); `nx test` shared-config/ui-theme green.
- [ ] Note: `/assets/i18n/{lang}.json` (static local fallback bundle) still fetched as the i18n base
      before the bootstrap overlay — local asset, not a backend API call.

## 4. Private runtime monitor (DEFERRED — do last, after public is validated)
- [ ] `PrivateRuntimeStateService` → `GET /tenant/runtime/state`.
- [ ] `PrivateRuntimeMonitor`: poll every 10 min; force at 30 min; on tab focus if stale > 2 min;
      after critical actions.
- [ ] Update notifications/readiness/blocking state from private-state.
- [ ] On version change or `FORCE_RELOAD` → call full private bootstrap once (cooldown, no loop).
- [ ] Stop polling on logout.

## 5. Blocking UI
- [ ] Shell blocking banner/overlay driven by `BLOCKED`; disable risky actions; keep logout/profile/
      help and safe navigation usable.
- [ ] Notification severity → UI (INFO badge, WARN toast, ERROR persistent notice, CRITICAL alert).

## 6. Route guards / rules
- [ ] No Keycloak login on public routes; no private bootstrap on public routes.
- [ ] No public bootstrap inside private shell; no private-state polling on public routes.

## 7. Tests
- [ ] `public-bootstrap.service.spec.ts`, `public-route-bootstrap.spec.ts`, `public-shell` render.
- [ ] `private-runtime-state.service.spec.ts`, `private-runtime-monitor.service.spec.ts`,
      `private-blocking-banner.component.spec.ts` (intervals, stale focus, version reload once,
      polling stops on logout).
- [ ] i18n loader spec (store-backed, fallback).

## 8. Validation
- [ ] `nx lint` / `nx test` / `nx build` green on affected projects.
- [ ] No remaining `/public/i18n` merge call; local bundles used only as fallback.

## 9. Static page fallback (DEFERRED — after the backend page contract is frozen)
> Goal: the public page renders even when the backend is unreachable (offline / KO), and the public
> deploy works as a static site on Vercel.
- [ ] Once the backend page contract is stable, capture a **complete real response** (public
      `/public/runtime/bootstrap` + the public page payload) and bundle it as a static fallback asset.
- [ ] `PublicRuntimeInitializer` / `PublicBootstrapStore`: on bootstrap failure, fall back to the
      bundled response so the public shell + PageModel still render (degrade, never blank).
- [ ] Verify the public page renders on **Vercel** with the backend unreachable (static fallback +
      local `fr/en/ht` i18n bundles, no API calls required).
- [ ] Keep the captured fallback in sync with the contract (note the capture date / schemaVersion).
