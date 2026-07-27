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
| `public-portal-mobile` | 4301 | anonymous, 390×844 |

Base URLs are env-driven (`PUBLIC_BASE_URL` / `ADMIN_BASE_URL` /
`PLATFORM_BASE_URL`); `playwright.config.cts` serves the three portals via
`nx serve` with `reuseExistingServer`.

`public-portal-mobile` rejoue le portail public dans un viewport étroit
(`isMobile`, `hasTouch`) : c'est le seul projet qui voit la navigation repliée,
les trois autres étant en Desktop Chrome. Ses specs vivent dans `src/mobile/`.

> **Config en `.cts`, pas en `.ts`.** Le fichier est chargé par deux outils aux
> attentes opposées : Playwright le transpile en CommonJS, le graphe Nx le charge
> via le type-stripping natif de Node. En `.ts` avec `import.meta.url`, Node le
> détectait comme ESM et le bundle CJS échouait au chargement
> (`exports is not defined in ES module scope`), rendant **toute la suite**
> inexécutable. `.cts` + `__filename` lève l'ambiguïté pour les deux, et c'est la
> forme que documente `nxE2EPreset` pour un workspace CommonJS.

## Pattern

**One Nx e2e project (`web-e2e`) covers all three apps** — not one project per
app. Each app is a Playwright *project* in `playwright.config.cts` (own base URL,
`testMatch` by `src/<portal>/` folder). Les motifs `testMatch` sont comparés au
chemin **absolu** : les ancrer sur `src/` pour qu'un répertoire parent au nom
malheureux n'attrape pas les specs des autres projets. Shared code lives in
`src/support/`:

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
pnpm e2e:web
make down-firebase-emulator
```

`pnpm e2e:web` sets the default local emulator credentials and seeded ids for
the stubbed suite. Override any `TCH_E2E_*` variable inline when a scenario needs
different data.

- `WEB_E2E_EMULATOR=1` → portals serve with their **`emulator`** configuration
  (`environment.emulator.ts` → `connectAuthEmulator(:9099)`, project
  `demo-tchalanet-local`).
- In emulator mode, Playwright does **not** reuse existing dev servers. This is
  intentional: reusing a stale local server can silently switch the browser back
  to the real Firebase configuration and make valid emulator users look invalid.
- `global-setup.ts` **fails fast** if the emulator isn't up (answers "launch it
  first") and **creates the seeded users** (idempotent).
- Real UI login → Firebase token → guard → **stubbed `/runtime/private`** gives
  the role (SUPER_ADMIN / TENANT_ADMIN) → dispatch. Data (tenants) stubbed too.
- Same identities/provider as the Python E2E (firebase-emulator); `local-jwt` is
  for load (Locust) only.
- Phase 3 also covers username lookup, public → private portal handoff, logout,
  and the support-tenant → admin support mode → return-platform round trip.
- Admin business V1 (`src/admin-portal/business-admin-v1.spec.ts`) covers the
  current admin web UI contract for setup, Maryaj gratis, tenant limits, and
  seller reports. Keep new admin scenarios in that file unless a new feature area
  genuinely needs its own spec.

CI can still run this self-contained variant when no disposable API was
deployed. In the full validation workflow, a successful runtime deploy switches
`web-e2e` to the API-backed variant below.

### Docker API run — disposable E2E API fixture

Use this when the web must exercise the deployed local Docker API instead of
Playwright REST stubs.

```bash
# 1. Infra/API: Firebase emulator must be up before API bootstrap.
cd ../tchalanet-infra
make up-firebase-emulator
make local-product-up

# If the API was already running before the emulator, restart it once so
# FIREBASE_BOOTSTRAP_AUTO_RUN_ON_STARTUP creates linked users.
docker restart tchl-api-dev

# 2. Web runtime profile: REST calls go to Docker API, auth stays emulator.
cd ../tchalanet-web
pnpm runtime:dev-docker-emulator

# 3. Browser E2E against the real Docker API.
TCH_E2E_SUPERADMIN_EMAIL=super_admin@localtest.me TCH_E2E_SUPERADMIN_PASSWORD=Changeme1! \
  TCH_E2E_ADMIN_EMAIL=admin@localtest.me TCH_E2E_ADMIN_PASSWORD=Changeme1! \
  pnpm e2e:web:api
```

What this mode does:

- `WEB_E2E_API=1` serves the portals with `serve:e2e-api`.
- `environment.e2e-api.ts` connects Firebase Auth SDK to `localhost:9099` but
  loads `/assets/config/runtime.<portal>.json`.
- `pnpm runtime:dev-docker-emulator` copies runtime files whose `apiBaseUrl` is
  `https://api.localtest.me/api/v1`.
- `apiStub` is a no-op, so an unmocked API problem fails against the real API.
- `business-admin-v1.spec.ts` is skipped in this mode until the disposable API
  seed exposes deterministic setup/Maryaj/limits/report fixtures. Backend
  business correctness remains covered by the Python E2E suite.
- `global-setup.ts` verifies the seed users can sign in; it does not create them.
  Creating them from Playwright would generate emulator UIDs that are not linked
  to `app_user_external_identity`.

Required API-side truth:

| API setting | Expected local E2E value |
|---|---|
| `TCH_IDENTITY_PROVIDER` | `firebase-emulator` |
| `FIREBASE_AUTH_EMULATOR_HOST` | `firebase-emulator:9099` inside Docker |
| `FIREBASE_PROJECT_ID` | `demo-tchalanet-local` |
| `TCH_IDENTITY_FIREBASE_BOOTSTRAP_USERS` | `superadmin,admin` |
| `FIREBASE_BOOTSTRAP_DEFAULT_USER_PASSWORD` | `Changeme1!` |

