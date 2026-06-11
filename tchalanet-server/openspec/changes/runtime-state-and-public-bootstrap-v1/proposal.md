# Change: runtime-state-and-public-bootstrap-v1

## Status

Proposed — follow-up to `private-bootstrap-v1`.

## Why

`private-bootstrap-v1` introduced `GET /runtime/bootstrap` for the private shell after Keycloak
login. Two gaps remain:

1. **Private runtime refresh** — the full bootstrap must not be re-called every few minutes while a
   user works. A lightweight endpoint is needed to surface readiness, notifications, blocking state,
   and version hints during a session.
2. **Public bootstrap** — public routes (no Keycloak) need the same clean process as private routes:
   a single lightweight runtime call returning public settings, theme, i18n, navigation, readiness,
   and a `pageModelRef`.

Decision: **no new feature, no new controller per case.** Keep `RuntimeController` +
`RuntimeService`. The existing `/runtime/bootstrap` is split/renamed to `/tenant/runtime/private-bootstrap`,
and two endpoints are added.

The bootstrap responses now carry the **i18n bundle** (private and public) so the frontend stops
doing a separate `/public/i18n` merge call; the surface is selected via header or query param.

## What changes

- `RuntimeController` exposes three endpoints (thin, no business logic, no `/api/v1` prefix, receives
  `TchRequestContext`):
  - `GET /tenant/runtime/private-bootstrap` — full private startup runtime (user, space, tenantContext,
    settings, theme, **i18n**, entitlements, private navigation, readiness, notifications summary,
    `pageModelRef`).
  - `GET /tenant/runtime/private-state` — lightweight refresh (status, readiness, notifications summary,
    blocking state, version hints, notices). Must NOT return full i18n/theme/navigation/settings/
    profile/page-model/dashboard data.
  - `GET /public/runtime/public-bootstrap` — public startup, no auth (public settings, theme, **i18n**,
    navigation, light readiness, `pageModelRef`). Must NOT expose private/user/entitlement data.
- `RuntimeService` gains `privateState(context)` and `publicBootstrap(context)`; `privateBootstrap`
  carries i18n.
- Surface selection for i18n via header or query param so one runtime path serves multiple surfaces.
- `BLOCKED` / `FORCE_RELOAD` runtime status drives frontend blocking and reload-once behavior.

## Impact

- `tchalanet-server` `features/runtime` (controller, service, model). No new module.
- Coordinates with web changes `web-runtime-bootstrap-state-i18n` (frontend consumption + ngx-translate
  loader rework) and `route-private-dashboards-through-widget-engine` (dashboards render after
  bootstrap + pageModelRef).
- Cross-project tracking: this is the backend half; the frontend half lives in `tchalanet-web`.

## Non-goals

- No websocket/SSE notifications, no multi-tab sync, no offline runtime refresh.
- No PageModels or large dashboard payloads inside bootstrap (PageModel stays a separate call).
- No public notifications, no tenant-branded public pages, no advanced cache invalidation.
- No draw/result operations or manual result entry.
