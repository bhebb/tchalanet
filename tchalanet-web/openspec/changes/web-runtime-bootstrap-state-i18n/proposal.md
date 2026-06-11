# Change: Web Runtime Bootstrap, Private-State Monitor and i18n Loader Rework

## Status

Proposed — frontend half of backend `runtime-state-and-public-bootstrap-v1`.

## Why

The backend is consolidating runtime startup on `features/runtime`: public and private routes both
call a single runtime service. Two consequences for the web app:

1. **i18n moves into bootstrap.** Today `MergedTranslateLoader` does
   `forkJoin(local /assets/i18n/{lang}.json + GET /public/i18n?surface=...) → merge`. The backend now
   returns the i18n bundle **inside** the bootstrap response (`/public/runtime/public-bootstrap` and
   `/tenant/runtime/private-bootstrap`), with the surface selected via header or query param. The
   ngx-translate loader mechanic must be reworked: bootstrap fills an i18n store, the loader reads
   from that store, and local bundles remain only as offline fallback.
2. **Both public and private bootstrap through `/runtime/*`.** Public routes call
   `/public/runtime/public-bootstrap` (no Keycloak); private routes call `/tenant/runtime/private-bootstrap` after
   login, then a lightweight `/tenant/runtime/private-state` is polled during the session.

This change implements the frontend consumption so later surfaces (dashboards, public pages) render
on a clean, single runtime contract.

## What changes

- **i18n loader rework**: replace the HTTP merge loader with a store-backed loader. Bootstrap writes
  the resolved bundle into a runtime i18n store; `TranslateLoader.getTranslation(lang)` reads it;
  local `fr/en/ht` bundles stay as fallback only. Surface passed via header/param when fetching.
- **Public bootstrap**: `PublicBootstrapService` + store; apply public settings/theme/i18n/navigation,
  then load PageModel from `pageModelRef.endpoint`. No Keycloak on public routes.
- **Private bootstrap alignment**: point the existing private bootstrap at `/tenant/runtime/private-bootstrap`
  and consume the embedded i18n/theme/navigation.
- **Private runtime monitor**: `PrivateRuntimeStateService` + `PrivateRuntimeMonitor` polling
  `/tenant/runtime/private-state` (10 min; forced at 30 min; on tab focus if stale > 2 min; after critical
  actions). Update notifications/readiness/blocking; call full bootstrap once on version change or
  `FORCE_RELOAD` (with cooldown). Stop polling on logout.
- **Blocking UI**: shell blocking banner/overlay driven by `BLOCKED` state; disable risky actions
  while keeping logout/profile/help and safe navigation available.

## Impact

- `apps/tch-portal/src/app/core/runtime/**` (public/private bootstrap + state services/stores,
  runtime monitor, runtime-i18n/theme services), `core/i18n/**` (loader rework), `shared-config`
  (`PORTAL_I18N_CONFIG` / runtime paths), private + public shells (blocking banner).
- Depends on backend `runtime-state-and-public-bootstrap-v1` for endpoints and payloads.
- Unblocks `route-private-dashboards-through-widget-engine` (i18n delivery + `pageModelRef`).

## Non-goals

- No websocket/SSE notifications, no multi-tab sync, no offline runtime refresh beyond the i18n
  fallback bundles.
- No dashboard widget rendering (owned by `route-private-dashboards-through-widget-engine`).
- No new Nx lib without a validated boundary.
