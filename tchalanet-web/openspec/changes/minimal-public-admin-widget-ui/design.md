# Design

## Frontend surfaces

This slice is intentionally small, but it is not disposable.
It establishes the frontend pattern later integrations should reuse:

- typed backend API service;
- PageModel/widget renderer;
- route-level role ownership;
- action forms that submit to backend-owned commands;
- widget-local error handling.

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

## Accessibility and UX

- Forms include labels, validation messages, loading state, and submit disabled state.
- Primary actions are obvious and limited to the scoped workflows.
- Empty states should be useful but short.
