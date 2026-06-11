# Change: Mobile Runtime Bootstrap Consumption V1

## Why

The initial mobile POS foundation defined a typed `RuntimeSettings` model but did not
implement the now-stable backend runtime bootstrap reads. Mobile must consume the
server-composed bootstrap contracts instead of calling settings, i18n, theme, gates,
or notification catalogs independently.

## What

- Load the global public-safe runtime from `GET /public/runtime/bootstrap` before
  authentication.
- Replace the public runtime with the tenant-resolved POS runtime from
  `GET /tenant/runtime/bootstrap` after authentication.
- Poll the lightweight `GET /tenant/runtime/state` contract during an authenticated
  session and re-run the tenant bootstrap once when its runtime version hints change.
- Apply bootstrap settings, i18n overrides, gates/entitlements, readiness,
  notification summary, and notices through typed app-scoped Riverpod state.
- Keep bundled Haitian Creole translations, the single Tchalanet Material 3 theme,
  and safe settings values as startup and failure fallbacks.
- Document settings as the V1 configuration/feature-toggle mechanism before any
  future Unleash adoption.

## Impact

- Mobile bootstrap, settings, i18n, authorization gates, and app composition.
- The existing backend runtime-state contract must publish meaningful changing
  version hints; its current static `boot-v1` value cannot detect permission, i18n,
  or settings changes.

## Non-goals

- Business-rule enforcement in mobile.
- Unleash integration.
- Admin settings editing.
- Direct mobile calls to settings, i18n, theme, entitlement, or notification
  catalogs.
- Tenant-branded public bootstrap; the current public contract is global/public-safe.
- Dynamic application of bootstrap theme data; Mobile V1 keeps the single Tchalanet
  Material 3 theme.
- Repeated full-bootstrap polling; authenticated polling uses the lightweight runtime
  state contract.
- PageModel consumption. Mobile V1 ignores bootstrap `pageModelRef` values and loads
  feature data through explicit typed POS endpoints.
