# Design

## Frontend surfaces

This slice is intentionally small, but it is not disposable.
It establishes the frontend pattern later integrations should reuse:

- typed backend API service;
- PageModel/widget renderer;
- route-level role ownership;
- action forms that submit to backend-owned commands;
- widget-local error handling.

## PageModel Renderer Contract

Frontend code should model the hierarchy as:

- page model;
- layout rows/regions;
- widget configs;
- action descriptors.

Each widget receives only its own widget config, resolved dynamic payload, local widget errors, and action descriptors.
Widgets must not receive the full page object.

V1 supports a small stable widget set:

- `HERO`;
- `ACTION_PANEL`;
- `STATUS_SUMMARY`;
- `FORM_ENTRY`;
- `LINK_LIST`;
- `NOTICE_LIST`.

Existing backend widget type names such as `HeroWidget`, `CheckTicketWidget`, `NewsTickerWidget`, `PublicDrawResultsWidget`, `PlansWidget`, `FeatureGridWidget`, and `TchalaSearchWidget` may be mapped into this V1 set by the registry.

Do not build a dynamic CMS, expression engine, frontend permission engine, or form schema runtime in this slice.

## Translation And Theme Rules

- Render direct translations from backend keys using the existing i18n pipe/service.
- If a translation is missing, render a stable fallback derived from the key rather than hiding the widget.
- Use theme tokens/CSS variables for colors, spacing, radius, and surface styling.
- Do not hard-code one-off widget colors outside token fallbacks.
- Theme fallback must keep widgets usable when public/tenant theme data is incomplete.

## Visual Direction From Mockups

Provided HTML/mockups are not source code.
They are composition references only.

Do not copy:

- markup structure verbatim;
- Tailwind class names or Tailwind CDN config;
- Google Fonts/Material Symbols CDN links;
- hard-coded colors such as mockup hex values;
- remote generated/profile images;
- French marketing copy as literal UI copy.

Adapt the direction into local Angular components:

- public top app bar;
- hero widget with direct i18n keys;
- dashboard/POS preview built from local markup and theme tokens;
- action panels rendered from PageModel actions;
- feature/link/notice widgets rendered through the widget registry.

The renderer should turn backend widget types into local components and tokenized styles, not reproduce a static landing page.

### Public page

The public page consumes backend widget/page payloads and renders supported widgets.
The first version is a renderer, not a page builder.

Backend source:

- `GET /api/v1/public/page-models/public.home`
- response data: `{ currentLang, langs, pageModel, dynamic }`

The renderer must handle:

- ordered sections/regions;
- known widget types;
- unsupported widgets;
- widget-local errors;
- public/no-auth bootstrap.
- missing translation values;
- missing/incomplete theme tokens.

### SUPER_ADMIN dashboard

The SUPER_ADMIN surface contains only two workflows:

- preview tenant onboarding;
- provision tenant with `initialAdminEmail` for the first tenant admin.

Backend source:

- `POST /api/v1/platform/tenant-onboarding/preview`
- `POST /api/v1/platform/tenant-onboarding/provision`

The UI should be compact and operational.
It should not look like a marketing landing page or analytics dashboard.

### TENANT_ADMIN dashboard

The TENANT_ADMIN surface contains only:

- onboard seller.

The UI must not expose tenant override fields.
The backend owns tenant resolution.

Backend source:

- `POST /api/v1/admin/identity/users`
- request uses `role`, `email`, optional `phone`, `firstName`, `lastName`, `outletId`, `terminalId`

## State

Use existing web runtime/auth/i18n/theme patterns.
Add local component state for form drafts unless the existing codebase already has a narrower store pattern for these flows.

## Error behavior

- Widget errors stay inside the widget.
- Unsupported widgets render a stable fallback.
- Admin command errors use existing backend response conventions.
- Invalid widgets with missing `id` or `type` render an invalid-widget fallback.
- A widget failure must not blank the page.

## Accessibility and UX

- Forms include labels, validation messages, loading state, and submit disabled state.
- Primary actions are obvious and limited to the scoped workflows.
- Empty states should be useful but short.
- Public mockup-inspired visuals must remain responsive, keyboard accessible, and readable with missing translations or fallback theme tokens.
