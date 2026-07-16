# Tasks — web-e2e-critical-flows-v1

Order: harness first, then flows per portal, then CI wiring. Check off in real
time (openspec-workflow rule). A flow's task may add a `data-testid` to portal
source — that is the only production-source edit this change allows.

## 1. Harness (`specs/web-e2e-harness`)

- [ ] Add role auth fixtures: programmatic firebase-emulator sign-in per role,
      persisted `storageState` (`super_admin` / `admin` / `cashier`).
- [ ] Document the seeded-tenant bring-up assumption in `apps/web-e2e/README.md`
      (reuse the Python E2E emulator stack; web does not provision).
- [ ] Establish the `data-testid` selector convention (lint/helper), no text/CSS
      coupling.
- [ ] Confirm base-URL wiring for the three projects is env-driven (already in
      `playwright.config.ts`); add per-project `storageState`.

## 1b. Phase 1 — Authentication, role dispatch & context override (DO FIRST)

> Emphasis for the first slice (`specs/web-e2e-auth-phase1`): the login / session
> / redirection backbone across the 3 portals, plus the two context-override
> flows. Auth-only — config/general screens are **Phase 2** (§2–§4 below).
>
> **Landed** (`src/support/auth.ts` + `src/{public,admin,platform}-portal/auth-phase1.spec.ts`,
> `data-testid`s on the shared login form, `README.md`). **Not yet executed** —
> needs the emulator + API + seeded tenant stack and a `web-e2e` CI target (§6);
> credential/id-dependent tests `test.skip` until env is provided.

- [x] **Public baseline** — `/` reachable anonymously; login page + form render.
- [x] **Admin real-UI login** — valid creds on `/login` → dispatched to `/app/admin`
      (or `/account/activation` if un-activated).
- [x] **Super-admin real-UI login** — valid creds → dispatched into the platform space.
- [x] **Invalid credentials** — inline `login-error`, stays on `/login`.
- [x] **Guards / redirection** — unauthenticated `/app/{admin,platform}` → `/login`.
      (`spaceDispatchGuard` entryRoute + activation redirect covered via dispatch asserts.)
- [x] **Super-admin acting within a tenant** — open `/app/platform/tenants/:tenantId`
      (tenant-scoped screen via `asTenantAdmin` / `X-Tenant-Id`).
- [x] **Admin acting on a seller terminal** — open
      `/app/admin/seller-terminals/:id/overrides`.
- [ ] **Wrong-role cross-app block** — `test.fixme` pending confirmation of the
      `location.assign` landing (open question).

## 1c. Phase 2 — Platform support mode (`specs/web-e2e-support-tenant-phase2`)

> Super-admin support-tenant flow. Reuses the Phase 1 harness (LoginPage
> fixture). **Landed** (`support/pages/support-tenant.page.ts`,
> `src/platform-portal/support-tenant.spec.ts`, `data-testid`s on the
> support-tenant page + start-access dialog). Auth/data tests `test.skip` until
> creds/seeded tenants are provided.

- [x] `/app/platform/support-tenant` guarded → `/login` when unauthenticated.
- [x] Super-admin renders the support-tenant screen (table or empty-state).
- [x] "Mode support admin" row action opens `tch-start-tenant-admin-access-dialog`.
- [ ] Confirm start-access → session starts (deeper flow; needs a seeded tenant +
      backend, left for a follow-up).

## 2. Public portal (`specs/public-portal-e2e`)

- [ ] Keep the existing shell smoke.
- [ ] Public runtime navigation renders resolved page-model content; no private
      provider source leaks into the DOM.
- [ ] Public ticket verification UI shows result / not-found state.
- [ ] `/login` renders; invalid credentials → inline error, no navigation.

## 3. Admin portal (`specs/admin-portal-e2e`)

- [ ] Admin login → space dispatch lands `/app/admin/dashboard`.
- [ ] First-login activation guard routes un-activated TENANT_ADMIN to
      `/account/activation`.
- [ ] `/app/admin/setup` renders setup/readiness sections, navigable.
- [ ] `/app/admin/limits` renders policy list; invalid edit → inline validation.
- [ ] Cashier blocked from tenant-admin-only route → forbidden/redirect.
- [ ] POS sale happy path (UI): build ticket → visible success/receipt feedback →
      form resets.
- [ ] POS rejected sale surfaces rejection toast/state in the POS UI.

## 4. Platform portal (`specs/platform-portal-e2e`)

- [ ] Super-admin login → dispatched to `/app/platform/dashboard`.
- [ ] Non-super-admin blocked from `/app/platform/*` → forbidden/redirect.
- [ ] `/app/platform/tenants` renders table + pagination (empty-state if none).
- [ ] `/app/platform/tenants/onboarding` renders; missing required field →
      inline validation, no POST.

## 5. Cross-origin handoff (in `admin-portal-e2e` or shared)

- [ ] `/login/handoff` lands the target portal's authenticated shell, no
      re-login loop.

## 6. CI wiring

- [x] `web-e2e` target runs in `.github/workflows/full-validation.yml` (nightly +
      dispatch). Made **non-blocking** (`continue-on-error`) + Playwright report
      artifact.
- [x] `playwright.config.ts`: `WEB_E2E_EXTERNAL=1` skips the local `nx serve` and
      targets deployed portals (PUBLIC/ADMIN/PLATFORM_BASE_URL) — lets CI point at
      staging with the firebase-emulator + seeded tenant.
- [ ] Provision the deployed-target env in CI (repo vars/secrets:
      `WEB_E2E_*_BASE_URL`, reuse `TCH_*_ADMIN_*` creds, `TCH_E2E_TENANT_ID` /
      `TCH_E2E_SELLER_TERMINAL_ID`) so the auth/context flows actually execute
      instead of skipping. Then keep observational until green, gate later.
- [x] Document the local run command in `apps/web-e2e/README.md`
      (`pnpm exec nx e2e web-e2e`, prerequisites, per-project selection).

## 7. Verify no duplication

- [ ] Apply design §6 checklist to every added spec: no assertion here is a
      re-test of a Unit / Integration / Python-E2E fact.
- [ ] Cross-check against `test-strategy-separation-v1` design §3 (Python E2E
      flows) — the browser suite must not duplicate those journeys' assertions.
