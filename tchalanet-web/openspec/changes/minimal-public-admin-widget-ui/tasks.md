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
- [x] Type `PageModelDoc` directly in `shared/types/pagemodel.types.ts` (replaced the abstract
      hero/text/notice vocabulary); added `PageDynamicPayload`/`WidgetDynamicError`/response types.
- [x] Define the `type → component` registry (`features/pagemodel/widget-registry.ts`, key = backend
      `type` string, e.g. `HeroWidget`).
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
- [x] If a backend contract is missing data or is unsafe/awkward for the UI, stop and update the
      backend slice before inventing frontend-only workarounds — **no gap encountered**; existing
      contracts were sufficient, backend left unchanged.

## 2. Public widget renderer

- [x] Rebuilt renderer in `features/pagemodel/` (`PageModelComponent` + `WidgetHostComponent` +
      light `PageShellComponent` header/footer), retargeted to `PageModelDoc` and the PR1 foundation
      (mono-app `tch-portal`; web-backup libs used as inspiration only, not copied).
- [x] V1 supported widgets: `HeroWidget`, `NewsTickerWidget`, `FeatureGridWidget`, `PlansWidget`
      (+ shell). `PublicDrawResultsWidget`/`CheckTicketWidget`/`TchalaSearchWidget` → fallback.
- [x] Resolve dynamic payload by widget id from `dynamic.widgets[id]`.
- [x] Add unsupported-widget fallback.
- [x] Add invalid-widget fallback for missing id/type.
- [x] Add widget-local error rendering (`dynamic.errors`) + render-failure containment.
- [x] Widgets use validated theme tokens/CSS variables, not hard-coded colors (theme engine in place).
- [x] Missing translations render stable key-derived fallback text (`LabelPipe`/`tchLabel`).
- [x] Public page works without authenticated session (anonymous `GET public.home`).
- [x] No copied mockup markup, CDN deps, remote images, or hard-coded palette.

## 3. SUPER_ADMIN dashboard UI

- [x] Route SUPER_ADMIN dashboard to minimal admin surface (`/app/platform` →
      `SuperAdminDashboardPage`).
- [x] Create tenant form (`TenantProvisioningRequest`: code/name/type/profile/timezone/currency).
- [x] Include `initialAdminEmail` as the first tenant-admin creation field.
- [x] Optional preview step using `/platform/tenant-onboarding/preview`.
- [ ] (W2) Manage tenant admins of a selected tenant via `/admin/identity/users` +
      `X-Tch-Tenant-Override` header. (Tenant list `GET /platform/tenants` also W2.)
- [x] Show success/error state from backend responses.
- [x] No analytics, jobs, flags, audit, release notes, or service health (separate change).

## 4. TENANT_ADMIN dashboard UI

- [x] Route TENANT_ADMIN dashboard to minimal tenant admin surface (`/app/admin` →
      `TenantAdminDashboardPage`).
- [x] Seller onboarding form using `CreateUserRequest` with **`role=CASHIER` + required `outletId`**.
- [x] Show success/error state from backend responses.
- [x] Do not expose tenant id override UI (no tenant field in the form).
- [x] No sales, draws, payout, reconciliation, or limits dashboards.

## 5. Tests / validation

- [x] Foundation + W1 unit tests green — 54 passing (was 33). `nx build`/`lint` green.
- [x] Renderer resolves supported widgets (registry spec + page-model spec).
- [x] Unsupported / invalid / widget-local error fallbacks contained (widget-host spec).
- [x] SUPER_ADMIN create-tenant flow (incl. `initialAdminEmail`) submit + error state (spec).
- [x] TENANT_ADMIN seller onboarding submit; CASHIER without `outletId` blocked (spec).
- [x] Route ownership asserted (`app.routes.spec.ts`); role guards already wired (PR1).
- [x] Run focused Nx tests for touched app/libs.

## 6. Documentation

- [x] Convention docs updated/added: `theme.md`, `settings.md`, `feature-flags.md`, `entitlements.md`,
      `access.md`, `pagemodel.md` (+ `90-web-rules.md` pointer).
- [x] Document widget component mapping near the renderer (`features/pagemodel/README.md`).
- [x] Direct translation and theme-token rules documented in the conventions.
- [x] Update this task list as implementation progresses — W1 delivered and shipped in PR #135
      (branch `codex/page-widget-contract-design`).
