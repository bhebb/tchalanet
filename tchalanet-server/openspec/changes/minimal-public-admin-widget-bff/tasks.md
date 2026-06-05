# Tasks

## 1. Contract discovery

- [x] Read current PageModel/public runtime contracts before editing implementation.
- [x] Read current tenant creation and user provisioning APIs before adding orchestration.
- [x] Decide whether this slice reuses PageModel runtime directly or adds a narrow BFF facade: reuse existing runtime and existing admin endpoints first.
- [x] During web integration, record any backend gap: **none found** — the existing `PageModelDoc`/
      `PublicPageModelResponse`/`PageDynamicPayload` contracts and admin/onboarding endpoints (all
      wrapped in `ApiResponse<T>`) were sufficient for the W1 web renderer + admin surfaces. Backend
      left unchanged this slice.

## 2. Public page widget payload

- [x] Confirm public page response model: `PublicPageModelResponse(currentLang, langs, pageModel, dynamic)`.
- [x] Confirm endpoint for public page runtime payload: `GET /public/page-models/{logicalId}`.
- [x] Identify the exact widget types present in the seeded `public.home` payload:
      `HeroWidget`, `NewsTickerWidget`, `PublicDrawResultsWidget`, `CheckTicketWidget`,
      `TchalaSearchWidget`, `FeatureGridWidget`, `PlansWidget`
      (`tchalanet-app/src/main/resources/pagemodel/public.home.json`).
- [x] Confirm contained widget errors exist through `PageDynamicPayload.errors`.
- [ ] Modify backend if `public.home` lacks an approved widget source/payload needed by the renderer.

## 3. SUPER_ADMIN minimal dashboard

- [x] Confirm dashboard payload endpoint: `GET /platform/page-models`.
- [x] Confirm create tenant + initial tenant admin flow: `POST /platform/tenant-onboarding/provision`.
- [x] Confirm read-only preview flow: `POST /platform/tenant-onboarding/preview`.
- [ ] Decide whether web needs separate "add tenant admin after tenant exists"; if yes, verify whether `/admin/identity/users` works under selected tenant context.
- [x] Confirm SUPER_ADMIN authorization on tenant provisioning endpoint.
- [ ] Modify backend if the existing provisioning response lacks the minimal confirmation/status fields needed by the UI.

## 4. TENANT_ADMIN minimal dashboard

- [x] Confirm tenant dashboard payload endpoint: `GET /tenant/page-models`.
- [x] Confirm seller onboarding endpoint: `POST /admin/identity/users` with `role = CASHIER` or role chosen by the approved seller model.
- [x] Confirm seller onboarding uses current tenant from request context.
- [x] Confirm request model does not expose tenant id override.
- [ ] Modify backend if seller onboarding needs a narrower action contract than generic user creation.

## 5. Tests

- [ ] Public page payload resolves for anonymous/public access.
- [ ] Unsupported widget returns a contained widget error.
- [ ] SUPER_ADMIN can create tenant.
- [ ] SUPER_ADMIN can create tenant admin for a tenant.
- [ ] TENANT_ADMIN can onboard seller in current tenant.
- [ ] TENANT_ADMIN cannot onboard seller for another tenant.
- [ ] Unauthorized roles cannot access admin actions.

## 6. Documentation

- [x] Document endpoint names in this OpenSpec change.
- [ ] Document widget source names after reading seeded payload/resources.
- [x] Update this task list as implementation progresses — web W1 consumed these contracts as-is
      (PR #135); backend unchanged this slice. Remaining backend test tasks (§5) stay open.
