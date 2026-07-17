# web-e2e — Playwright critical flows

Browser layer of the test pyramid (adjacent to the backend). **UI-observable
behavior only** — rendering, navigation, route guards, role/space dispatch,
context override. No backend business assertions (owned by Unit / Integration /
Python E2E). See `openspec/changes/web-e2e-critical-flows-v1`.

## Projects

| Project | Port | Space |
|---|---|---|
| `public-portal` | 4301 | anonymous |
| `admin-portal` | 4302 | TENANT_ADMIN |
| `platform-portal` | 4303 | SUPER_ADMIN |

Base URLs are env-driven (`PUBLIC_BASE_URL` / `ADMIN_BASE_URL` /
`PLATFORM_BASE_URL`); `playwright.config.ts` serves the three portals via
`nx serve` with `reuseExistingServer`.

## Pattern

**One Nx e2e project (`web-e2e`) covers all three apps** — not one project per
app. Each app is a Playwright *project* in `playwright.config.ts` (own base URL,
`testMatch` by `src/<portal>/` folder). Shared code lives in `src/support/`:

- `support/pages/` — **Page Objects** (`LoginPage`, …): selectors + actions for a
  screen, reused across the three portals (the login screen is the same
  `tch-login-page` component in every app).
- `support/fixtures.ts` — Playwright `test.extend` injecting the page objects
  (`loginPage`). **Specs import `test` / `expect` from `../support/fixtures`**,
  not from `@playwright/test`.
- `support/env.ts` — env-driven credentials / seeded ids.

Selectors use `data-testid` (Playwright `getByTestId`) — no text/CSS coupling.

### API stub / mocks

`support/api-stub.ts` (`ApiStub`) intercepts backend REST (`/api/v1/**`) with
Playwright `page.route`, exposed via the **`apiStub`** fixture — list it in a
test's args to run backend-free with deterministic data
(`await apiStub.tenants([...])`, `apiStub.privateBootstrap(...)`).

**Limit**: auth is decided by the Firebase Auth SDK in the browser, not a REST
call, so the stub **cannot fake a session**. Pure stubs cover the
**unauthenticated** surface (public shell, login page, guard redirects);
authenticated flows still need the firebase-emulator for the session, with the
REST stubs making the data deterministic (hybrid).

### Emulator run — authenticated flows (variant A)

Everything self-contained; **no backend API** (REST is stubbed):

```bash
make up-firebase-emulator                       # infra, Docker, :9099
WEB_E2E_EMULATOR=1 \
  TCH_E2E_SUPERADMIN_EMAIL=super_admin@e2e.local TCH_E2E_SUPERADMIN_PASSWORD=e2e-password-123 \
  TCH_E2E_ADMIN_EMAIL=admin@e2e.local TCH_E2E_ADMIN_PASSWORD=e2e-password-123 \
  pnpm exec nx e2e web-e2e
make down-firebase-emulator
```

- `WEB_E2E_EMULATOR=1` → portals serve with their **`emulator`** configuration
  (`environment.emulator.ts` → `connectAuthEmulator(:9099)`, project
  `demo-tchalanet-local`).
- `global-setup.ts` **fails fast** if the emulator isn't up (answers "launch it
  first") and **creates the seeded users** (idempotent).
- Real UI login → Firebase token → guard → **stubbed `/runtime/private`** gives
  the role (SUPER_ADMIN / TENANT_ADMIN) → dispatch. Data (tenants) stubbed too.
- Same identities/provider as the Python E2E (firebase-emulator); `local-jwt` is
  for load (Locust) only.

CI runs this on the runner (`full-validation.yml` → `web-e2e`, non-blocking).

## Prerequisites

The suite treats the backend as a **fixture** — it does not provision. Bring up
the firebase-emulator + API + a seeded/provisioned tenant (reuse the Python E2E
stack), then point the portals at it.

## Environment (Phase 1 — auth)

Login runs through the real UI form; credentials and seeded ids come from env so
tests **skip** when not configured (they never hard-fail on a missing fixture):

| Var | Used by |
|---|---|
| `TCH_E2E_ADMIN_EMAIL` / `TCH_E2E_ADMIN_PASSWORD` | admin login/dispatch, seller-terminal |
| `TCH_E2E_SUPERADMIN_EMAIL` / `TCH_E2E_SUPERADMIN_PASSWORD` | platform login/dispatch, tenant |
| `TCH_E2E_TENANT_ID` | super-admin → tenant detail |
| `TCH_E2E_SELLER_TERMINAL_ID` | admin → seller-terminal overrides |

## Run

```bash
pnpm exec nx e2e web-e2e                      # all projects
pnpm exec nx e2e web-e2e --project=admin-portal
pnpm exec nx e2e web-e2e -- src/**/auth-phase1.spec.ts
```

> Status: Phase 1 specs landed but **not yet executed in CI** (no `web-e2e` CI
> target yet — see `web-e2e-critical-flows-v1` tasks §6). Run locally against the
> emulator stack to validate.
