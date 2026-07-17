# Tchalanet E2E — Business Runtime

Reproducible end-to-end tests for the Tchalanet platform. They drive the **real** stack
(API + Keycloak + Postgres + Redis + Traefik + edge-service) over HTTPS, exactly as a
client would. No mocks.

> **Agents & first-timers: read this whole file before running anything.** The two things
> that waste the most time are (1) the Keycloak `keycloak-init` cache gotcha and (2)
> rebuilding the API image. Both are solved below — don't rediscover them.
>
> ⚠️ **Sections 5–7 below are legacy (Keycloak + outlet/terminal/seller).** The current
> model is described in *Current status* immediately below — read that first.

---

## Current status & progress — 2026-07-17

**Domain model (current).** The seller actor is **`SellerTerminal`** (`core.sellerterminal`).
The old `outlet` / `terminal` / `seller` trio is **removed** — ignore those flows/fixtures.
Auth is **Firebase only**. Server E2E uses `firebase-emulator` locally. Local
IDE uses real Firebase by default, and staging/prod use real Firebase. **Keycloak
is decommissioned** (§5 kept only for legacy targets). `TCH_OUTLET_ID` /
`TCH_TERMINAL_ID` no longer apply.

**Auth environment rule.**

| Runtime | API identity provider | Harness auth provider | Notes |
|---------|-----------------------|-----------------------|-------|
| Server E2E local Docker dev | `firebase-emulator` | `firebase-emulator` | Deterministic identities, bootstrap users, disposable data |
| Local load/perf with Locust | `local-perf` or `local-jwt` | `local-perf` or `local-jwt` | Avoids loading Firebase Auth Emulator; uses signed local JWTs for seeded actors |
| Local IDE | `firebase` | manual/dev client | Real Firebase by default; use emulator only by explicit developer override |
| Staging | `firebase` | `firebase` for live smoke | No `FIREBASE_AUTH_EMULATOR_HOST` |
| Production | `firebase` | no destructive E2E | No emulator, no test provisioning |

**Scenario source of truth.** The canonical server E2E entry point is
[`docs/business-day-scenarios.md`](docs/business-day-scenarios.md). There is one
current scenario: business-day. Do not add scenario matrices in this README or
in `loadtest/README.md`; add new scenarios to that file first. This README
documents stack setup, auth, env vars, and run commands only.

**Auth providers for tests (`TCH_E2E_AUTH_PROVIDER`).**

| Provider | Signs | Can provision? | Use for |
|---|---|---|---|
| `firebase` | ID tokens via real Firebase password sign-in | ✅ via API | nightly live E2E against staging/prod-like API |
| `firebase-emulator` | ID tokens via the running emulator | ✅ yes | local create seller-terminal, sell, maryaj, limits |
| `local-jwt` / `local-perf` | HS256 for seeded `super_admin`/`admin`/`cashier` | ❌ read/auth only | read endpoints, perf/load, RLS/isolation |
| `keycloak` | password grant | (legacy) | legacy targets only |

Creating a seller-terminal is supported by Firebase-backed APIs. Tests that need to mint
arbitrary dynamic identities from the harness itself, such as `full_flow`, still require
**`firebase-emulator`**. Under `local-jwt` those tests skip cleanly; read endpoints
(list/summary) still run.

**Validated locally after the migration cleanup on 2026-07-17.** `pytest -m L0`
passed, the reduced business-day path passed with `alpha,delta` and one draw,
and the BetOptions support check passed. See the canonical entry point for what
the business-day scenario means and which checks are isolated.

**How the firebase-emulator path works (key facts).**
- Bring it up: `make up-firebase-emulator` (`:9099`, project `demo-tchalanet-local`), then run
  the Docker dev API with `TCH_IDENTITY_PROVIDER=firebase-emulator`,
  `FIREBASE_AUTH_EMULATOR_HOST=firebase-emulator:9099`,
  `FIREBASE_PROJECT_ID=demo-tchalanet-local`, bootstrap on, and
  `TCH_IDENTITY_FIREBASE_BOOTSTRAP_USERS=superadmin,admin` (bootstrap keys on
  **username**; its built-in default is emails and matches nothing).
