# Tchalanet E2E — Business Runtime

Reproducible end-to-end tests for the Tchalanet platform. They drive the **real** stack
(API + Keycloak + Postgres + Redis + Traefik + edge-service) over HTTPS, exactly as a
client would. No mocks.

> **Agents & first-timers: read this whole file before running anything.** The two things
> that waste the most time are (1) the Keycloak `keycloak-init` cache gotcha and (2)
> rebuilding the API image. Both are solved below — don't rediscover them.

---

## 0. TL;DR

```bash
# 1. Bring the stack up (from tchalanet-infra/)
cd tchalanet-infra
make local-product-up            # Traefik + Postgres + Keycloak + Redis + API + edge-service

# 2. Sanity: stack reachable + auth works
curl -sk https://api.localtest.me/api/v1/actuator/health      # {"status":"UP"}
curl -sk -X POST https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token \
  -d grant_type=password -d client_id=tchalanet-swagger -d scope=openid \
  -d username=super_admin -d password='Changeme1!' | python3 -c 'import sys,json;print("OK" if "access_token" in json.load(sys.stdin) else "FAIL")'

# 3. Run the tests (from tchalanet-server/testing/e2e/)
cd ../tchalanet-server/testing/e2e
source .venv/bin/activate
python -m pytest -m L0                       # boot smoke (fast)
python -m pytest -m "not L3"                 # full business suite (no concurrency)
```

If auth says `FAIL` or a smoke test 500s on user lookup → **§5 Keycloak gotcha** (restart KC).

---

## 1. What runs where (entry points)

| Layer | URL (dev) | Notes |
|---|---|---|
| API | `https://api.localtest.me/api/v1` | Traefik TLS; HTTP→HTTPS 301. Context path `/api/v1` is **auto-added** by the servlet — controllers must NOT repeat it. |
| Keycloak | `https://auth.localtest.me/realms/tchalanet` | Realm `tchalanet`. Direct-access (password) grant client = **`tchalanet-swagger`** (public, no secret). `tchalanet-api` is confidential and has direct-access **disabled** — don't use it for password grant. |
| edge-service | `http://edge-service:3000` (in-cluster) | Slack/email relay. API reaches it by Docker DNS name, not localhost. |
| Postgres | `tchl-postgres-dev` | App DB `tchalanet` (user `app_user`), KC DB `keycloak_db` (user `kc_user`). |

Test code lives in `tchalanet-server/testing/e2e/`:

```
tch_e2e/        harness: config, auth, client, api_response, scenario_world, assertions,
                data_factory, ticket_matrix, concurrency
flows/          high-level flows (cashier, onboarding, outlet, terminal, seller, ...)
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
| `make local-ide-up` | P0: Traefik + Postgres + Keycloak (API runs in your IDE) |
| `make local-ide-up-redis` | P0 + Redis |
| `make local-api-up` | P0 + Redis + **API in Docker** |
| `make local-product-up` | **Full stack**: API + edge-service + web ← use this for E2E |
| `make up-edge` / `make down-edge` | edge-service only |
| `make local-api-smoke` | health check (Redis + API) |
| `make ps` | list running containers |
| `make logs-api` / `make logs-<svc>` | tail a service's logs |
| `make local-product-down` | tear the full stack down |

Ordering is handled by `depends_on`: Postgres → `keycloak-init` (one-shot realm import) →
Keycloak server → API. A **cold** bring-up is correct and reproducible on any machine
(the KC server starts *after* the import). See §5 for the one case where it isn't.

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
| `TCH_E2E_AUTH_PROVIDER` | `keycloak` | `keycloak`, `local-jwt`, or `local-perf` |
| `TCH_LOCAL_JWT_ISSUER` / `_SECRET` | `tchalanet-local` / dev-only secret | Required when using local auth; must match API config |
| `TCH_KEYCLOAK_TOKEN_URL` | `https://auth.localtest.me/realms/tchalanet/protocol/openid-connect/token` | token endpoint |
| `TCH_KEYCLOAK_CLIENT_ID` | `tchalanet-swagger` | **public** direct-access client (default if unset) |
| `TCH_KEYCLOAK_CLIENT_SECRET` | *(leave unset)* | a secret on a public client → `invalid_client` |
| `TCH_SUPER_ADMIN_USERNAME` / `_PASSWORD` | `super_admin` / `Changeme1!` | platform role |
| `TCH_SELLER_USERNAME` / `_PASSWORD` | `cashier` / `Changeme1!` | POS cashier |
| `TCH_TENANT_ADMIN_USERNAME` / `_PASSWORD` | `admin` / `Changeme1!` | `/admin/*` endpoints |
| `TCH_TENANT_CODE` / `TCH_TENANT_ID` | `tchalanet` / `…0003` | seeded Tenant A |
| `TCH_OUTLET_ID` / `TCH_TERMINAL_ID` | `…3001` / `…3101` | seeded outlet/terminal (V205) |
| `TCH_TEST_SLACK_CHANNEL_KEY` | `delivery` | enables the POS "send ticket" step (§6) |
| `TCH_TENANT_2_*` | *(unset)* | Tenant B — enables `multitenant/` + `concurrency/` |

