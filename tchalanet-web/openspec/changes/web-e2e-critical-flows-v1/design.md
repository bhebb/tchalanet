# Design — Web E2E Critical Flows

Concrete allocation for the browser layer. One list per portal, plus the harness
contract. Built so the web suite proves only what a browser uniquely proves and
never re-tests a backend rule.

## 0. Boundary — what web e2e MUST and MUST NOT assert

| MUST assert (UI-observable) | MUST NOT assert (owned below) |
|---|---|
| Correct screen renders for a given role/route | Limit outcome / payout math (Unit) |
| Route guards redirect (auth / role / space dispatch) | Idempotency-store behavior (Integration) |
| Cross-origin portal handoff lands the right space | ProblemDetail JSON shape (Integration) |
| Form validation + submit feedback (toast / inline / disabled) | RLS / cross-tenant leak at API level (Integration/Python E2E) |
| Empty / loading / error states of a page | The sell/limit/promo permutation matrix (Unit) |
| A ticket sale produces a visible receipt/feedback in the POS UI | Whether the receipt math is correct (Unit/Integration) |

Rule of thumb: **assert what the user sees, not what the server computed.** If an
assertion would still hold with the API stubbed, it belongs here; if it needs the
real business result to be meaningful, it belongs in Python E2E.

## 1. Harness (owned by `web-e2e-harness`)

- **Auth fixtures** — `asRole('public' | 'admin' | 'cashier' | 'superAdmin')`.
  Default: programmatic firebase-emulator sign-in once per role → persisted
  `storageState`, injected per project. Emulator identities mirror the Python
  E2E (`super_admin` / `admin` / `cashier`, project `demo-tchalanet-local`).
- **Seeded tenant** — the suite assumes one provisioned tenant (same bring-up as
  Python E2E: `make up-firebase-emulator` + API on emulator). The web suite does
  **not** provision; it consumes.
- **Selectors** — stable `data-testid` convention; no text/CSS-class coupling.
  Missing testids on a critical control are added to portal source as part of the
  flow's task (the only source change this change permits).
- **Base URLs** — unchanged: public :4301, admin :4302, platform :4303, driven by
  `PUBLIC_/ADMIN_/PLATFORM_BASE_URL` (already in `playwright.config.ts`).
- **No business assertion helper** — the harness offers navigation + auth + DOM
  helpers only; it deliberately ships no "assert limit blocked" style helper.

## 2. Public portal (anonymous) — `public-portal-e2e`

Base :4301. No auth.

- **Public shell renders** — home loads, shell visible *(exists as smoke; keep)*.
- **Public runtime navigation** — home → a public page-model route renders its
  resolved content (no private provider source leaks into the DOM).
- **Public ticket verification (UI)** — enter a code → a result/"not found" state
  is shown (assert the *state shown*, not the verification math).
- **Login entry from public** — `/login` renders; invalid credentials surface an
  inline error; no navigation on failure.

## 3. Admin portal (tenant-admin + cashier) — `admin-portal-e2e`

Base :4302. Roles: `admin` (TENANT_ADMIN), `cashier`.

Tenant-admin:
- **Login → space dispatch** — admin signs in → `spaceDispatchGuard` lands on
  `/app/admin/dashboard` (not platform, not forbidden).
- **First-login activation guard** — an un-activated TENANT_ADMIN hitting an app
  route is routed to `/account/activation`; a completed one is not.
- **Setup console renders** — `/app/admin/setup` shows the setup/readiness
  sections (assert sections present + navigable, not readiness computation).
- **Limits screen loads + form feedback** — `/app/admin/limits` renders the
  policy list; opening the editor and submitting an invalid value shows inline
  validation (assert the *feedback*, not the limit engine result).
- **Role isolation** — cashier signing into the admin portal cannot reach a
  tenant-admin-only route → `ForbiddenPage` / redirect (UI guard, not API 403).

Cashier POS:
- **POS sale happy path (UI)** — cashier opens `/app/admin/pos/sale`, builds a
  ticket, confirms → a **success/receipt feedback is visible** and the form
  resets for the next sale. (Correctness of the receipt is Unit/Integration.)
- **POS rejected feedback (UI)** — a sale the API rejects surfaces the rejection
  toast/state in the POS UI (assert the UI reaction to a rejection, not the rule
  that caused it).

## 4. Platform portal (super-admin) — `platform-portal-e2e`

Base :4303. Role: `superAdmin`.

- **Login → platform space** — super-admin signs in → dispatched to
  `/app/platform/dashboard`.
- **Role guard** — a non-super-admin identity is blocked from `/app/platform/*`
  → `ForbiddenPage` / redirect.
- **Tenants list renders** — `/app/platform/tenants` shows the tenants table
  (list + pagination controls present; empty-state if none).
- **Tenant onboarding form** — `/app/platform/tenants/onboarding` renders; submit
  with a missing required field shows inline validation and does **not** POST
  (assert the guard/feedback, provisioning correctness is Python E2E).

## 5. Cross-origin portal handoff (cross-cutting)

- **Handoff lands the right space** — signing in on one origin and following
  `/login/handoff` results in the target portal's authenticated shell, not a
  re-login loop. This is the one flow that is *only* provable across real
  browser origins — its natural home is here, not the Python suite.

## 6. Anti-duplication checklist (apply on every web-e2e PR)

1. Would this assertion still pass with the API response stubbed? → it's
   UI-observable, keep it here.
2. Does it need the real business result to mean anything? → move it to Python
   E2E / Integration / Unit.
3. Is it enumerating a matrix (stakes / limits / promo permutations)? → Unit.
4. Is it re-checking a ProblemDetail body or idempotency replay? → Integration.