- Tokens are **unsigned** (`alg=none`), `iss=https://securetoken.google.com/<projectId>`,
  `aud=<projectId>`. `sub` must equal the FIREBASE external subject.
- Seeded users provision with firebase uid = app_user id (deterministic). A **provisioned tenant
  admin** gets a random emulator uid → look it up with `uid_for_email`. A **seller-terminal**'s
  subject is its `sellerTerminalId` (returned by create).
- **No terminal binding anymore.** Drive POS as the admin acting-as-terminal: send
  `X-Tch-Act-As-Terminal: <sellerTerminalId>` and put `sellerTerminalId` in the preview/sell
  body. (`X-Tch-Client-Type: POS` selects the seller-terminal identity resolver for a genuine
  seller token.)

**Pending / deferred.** Legacy Keycloak/outlet/cashier documentation below is
kept only for old targets and should not be used for new SellerTerminal work.
The next load-testing step is a Locust business-day user that reuses the
canonical `tch_e2e.business_day` builders instead of defining a second scenario.

---

## 0. TL;DR

```bash
# 1. Bring the stack up (from tchalanet-infra/)
cd tchalanet-infra
make local-product-up            # Traefik + Postgres + Redis + API + edge-service

# 2. Sanity: stack reachable
curl -sk https://api.localtest.me/api/v1/actuator/health      # {"status":"UP"}

# 3. Run the tests (from tchalanet-server/testing/e2e/)
cd ../tchalanet-server/testing/e2e
source .venv/bin/activate
bash scripts_agent_run.sh agent
```

If a Firebase-backed test returns `external_identity.not_linked`, verify the API
was started with Firebase bootstrap enabled and
`TCH_IDENTITY_FIREBASE_BOOTSTRAP_USERS=superadmin,admin`.

---

## 1. What runs where (entry points)

| Layer | URL (dev) | Notes |
|---|---|---|
| API | `https://api.localtest.me/api/v1` | Traefik TLS; HTTP→HTTPS 301. Context path `/api/v1` is **auto-added** by the servlet — controllers must NOT repeat it. |
| Firebase Auth Emulator | `http://127.0.0.1:9099` | Local auth provider for dynamic tenant/admin/seller-terminal E2E. |
| Keycloak | `https://auth.localtest.me/realms/tchalanet` | Legacy targets only. Do not use for new SellerTerminal scenarios. |
| edge-service | `http://edge-service:3000` (in-cluster) | Slack/email relay. API reaches it by Docker DNS name, not localhost. |
| Postgres | `tchl-postgres-dev` | App DB `tchalanet_db` (user `app_user`). |

Test code lives in `tchalanet-server/testing/e2e/`:

```
tch_e2e/        harness: config, auth, client, api_response, scenario_world, assertions,
                data_factory, ticket_matrix, concurrency
flows/          high-level helpers; legacy outlet/terminal/seller helpers are not scenario truth
prereqs/        idempotent setup helpers (draws, app_user, session)
fixtures/       pos_context.py (fully-onboarded POS context fixture)
tests/          public/ auth_context/ onboarding/ dashboard/ overview/
                cashier_pos/ business_critical/ multitenant/ concurrency/
conftest.py     session fixtures (tokens, clients, world)
pytest.ini      markers
```

---

## 2. Bring up the stack (Makefile)

All `make` targets run from **`tchalanet-infra/`**. Project name is `tch-<ENV>` (default
`tch-dev`); containers are `tchl-*-dev`.

| Target | What it gives you |
|---|---|
| `make local-ide-up` | P0: Traefik + Postgres (API runs in your IDE) |
| `make local-ide-up-redis` | P0 + Redis |
| `make local-api-up` | P0 + Redis + **API in Docker** |
| `make local-product-up` | **Full stack**: API + edge-service + web ← use this for E2E |
| `make up-edge` / `make down-edge` | edge-service only |
| `make local-api-smoke` | health check (Redis + API) |
| `make ps` | list running containers |
| `make logs-api` / `make logs-<svc>` | tail a service's logs |
| `make local-product-down` | tear the full stack down |