> **Common mistake:** `.env.local` setting `TCH_KEYCLOAK_CLIENT_SECRET` while
> `TCH_KEYCLOAK_CLIENT_ID` defaults to the *public* `tchalanet-swagger`. A public client
> rejects a secret → `invalid_client`. Either leave the secret unset, or set the client id
> to a confidential client that has direct-access grants enabled.

Seeded users (realm import), all password `Changeme1!`:
`super_admin`, `admin` (tenant admin), `operator`, `cashier`, plus edge-case cashiers
(`cashier_blocked`, `cashier_no_terminal`, `cashier_offline_allowed`, `cashier_offline_denied`).
The deterministic users (`super_admin`/`admin`/`cashier`/`operator`) have fixed Keycloak external
subjects in `app_user_external_identity`
matching the `app_user` seeds, so no sync is needed for the happy path.

For Firebase-independent E2E/performance validation, start the API with
`TCH_IDENTITY_PROVIDER=local-jwt` or `local-perf`, configure the same
`TCH_LOCAL_JWT_ISSUER`/`TCH_LOCAL_JWT_SECRET` in the harness, and set
`TCH_E2E_AUTH_PROVIDER` accordingly. The harness signs tokens only for the seeded
`super_admin`, `admin`, and `cashier`. Their token roles are routing hints; the API replaces them
with database-owned roles and permissions before executing handlers. The existing multitenant L3
suite then exercises the normal context, permission, pooled-connection, and PostgreSQL RLS path.

After recreating the database with the current canonical migrations:

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

### Recipes

```bash
python -m pytest -m L0                       # boot smoke
python -m pytest -m "L0 or L1"               # daily smoke
python -m pytest -m "not L3"                 # everything except concurrency (CI default)
python -m pytest -m cashier_pos              # just the POS flow
python -m pytest tests/onboarding -q         # one directory
python -m pytest -m L3                        # concurrency (needs Tenant B, §3)
python -m pytest -k happy_path -q            # by name
```

Tests that need an unconfigured prerequisite **skip** (not fail) — e.g. Tenant B tests skip
with a clear `UserWarning` until `TCH_TENANT_2_*` is set.

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

Do **not** edit `realm.json` to "fix" users — user/realm state is owned by the import +
`KeycloakBootstrapSyncService`, not by hand-editing.

### Keycloak external-subject sync (rarely needed)

The cashier fixture auto-calls `POST /platform/ops/sync/identity/keycloak-bootstrap-users`
(super_admin) if `/tenant/me/profile` 403s, to reconcile the KEYCLOAK
`app_user_external_identity.external_subject` with KC.
The deterministic seeded users already match, so this only matters for the random-UUID
edge-case cashiers.

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
| auth `invalid_client` | secret sent to public client | unset `TCH_KEYCLOAK_CLIENT_SECRET` |
| auth `invalid_grant` for `cashier` but `super_admin` works | KC cache stale after re-import | `docker restart tchl-keycloak-dev` (§5) |
| `users/count` = 0 but DB has rows | same KC cache gotcha | restart KC (§5) |
| code changes don't take effect | stale API image | rebuild via §7, not `make rebuild-api` |
| slack-test `I/O error localhost:3000` | edge URL env drift / wrong network | §6 |
| Tenant B tests all skip | `TCH_TENANT_2_*` unset | configure Tenant B (§3) |
| `/tmp` ENOSPC in sandbox | tiny tmpfs | `CLAUDE_CODE_TMPDIR=/tmp/tch-e2e <cmd>` |

---

## 9. Design & specs

- Proposal/design/tasks: `tchalanet-server/openspec/changes/e2e-business-runtime-v1/`
- These tests are **scenario-first, not endpoint-first**. Concurrency here means
  *correctness under small parallelism* (2–10 requests), **not** load/perf — that's a
  separate future suite.
