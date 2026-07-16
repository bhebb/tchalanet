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