Ordering is handled by `depends_on`: Postgres/Redis/Firebase emulator are made
available before the API. A cold bring-up is correct and reproducible on any
machine when the dev env contains the Firebase bootstrap settings above.

> The user-requested manual order — `make up` (P0), then `make up-edge`, then `make local-api-up` —
> works too; `local-product-up` just bundles them.

---

## 3. Configuration: env vars & URLs

The harness loads env from the **first** of these that exists (via `tch_e2e/config.py`):

1. `tchalanet-server/scripts/.env.local`  ← canonical for local dev
2. `tchalanet-server/testing/e2e/.env.local`  ← optional override

Copy `.env.example` to one of those and fill in passwords. Key vars:

| Var | Dev value | Purpose |
|---|---|---|
| `TCH_BASE_URL` | `https://api.localtest.me/api/v1` | API root |
| `TCH_E2E_VERIFY_SSL` | `false` | accept the local mkcert cert |
| `TCH_E2E_AUTH_PROVIDER` | `firebase-emulator` | `firebase-emulator`, `firebase`, `local-jwt`, `local-perf`; `keycloak` is legacy |
| `TCH_FIREBASE_PROJECT_ID` | `demo-tchalanet-local` | Firebase emulator project id |
| `TCH_SUPER_ADMIN_USERNAME` / `_PASSWORD` | `super_admin` / `Changeme1!` | platform role |
| `TCH_TENANT_ADMIN_USERNAME` / `_PASSWORD` | `admin` / `Changeme1!` | `/admin/*` endpoints |
| `TCH_E2E_HOST_HEADER` | `api.localtest.me` when using `https://127.0.0.1/api/v1` | Optional Host override for Traefik when local DNS is unavailable |
| `TCH_TEST_SLACK_CHANNEL_KEY` | `delivery` | enables the POS "send ticket" step (§6) |
| `TCH_TENANT_2_*` | *(unset)* | Enables focused multitenant/concurrency checks when using seeded tenants |

Keycloak variables are legacy and documented in §5 only for old targets.

Do not switch the E2E runner to real Firebase for local destructive scenarios.
The canonical runner intentionally exports `TCH_E2E_AUTH_PROVIDER=firebase-emulator`.

For Firebase-independent E2E/performance validation, including Locust, start the API with
`TCH_IDENTITY_PROVIDER=local-jwt` or `local-perf`, configure the same
`TCH_LOCAL_JWT_ISSUER`/`TCH_LOCAL_JWT_SECRET` in the harness, and set
`TCH_E2E_AUTH_PROVIDER` accordingly. The harness signs tokens only for the seeded
`super_admin`, `admin`, and `cashier`. Their token roles are routing hints; the API replaces them
with database-owned roles and permissions before executing handlers. The existing multitenant L3
suite then exercises the normal context, permission, pooled-connection, and PostgreSQL RLS path.

Do not run Locust against Firebase Auth Emulator. The emulator is useful for
deterministic provisioning and destructive server E2E, but it is not the auth
surface we want to benchmark under load.

For targeted read-model/RLS debugging after the canonical runner has passed:

```bash
export TCH_E2E_AUTH_PROVIDER=local-perf
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters
python -m pytest tests/auth_context tests/multitenant/test_tenant_isolation.py -m "L2 or L3"
```

---

## 4. Running tests

From `tchalanet-server/testing/e2e/` with the venv active (`source .venv/bin/activate`).

### Test levels (markers)

| Marker | Scope | When |
|---|---|---|
| `L0` | boot smoke (API/auth/public basics) | every run |
| `L1` | daily smoke (main happy paths) | daily |
| `L2` | business critical (POS/sales/limits/promotions/idempotency) | nightly / pre-merge |
| `L3` | concurrency correctness (small parallel races) | on demand |
| `public` `cashier_pos` `onboarding` `auth_context` `ticket_sizes` `slow` | topical | as needed |