Useful checks:

```bash
curl -sk -H 'Host: api.localtest.me' \
  https://127.0.0.1/api/v1/actuator/health

FIREBASE_EMAIL='super_admin@localtest.me' FIREBASE_PASSWORD='Changeme1!' \
  bash ../tchalanet-server/scripts/firebase-id-token.sh
```

If login fails with `EMAIL_NOT_FOUND`, the emulator started after the API.
Restart the API or call the platform identity sync endpoint after authenticating
with an already linked super-admin.

### GitHub workflow handoff — disposable API

In `full-validation.yml`, the runtime deploy is the source of truth for dynamic
coordinates. Web E2E does not hardcode `api.localtest.me`, staging URLs, or a
run-id base path. The deploy job publishes:

| Output | Web usage |
|---|---|
| `api_base_url` | `WEB_E2E_API_BASE_URL` |
| `firebase_emulator_host` | `TCH_FIREBASE_EMULATOR_HOST` for Playwright/global setup |
| `firebase_auth_emulator_url` | `WEB_E2E_FIREBASE_AUTH_EMULATOR_URL` for browser Firebase SDK |

Before Playwright starts the Angular dev servers, CI runs:

```bash
WEB_E2E_API_BASE_URL="$api_base_url" \
WEB_E2E_FIREBASE_AUTH_EMULATOR_URL="$firebase_auth_emulator_url" \
node apps/web-e2e/scripts/write-runtime-config.mjs
```

That script writes `runtime.public-portal.json`, `runtime.admin-portal.json`,
and `runtime.platform-portal.json` on the runner only. The checked-in runtime
profiles remain stable; the unknown API base path generated by the workflow is
passed into the web runtime at test time.

The web portals themselves are served locally by Playwright on
`http://localhost:4301`, `http://localhost:4302`, and `http://localhost:4303`.
The API deploy therefore configures CORS with those localhost origins. A
separate staging smoke can still use `WEB_E2E_EXTERNAL=1`, but that is not the
full-validation path.

## Prerequisites

There are two distinct modes. Do not mix their prerequisites:

| Mode | Auth | Data/API | Use |
|---|---|---|---|
| Local/CI variant A | Firebase Auth emulator | Playwright REST stubs | Default web-e2e: auth, guards, dispatch, shell/navigation |
| Docker API integration | Firebase Auth emulator | Real local Docker API | Browser + disposable API smoke |
| Deployed integration | Real configured auth | Real backend API | Staging/browser integration smoke |

Variant A is self-contained after `make up-firebase-emulator`: it does not need
the backend API, tenant provisioning, or server Python E2E fixtures. The suite
seeds emulator users in `global-setup.ts`; roles and deterministic screen data
come from `support/api-stub.ts`.

For Docker API integration, treat the backend as a fixture: provision it with
the Python E2E stack first when a test needs tenant/seller-terminal data, then
pass the resulting IDs through env. For deployed portal integration, point
`PUBLIC_BASE_URL`, `ADMIN_BASE_URL`, and `PLATFORM_BASE_URL` at the deployed
origins with `WEB_E2E_EXTERNAL=1`.

Portal serving shortcuts live at the workspace root:

```bash
pnpm serve:portals                  # local-ide
pnpm serve:portals:emulator         # local-ide-emulator
pnpm serve:portals:docker           # dev-docker
pnpm serve:portals:docker-emulator  # dev-docker-emulator
pnpm serve:portals:stg              # stg-cloudflare
pnpm serve:portals:prod             # prod-cloudflare
```

Use `--only` when you do not need every portal:

```bash
pnpm serve:admin
pnpm serve:admin:docker-emulator
pnpm serve:portals -- --only=admin,platform
```

## Environment (Phase 1 — auth)

Login runs through the real UI form; credentials and seeded ids come from env so
tests **skip** when not configured (they never hard-fail on a missing fixture):

| Var | Used by |
|---|---|
| `TCH_E2E_ADMIN_EMAIL` / `TCH_E2E_ADMIN_PASSWORD` | admin login/dispatch, seller-terminal |
| `TCH_E2E_SUPERADMIN_EMAIL` / `TCH_E2E_SUPERADMIN_PASSWORD` | platform login/dispatch, tenant |
| `TCH_E2E_TENANT_ID` | super-admin → tenant detail (`stub-tenant-1` in variant A) |
| `TCH_E2E_SELLER_TERMINAL_ID` | admin → seller-terminal overrides (`stub-terminal-1` in variant A) |

## Covered flows

| Phase | Coverage |
|---|---|
| Phase 1 | Public shell, login page, invalid credentials, admin/platform login dispatch, guards, tenant/seller-terminal context URLs |
| Phase 2 | Platform support-tenant screen and start-access dialog opening |
| Phase 3 | Email and username login, public handoff to platform, logout, support access round trip with visible tenant support banner and return |
| Admin business V1 | Setup checklist, Maryaj gratis panels/save request, limits invalid-edit guard, seller report rows/CSV/PDF print root |

## Run

```bash
pnpm e2e:web                                 # all projects, emulator + REST stubs
pnpm e2e:web:admin                           # admin project only
pnpm e2e:web:admin-business                  # admin setup/Maryaj/limits/report scenario
pnpm e2e:web -- src/**/auth-phase1.spec.ts
pnpm e2e:web:api                             # Docker API mode, no Playwright REST stubs
```

> Status: the suite is wired into `full-validation.yml` as non-blocking while it
> stabilizes. Treat failures as actionable, but do not assume the nightly is gated
> by web-e2e until `continue-on-error` is removed.
