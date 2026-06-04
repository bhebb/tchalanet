# Change: minimal-public-admin-widget-ui

## Why

The web app needs a small usable slice instead of a broad dashboard buildout.
This change proves the public widget renderer and the first admin workflows without building analytics or a full console.

It also lays frontend rails for later integrations: typed API services, PageModel/widget rendering,
role dashboard routing, contained widget errors, and action-form patterns.

## What changes

- Render the public page from a backend widget/page payload.
- Add SUPER_ADMIN dashboard UI using the existing tenant onboarding backend flow.
- Add TENANT_ADMIN dashboard UI using the existing identity user creation backend flow.
- Keep layouts, forms, and states focused on these actions.

## Impact

- Touches Angular/Nx app routes and feature surfaces.
- May use existing runtime store, i18n, theme, auth, and shell primitives from the web foundation.
- Depends on existing backend contracts documented in `minimal-public-admin-widget-bff`.
- Future web integrations should reuse the renderer/service/form patterns introduced here.

## Non-goals

- No cashier dashboard.
- No analytics-heavy cards.
- No full tenant/user CRUD tables.
- No CMS editor or widget builder UI.
- No mock-only dashboards that diverge from backend contracts.