### Runner

```bash
bash scripts_agent_run.sh agent              # default agent check
bash scripts_agent_run.sh smoke              # L0 only
bash scripts_agent_run.sh business-day       # reduced canonical scenario
bash scripts_agent_run.sh full-business-day  # full canonical scenario
bash scripts_agent_run.sh bet-options        # support check
```

Tests that need an unconfigured prerequisite **skip** (not fail) — e.g. Tenant B tests skip
with a clear `UserWarning` until `TCH_TENANT_2_*` is set.

Direct `pytest` commands are for debugging a failed check after the runner has
identified the failing area. Do not add direct pytest recipes here for new
business scenarios; add a named runner mode first.

### Canonical runner details

For scenario intent, expected totals, and no-duplicate rules, read
[`docs/business-day-scenarios.md`](docs/business-day-scenarios.md). Agents should
use the runner instead of assembling pytest commands manually:

```bash
bash scripts_agent_run.sh agent

bash scripts_agent_run.sh smoke
bash scripts_agent_run.sh business-day
bash scripts_agent_run.sh bet-options

# Isolated availability gates. Do not run in parallel with other E2E jobs.
TCH_E2E_ALLOW_CATALOG_MUTATION=true \
bash scripts_agent_run.sh availability-gates
```

> Tip for sandboxes with a tiny `/tmp`: prefix with `CLAUDE_CODE_TMPDIR=/tmp/tch-e2e`.

---

## 5. Keycloak: testing & the `keycloak-init` cache gotcha

### Quick auth check

```bash
# password grant — public client, NO secret
curl -sk -X POST https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token \
  -d grant_type=password -d client_id=tchalanet-swagger -d scope=openid \
  -d username=cashier -d password='Changeme1!'
```

```bash
# admin view — master admin token, then count users in the tchalanet realm
ADM=$(curl -sk -X POST https://auth.localtest.me/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
curl -sk -H "Authorization: Bearer $ADM" https://auth.localtest.me/admin/realms/tchalanet/users/count
# expected: 8
```

### ⚠️ The gotcha (this is what trips agents)

`keycloak-init` is a **one-shot** container: it imports the realm into Postgres
(`import --override true`) and exits. The long-running Keycloak server runs with
`--cache=local` (infinispan). **A cold start is fine** because the server boots *after* the
import. But if `keycloak-init` re-runs while the server is **already up** (e.g.
`make rebuild-api` pulls it in as a dependency), the server keeps serving its stale cache:

- `GET …/realms/tchalanet/users/count` → **0**
- a non-cached user (`cashier`) → `invalid_grant: Invalid user credentials`
- an already-cached user (`super_admin`) → still logs in ← contradictory, this is the tell

**Fix:** restart the server so it reloads the realm from the DB:

```bash
docker restart tchl-keycloak-dev
# wait for healthy, then re-check users/count → 8
```

This Keycloak-only troubleshooting section is retained temporarily for legacy E2E targets. The
standard local-IDE path now uses Firebase Auth Emulator.

### Firebase bootstrap sync

`POST /platform/ops/sync/identity/firebase-bootstrap-users` creates or reuses the deterministic
Firebase Emulator users and persists their `FIREBASE` external identity mappings. It is
idempotent and restricted to `SUPER_ADMIN`.

---

## 6. edge-service: testing API → edge → Slack

The API does **not** talk to Slack directly. `SLACK_ENABLED=false` on the API; it relays to
edge-service (`TCH_EDGE_BASE_URL=http://edge-service:3000`), which holds the real webhooks
(`SLACK_ENABLED=true`). Valid channel keys: `OPS_ALERTS`, `SECURITY_AUDIT`, `DELIVERY`,
`BATCH_DRAWS`.

Verify the full chain (sends a **real** Slack message — use a low-noise channel):

