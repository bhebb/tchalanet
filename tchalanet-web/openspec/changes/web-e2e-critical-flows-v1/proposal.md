# OpenSpec Change — Web E2E Critical Flows V1

## Status

Proposed — 2026-07-13

## Why

`apps/web-e2e` (Playwright) exists but only carries one **shell smoke** per
portal (public / admin / platform) — it proves the app boots, nothing else. The
cross-project contract `openspec/changes/test-strategy-separation-v1` explicitly
defers the browser layer to a dedicated web change (design §4, tasks §6). This is
that change.

Web is **adjacent to the backend pyramid, not inside it**. The backend already
owns business correctness at the right level: pure rules in Java Unit, wiring in
Java Integration, whole-stack journeys in Python E2E. If web e2e re-asserts limit
math, idempotency, or ProblemDetail shapes it becomes a slow, redundant, flaky
copy of the Python suite — the exact overlap the parent contract forbids.

So this change specifies what the browser layer **uniquely** owns: things only a
real browser can prove — that the right screen renders for the right role, that
route guards redirect, that a form surfaces its validation/feedback, that the
cross-origin portal handoff lands the user in the correct space. UI-observable
behavior only.

## Decision (locked)

- **Scope = UI-observable behavior**: rendering, navigation, route guards,
  role/space dispatch, form feedback, empty/error states, cross-origin handoff.
- **Not scope = backend business rules**: no assertion on limit outcomes,
  payout math, idempotency-store, ProblemDetail bodies, RLS. Those stay in
  Unit / Integration / Python E2E.
- **Auth = firebase-emulator**, same identities as Python E2E
  (`super_admin` / `admin` / `cashier`). Default path: programmatic sign-in →
  reused `storageState` per role. A small number of tests exercise the **real UI
  login + portal handoff** on purpose.
- **Three Playwright projects** stay as-is: `public-portal` (:4301),
  `admin-portal` (:4302), `platform-portal` (:4303).
- **Backend is a fixture, not a subject**: the emulator + a seeded/provisioned
  tenant are brought up; the web suite treats API responses as given.

## What Changes

- A harness contract for `apps/web-e2e`: role auth fixtures, base-URL wiring,
  seeded-tenant assumption, a stable `data-testid` selector convention, and the
  "UI-observable only" boundary (see `specs/web-e2e-harness`).
- Critical-flow specs per portal: public, admin (tenant-admin + cashier POS),
  platform (super-admin). See the three `*-portal-e2e` specs.
- `tasks.md`: implement the fixtures, then the flows, then wire the CI target.

## Impact

- New/expanded tests under `tchalanet-web/apps/web-e2e/src/**` only.
- No production runtime change; no change to portal source.
- References and is referenced by
  `openspec/changes/test-strategy-separation-v1` (the parent contract).
- CI gains a `web-e2e` target (non-blocking initially, like the Python E2E).

## Non-goals

- No backend business assertions (owned by the pyramid).
- No visual-regression / pixel snapshots (separate concern if ever wanted).
- No performance/load in the browser (Locust owns capacity/latency).
- No mobile (Flutter) coverage — different runtime, different change.
- No coverage gate in V1 (tracked, enabled later).

## Deepened — 2026-07-15 — Phase 1 (auth & context)

The first implementation slice is carved out as **Phase 1 — authentication, role
dispatch and context override** (`specs/web-e2e-auth-phase1`, `tasks.md §1b`),
sequenced before the per-screen config/general flows:

1. Public anonymous baseline + login entry.
2. Real-UI login for `admin` and `super_admin` → role-based dispatch
   (`/app/admin`, `/app/platform`).
3. Guards & redirection (`authGuard`, `roleGuard`, `spaceDispatchGuard` via
   `session.entryRoute`; activation redirect).
4. **Super-admin acting within a tenant** (platform → `asTenantAdmin` /
   `X-Tenant-Id` override; tenant scope shown, then restored).
5. **Admin acting on a seller terminal** (`admin-portal/features/seller-terminals`).

Flows 4 and 5 are **new** vs the original per-portal task list, which covered
login/dispatch but not the tenant-impersonation and seller-terminal context
overrides. Config/general screens (setup, limits editing, POS sale, reporting)
remain Phase 2. Grounding: `libs/core/auth` (`LoginPage`, `AuthRedirectService`,
`auth.guard.ts`), `libs/api` (`TchBackendClient.asTenantAdmin`),
`apps/admin-portal/src/app/features/seller-terminals`.

## Deepened — 2026-07-15 — Phase 2 (platform support mode)

Second slice (`specs/web-e2e-support-tenant-phase2`, `tasks.md §1c`): the
super-admin **support-tenant** flow — open `/app/platform/support-tenant`, render
the tenant list/empty-state, and open the start-tenant-admin-access dialog on a
tenant row. Reuses the Phase 1 harness (LoginPage fixture) and adds a
`SupportTenantPage` page object. Grounding:
`apps/platform-portal/src/app/features/support-tenant` and the shared
`start-tenant-admin-access-dialog`. Completing the start-access session (backend
side effect) is left for a follow-up.
