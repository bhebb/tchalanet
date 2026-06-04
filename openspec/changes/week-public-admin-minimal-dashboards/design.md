# Design

## Slices

This work is a foundation slice.
It proves the integration pattern for public widgets and minimal role dashboards, then leaves richer integrations for later.
Future work should reuse the same rails instead of adding separate one-off dashboard/page contracts.

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
- Business rules stay in backend/core/platform owners.
- The frontend does not infer tenant readiness or seller eligibility.
- Widget rendering errors are local to the widget when possible.
- Future integrations must extend the same widget/action contract pattern rather than bypass it with hard-coded pages.
