# Design

## Context packs

- `openspec/context/05-version-guard.md`
- `tchalanet-server/openspec/context/10-non-negotiables.md`
- `tchalanet-server/openspec/context/79-request-context-rules.md`
- `tchalanet-server/openspec/context/81-features-rules.md`

## Existing backend routes

The backend slice is mostly present:

- `GET /api/v1/public/page-models/{logicalId}` resolves anonymous public PageModels.
- `GET /api/v1/platform/page-models` resolves the SUPER_ADMIN dashboard PageModel server-side.
- `GET /api/v1/tenant/page-models` resolves the tenant dashboard PageModel from request context.
- `POST /api/v1/platform/tenant-onboarding/preview` previews tenant provisioning.
- `POST /api/v1/platform/tenant-onboarding/provision` creates tenant and optional initial tenant admin.
- `POST /api/v1/admin/identity/users` creates tenant-scoped users, including sellers/cashiers.

These routes are the first rails for future integrations:

- public and private screens consume PageModel/widget payloads;
- role dashboards resolve server-side instead of accepting arbitrary logical ids;
- mutations use owning action endpoints;
- contract tests protect the web-facing response shapes.

## Backend ownership

This slice should live in `features/` when it assembles page/dashboard payloads or orchestrates cross-module APIs.
It must call platform/core/catalog capabilities through their public APIs only.

Most of the server code already exists, but backend edits remain valid when they are targeted and justified by the slice.
Do not treat the server as frozen.

Allowed backend changes include:

- adding missing fields to an existing response needed by the approved UI;
- adding or adjusting a widget provider/source for `public.home`;
- fixing route authorization or request-context behavior;
- narrowing an action endpoint if the generic endpoint is unsafe or too broad for the UI;
- adding focused tests around the consumed contracts.

## Public widget payload

The existing public endpoint returns:

- page identity;
- ordered widget regions;
- widget type/code;
- i18n key references;
- static configuration;
- dynamic payload by widget id or source;
- contained widget errors.

The backend decides which widgets exist.
The web decides how supported widget types render.

The payload should remain ready to render:

- include widget ids and types for every renderable widget;
- include translation keys directly (`title_key`, `description_key`, `label_key`, etc.);
- include action descriptors instead of asking frontend to infer workflows from roles;
- include enough payload for display widgets without forcing per-widget follow-up calls;
- include schema/contract version metadata when the backend contract changes.

The backend should not execute sensitive actions through PageModel.
PageModel exposes action descriptors; owning endpoints execute mutations.

## Translation And Theme Contract

- Backend may send keys even when a resolved translation is not available.
- Missing translation values must not remove the widget from the payload.
- Backend should keep text fields as keys or explicit fallback labels; the web performs direct translation/fallback rendering.
- Theme information should be expressed as preset/token references or CSS-token-compatible values, not hard-coded frontend colors.
- Cache may optimize templates, public content, settings, i18n, and theme presets, but never critical action authorization or mutation results.

## SUPER_ADMIN actions

The SUPER_ADMIN web flow should use existing backend actions:

- preview tenant onboarding;
- provision tenant with optional `initialAdminEmail`.

If web needs "create another tenant admin after tenant exists", that is a separate confirmation task.

## TENANT_ADMIN actions

The TENANT_ADMIN web flow should use:

- `POST /api/v1/admin/identity/users` with tenant context from the authenticated request and a seller/cashier role.

`CreateUserRequest` does not carry tenant id. The backend uses `TchRequestContext`.

## Error behavior

- Public widget provider failures become widget-local errors.
- Admin command failures return normal backend error/notice conventions.
- The frontend should not receive internal provider exception text.
- Unknown or invalid widget payloads should be testable as contained contract failures, not whole-page failures.
