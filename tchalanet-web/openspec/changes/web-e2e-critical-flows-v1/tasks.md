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

- [ ] **Public baseline** — `/` renders public shell anonymously, no `Authorization`
      header, "Connexion" → `/login`.
- [ ] **Admin real-UI login** — valid creds on `/login` → dispatched to `/app/admin`,
      tenant-admin nav renders.
- [ ] **Super-admin real-UI login** — valid creds → dispatched to `/app/platform`.
- [ ] **Invalid credentials** — inline error, no navigation, button re-enabled.
- [ ] **Guards / redirection** — unauthenticated `/app/**` → `/login`; wrong-role
      → forbidden/redirect (`roleGuard`); `spaceDispatchGuard` honors
      `session.entryRoute`; un-activated TENANT_ADMIN → `/account/activation`.
- [ ] **Super-admin acting within a tenant** — from platform portal, act on a
      tenant (`asTenantAdmin` / `X-Tenant-Id`) → tenant-scoped screen renders +
      active-tenant indicator; exit restores platform scope.
- [ ] **Admin acting on a seller terminal** — `admin-portal/features/seller-terminals`
      → open a terminal → that terminal's context screen renders; cross-tenant
      terminal id → not-found/forbidden.

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

- [ ] Add a `web-e2e` CI target (needs emulator + API + a seeded tenant),
      non-blocking initially like Python E2E.
- [ ] Document the local run command in `apps/web-e2e/README.md`
      (`pnpm exec nx e2e web-e2e`, prerequisites, per-project selection).

## 7. Verify no duplication

- [ ] Apply design §6 checklist to every added spec: no assertion here is a
      re-test of a Unit / Integration / Python-E2E fact.
- [ ] Cross-check against `test-strategy-separation-v1` design §3 (Python E2E
      flows) — the browser suite must not duplicate those journeys' assertions.
