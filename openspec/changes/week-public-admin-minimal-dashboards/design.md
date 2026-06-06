# Design

## Slices

This work is a foundation slice.
It proves the integration pattern for public widgets and minimal role dashboards, then leaves richer integrations for later.
Future work should reuse the same rails instead of adding separate one-off dashboard/page contracts.

## Contract Shape

The cross-project contract is intentionally simple:

- `PageModel`: identity, surface/scope, layout, widgets, notices, meta/schema version.
- `Layout`: ordered rows/regions and widget ids.
- `Widget`: id, type, title/titleKey, payload, actions, status/visibility.
- `Action`: id, kind, label/labelKey, destination or operation, optional capability, disabled/reason.

The backend returns payloads that are ready to render. The page request should not force Angular to make many extra calls for each widget.
Actions may call backend endpoints only when the user clicks/submits.

## Translation And Theme Rules

- The web renders direct translations from `labelKey`, `titleKey`, `descriptionKey`, and similar key fields.
- If a translation value is missing, the UI still renders a stable fallback from the key; missing translations do not block rendering.
- Widgets must consume theme tokens/CSS variables rather than hard-coded palette values.
- Theme fallback is allowed, but widgets must stay visually coherent when tenant/public theme tokens are incomplete.

## Visual Inspiration Boundary

External HTML/mockups are design inspiration only.
Do not copy their markup, classes, CDN dependencies, remote images, text, or hard-coded color palette.

For the public home experience, the useful idea is the composition:

- top app bar;
- strong hero with primary action;
- POS/dashboard preview visual;
- compact trust/status signals;
- small feature preview section;
- simple footer.

The implementation must express that composition through Tchalanet PageModel widgets, direct i18n keys, and theme tokens.
If a mockup uses Tailwind colors or Material color names, map the intent to existing Tchalanet tokens instead of copying values.

### Public page

Backend owns a stable public PageModel/widget payload.
Web owns rendering widgets from that payload.

The public page may include static and dynamic widgets, but this slice proves only the engine and a small set of safe public widgets.

### SUPER_ADMIN dashboard

The SUPER_ADMIN dashboard is not a general platform console in this slice.
It supports only:

- create tenant;
- create the first tenant admin for a tenant;
- view enough status to confirm the tenant/admin creation result.

### TENANT_ADMIN dashboard

The TENANT_ADMIN dashboard is not an operational analytics console in this slice.
It supports only:

- onboard seller;
- view enough status to confirm seller onboarding result.

## Project changes

Backend:

- existing public widget payload: `GET /api/v1/public/page-models/{logicalId}`;
- existing platform dashboard payload: `GET /api/v1/platform/page-models`;
- existing tenant dashboard payload: `GET /api/v1/tenant/page-models`;
- existing SUPER_ADMIN tenant provisioning: `POST /api/v1/platform/tenant-onboarding/preview` and `/provision`;
- existing TENANT_ADMIN/SUPER_ADMIN user creation: `POST /api/v1/admin/identity/users`.

Backend is not frozen. Implementation should reuse these routes first, but may modify backend code when the web integration exposes a real gap, including:

- response shape missing data needed by the approved UI;
- route or role behavior inconsistent with the intended workflow;
- missing widget source/payload for the public renderer;
- security or tenant-context bug;
- missing focused test coverage around the consumed contract.

Web:

- render public widgets;
- route SUPER_ADMIN to the minimal tenant/admin creation dashboard;
- route TENANT_ADMIN to the minimal seller onboarding dashboard.

## Guardrails

- Dashboard widgets are action blocks, not analytics cards.
- V1 widget types stay limited: `HERO`, `ACTION_PANEL`, `STATUS_SUMMARY`, `FORM_ENTRY`, `LINK_LIST`, `NOTICE_LIST`, plus mapped legacy backend types when consumed.
- No dynamic form schema engine, frontend rule expression engine, layout builder, permissions engine, or CMS in this slice.
- No copied mockup markup, no Tailwind CDN, no Google Font dependency, and no remote stock/profile images in the application implementation.
- Business rules stay in backend/core/platform owners.
- The frontend does not infer tenant readiness or seller eligibility.
- Widget rendering errors are local to the widget when possible.
- Future integrations must extend the same widget/action contract pattern rather than bypass it with hard-coded pages.
