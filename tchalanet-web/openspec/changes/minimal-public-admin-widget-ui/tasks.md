# Tasks

## 0. Foundations (PR1 — delivered)

- [x] Port the Material 3 theme pipeline from `web-backup` into `core/theme/scss` (brand tonal
      palettes, `tch-generate-theme` mixin, preset catalog, `runtime-root`/`runtime-vars` deriving
      validated `--tch-*` from `--mat-sys-*`).
- [x] Add generator `tools/generate-theme-registry.mjs` (`npm run theme:generate`) →
      `theme-presets.registry.ts` (12 presets, ids mirror backend `theme_preset.code`).
- [x] Map backend editable theme tokens to validated CSS vars (`theme-token-map.ts`); wire
      `ThemeApi`/`ThemeRepository`/`styles.scss` to the registry.
- [x] Isolated gating seams: `core/feature` (`FeatureFlags`, Unleash-ready), `core/entitlement`
      (`EntitlementsStore`), `core/access` (`AccessService` + `*tchCan` + `can` pipe + `accessGuard`);
      re-point `*tchFeature`/`featureGuard`/`AppRuntimeStore` at the seam.
- [x] Add missing `@angular/animations` dependency (unblocks `nx build`).
- [x] i18n preset keys (fr/en/ht) aligned to backend codes.

## 1. Contract alignment

- [x] Read backend OpenSpec `minimal-public-admin-widget-bff` before implementation.
- [x] Decision: **type the real backend `PageModelDoc`** (meta/theme/shell/content + separate
      `dynamic.widgets[id]`/`errors`). No abstract widget vocabulary, no legacy-mapping registry.
- [ ] Create `pagemodel.contract.ts` typed on `PageModelDoc`; replace `shared/types/pagemodel.types.ts`.
- [ ] Define the `type → component` registry (key = backend `type` string, e.g. `HeroWidget`).
- [x] Direct translation fallback rule confirmed (i18n-first; missing value → key-derived fallback).
- [x] Theme token fallback confirmed (validated `--tch-*` derived from M3 tokens with `:root` fallback).
- [x] Routes for public / SUPER_ADMIN / TENANT_ADMIN confirmed (already wired: `/public`,
      `/app/platform`, `/app/admin` with role guards).
- [x] Confirm backend calls to consume:
  - `GET /api/v1/public/page-models/public.home`
  - `GET /api/v1/platform/page-models`
  - `GET /api/v1/tenant/page-models`
  - `POST /api/v1/platform/tenant-onboarding/preview`
  - `POST /api/v1/platform/tenant-onboarding/provision`
  - `POST /api/v1/admin/identity/users` (+ `GET /api/v1/platform/tenants` for tenant list)
- [ ] If a backend contract is missing data or is unsafe/awkward for the UI, stop and update the
      backend slice before inventing frontend-only workarounds.

## 2. Public widget renderer

- [ ] Salvage `WidgetRendererComponent` + `grid-layout` + static widgets (hero/news/feature/plans) +
      shell/header/footer from `web-backup`, retargeted to `PageModelDoc` and the current foundation.
- [ ] V1 supported widgets: `HeroWidget`, `NewsTickerWidget`, `FeatureGridWidget`, `PlansWidget`
      (+ shell). `PublicDrawResultsWidget`/`CheckTicketWidget`/`TchalaSearchWidget` → fallback this slice.
- [ ] Resolve dynamic payload by widget id from `dynamic.widgets[id]`.
- [ ] Add unsupported-widget fallback.
- [ ] Add invalid-widget fallback for missing id/type.
- [ ] Add widget-local error rendering (`dynamic.errors`).
- [x] Widgets use validated theme tokens/CSS variables, not hard-coded colors (theme engine in place).
- [ ] Ensure missing translations render stable key-derived fallback text.
- [ ] Ensure public page works without authenticated session.
- [ ] No copied mockup markup, CDN deps, remote images, or hard-coded palette.

## 3. SUPER_ADMIN dashboard UI

- [ ] Route SUPER_ADMIN dashboard to minimal admin surface (`/app/platform`).
- [ ] Tenant list (`GET /platform/tenants`, paged) + create tenant form (`TenantProvisioningRequest`).
- [ ] Include `initialAdminEmail` as the first tenant-admin creation field.
- [ ] Optional preview step using `/platform/tenant-onboarding/preview`.
- [ ] (W2) Manage tenant admins of a selected tenant via `/admin/identity/users` +
      `X-Tch-Tenant-Override` header.
- [ ] Show success/error state from backend responses.
- [ ] No analytics, jobs, flags, audit, release notes, or service health (separate change).

## 4. TENANT_ADMIN dashboard UI

- [ ] Route TENANT_ADMIN dashboard to minimal tenant admin surface (`/app/admin`).
- [ ] Seller onboarding form using `CreateUserRequest` with **`role=CASHIER` + required `outletId`**.
- [ ] Show success/error state from backend responses.
- [ ] Do not expose tenant id override UI.
- [ ] No sales, draws, payout, reconciliation, or limits dashboards.

## 5. Tests / validation

- [x] Foundation unit tests green (theme token map, feature/access gating) — 33 passing.
- [ ] Public widget page renders supported widgets.
- [ ] Unsupported widget fallback renders without breaking page.
- [ ] SUPER_ADMIN can submit create tenant flow.
- [ ] SUPER_ADMIN can submit create tenant admin flow.
- [ ] TENANT_ADMIN can submit seller onboarding flow.
- [ ] Route guards prevent wrong roles from accessing each dashboard.
- [ ] Run focused Nx tests for touched app/libs.

## 6. Documentation

- [x] Convention docs updated/added: `theme.md`, `settings.md`, `feature-flags.md`, `entitlements.md`,
      `access.md`, `pagemodel.md` (+ `90-web-rules.md` pointer).
- [ ] Document widget component mapping near the renderer once built.
- [x] Direct translation and theme-token rules documented in the conventions.
- [ ] Update this task list as implementation progresses.
