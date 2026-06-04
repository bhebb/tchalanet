# Tasks

## 1. Contract alignment

- [x] Read backend OpenSpec `minimal-public-admin-widget-bff` before implementation.
- [ ] Map existing backend widget/page payload to typed frontend contracts.
- [ ] Confirm routes for public, SUPER_ADMIN, and TENANT_ADMIN surfaces.
- [x] Confirm backend calls to consume:
  - `GET /api/v1/public/page-models/public.home`
  - `GET /api/v1/platform/page-models`
  - `GET /api/v1/tenant/page-models`
  - `POST /api/v1/platform/tenant-onboarding/preview`
  - `POST /api/v1/platform/tenant-onboarding/provision`
  - `POST /api/v1/admin/identity/users`
- [ ] If a backend contract is missing data or is unsafe/awkward for the UI, stop and update the backend slice before inventing frontend-only workarounds.

## 2. Public widget renderer

- [ ] Create or adapt widget renderer entrypoint for public page payload.
- [ ] Support the approved widget types for this slice.
- [ ] Add unsupported-widget fallback.
- [ ] Add widget-local error rendering.
- [ ] Ensure public page works without authenticated session.

## 3. SUPER_ADMIN dashboard UI

- [ ] Route SUPER_ADMIN dashboard to minimal admin surface.
- [ ] Add tenant onboarding form using `TenantProvisioningRequest`.
- [ ] Include `initialAdminEmail` as the first tenant-admin creation field.
- [ ] Add optional preview step using `/platform/tenant-onboarding/preview`.
- [ ] Show success/error state from backend responses.
- [ ] Do not show analytics, jobs, flags, audit, release notes, or service health.

## 4. TENANT_ADMIN dashboard UI

- [ ] Route TENANT_ADMIN dashboard to minimal tenant admin surface.
- [ ] Add seller onboarding form using `CreateUserRequest`.
- [ ] Show success/error state from backend responses.
- [ ] Do not expose tenant id override UI.
- [ ] Do not show sales, draws, payout, reconciliation, or limits dashboards.

## 5. Tests / validation

- [ ] Public widget page renders supported widgets.
- [ ] Unsupported widget fallback renders without breaking page.
- [ ] SUPER_ADMIN can submit create tenant flow.
- [ ] SUPER_ADMIN can submit create tenant admin flow.
- [ ] TENANT_ADMIN can submit seller onboarding flow.
- [ ] Route guards prevent wrong roles from accessing each dashboard.
- [ ] Run focused Nx tests for touched app/libs.

## 6. Documentation

- [ ] Document widget component mapping near the renderer or feature README.
- [ ] Update this task list as implementation progresses.