```bash
SA=$(curl -sk -X POST https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token \
  -d grant_type=password -d client_id=tchalanet-swagger -d scope=openid \
  -d username=super_admin -d password='Changeme1!' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

curl -sk -X POST https://api.localtest.me/api/v1/platform/ops/communication/slack-test \
  -H "Authorization: Bearer $SA" -H "Content-Type: application/json" \
  -d '{"channelKey":"OPS_ALERTS","title":"edge check","message":"API → edge → Slack OK"}'
# expected: {"status":"SUCCESS","data":{"sent":true,"provider":"edge",...}}

docker logs tchalanet-edge-service-dev --tail 5   # POST /internal/messages/send → 202
```

If you get `I/O error … http://localhost:3000` the API isn't picking up `TCH_EDGE_BASE_URL`
(env-name drift) — the app reads `TCH_EDGE_BASE_URL` first, then
`TCH_COMMUNICATION_EDGE_BASE_URL`. Confirm the API container is on the `edge-dev` network
and resolves `edge-service`.

---

## 7. Rebuilding the API after code changes

> **`make rebuild-api` is unreliable**: it builds the JAR but the dev container runs image
> tag `local-dev` while the override defaults to `local-build`, and compose reports
> "Container Running" (skips recreate). Your changes silently don't take effect. It also
> re-runs `keycloak-init` (see §5).

Reliable path — the Dockerfile is self-contained (it compiles from source in a build
stage), so no separate `mvn` step is needed:

```bash
# 1. build, tagged exactly what the dev container uses
cd tchalanet-server
docker build -t ghcr.io/bhebb/tchalanet-api:local-dev -f Dockerfile .   # ~45s (deps cached)

# 2. recreate ONLY the api container (don't trigger keycloak-init)
cd ../tchalanet-infra
ENV=dev IMAGE_TAG=local-dev KEYCLOAK_IMAGE=tchl/keycloak:local-dev KC_EXTRA_ARGS="" \
docker compose --project-name tch-dev \
  --env-file envs/common/compose.env --env-file envs/dev/compose.env --env-file envs/dev/.env.merged \
  -f compose/docker-compose-project.yml -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml -f compose/docker-compose-keycloak.yml \
  -f compose/docker-compose.local-build.yml -f compose/docker-compose-api.yml \
  up -d --no-deps --force-recreate api
```

Traefik routes by labels, so the container name doesn't matter for `api.localtest.me`.
Verify with `docker inspect <api-container> --format '{{.Image}}'` against the new build SHA,
and `curl -sk https://api.localtest.me/api/v1/actuator/health`.

---

## 8. Troubleshooting quick reference

| Symptom | Likely cause | Fix |
|---|---|---|
| Firebase auth `external_identity.not_linked` | bootstrap users not synced | restart API with Firebase bootstrap enabled and `TCH_IDENTITY_FIREBASE_BOOTSTRAP_USERS=superadmin,admin` |
| legacy Keycloak auth `invalid_client` | secret sent to public client | unset `TCH_KEYCLOAK_CLIENT_SECRET` |
| legacy Keycloak auth `invalid_grant` for `cashier` but `super_admin` works | KC cache stale after re-import | `docker restart tchl-keycloak-dev` (§5) |
| legacy Keycloak `users/count` = 0 but DB has rows | same KC cache gotcha | restart KC (§5) |
| code changes don't take effect | stale API image | rebuild via §7, not `make rebuild-api` |
| slack-test `I/O error localhost:3000` | edge URL env drift / wrong network | §6 |
| Tenant B tests all skip | `TCH_TENANT_2_*` unset | configure Tenant B (§3) |
| `/tmp` ENOSPC in sandbox | tiny tmpfs | `CLAUDE_CODE_TMPDIR=/tmp/tch-e2e <cmd>` |

---

## 9. Design & specs

- Proposal/design/tasks: `tchalanet-server/openspec/changes/e2e-business-runtime-v1/`
- These tests are **scenario-first, not endpoint-first**. Concurrency here means
  *correctness under small parallelism* (2-10 requests), **not** load/perf. Load/perf lives
  under `loadtest/` and must reuse the canonical scenario entry point instead of defining new
  business truth.
