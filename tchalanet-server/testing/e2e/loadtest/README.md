# Load / perf testing (Locust)

Locust is a load projection over the E2E harness, not a second source of
business scenarios. The canonical scenario entry point is
[`../docs/business-day-scenarios.md`](../docs/business-day-scenarios.md).

The current v1 harness drives the **real** seller-terminal POS sell path
(`preview` → `sell`, 5-10 line baskets) and POS reads at concurrency, reusing
the e2e layer (`tch_e2e` client/auth/config + `flows.cashier.CashierFlow`).
Package is
`loadtest` (not `locust`) so it doesn't shadow the library.

The next business-day Locust user must reuse `tch_e2e.business_day` tenant
plans, seller-terminal plans, ticket basket, and result plan. It may vary only
concurrency, spawn rate, duration, selected tenants/draws, and basket repeats.

## Install

```bash
cd tchalanet-server/testing/e2e
pip install -e '.[perf]'        # installs locust + the e2e deps
```

## Configure (env — same vars as the e2e suite)

Locust must not use Firebase Auth Emulator for load. Run the API with
`TCH_IDENTITY_PROVIDER=local-perf` or `local-jwt`, and configure the harness with
the same issuer/secret.

| Var | Meaning |
|-----|---------|
| `TCH_BASE_URL` | API base URL (**non-prod**; prod hosts are refused, see `safety.py`) |
| `TCH_E2E_AUTH_PROVIDER` | `local-perf` or `local-jwt` for Locust |
| `TCH_LOCAL_JWT_ISSUER` | Must match the API runtime issuer, usually `tchalanet-local` |
| `TCH_LOCAL_JWT_SECRET` | Must match the API runtime secret; at least 32 characters |
| `TCH_LOAD_SELLER_TERMINAL_ID` | Optional seller-terminal id. If omitted, Locust discovers the first active terminal through `/admin/seller-terminals`. |
| `TCH_LOAD_ACTOR_MODE` | `seller-terminal` (default) or `admin-act-as` for diagnostics only |
| `TCH_TENANT_ID` / `TCH_LOAD_TENANT_ID` | Tenant used for seller-terminal discovery |
| `TCH_STAKE_CENTS` | per-line stake for the current v1 harness |
| `TCH_LOAD_RUN_ID` | tags idempotency keys / metadata for this run |
| `TCH_LOAD_ALLOWED_HOSTS` | comma list to allow an otherwise-denied host |

## Local Quickstart

From a rebuilt local stack, switch only the API to `local-perf`, prepare the
seller-terminal identities, then run Locust.

Local routing is Traefik-first:

- Locust target host is `https://127.0.0.1/api/v1`.
- Send `TCH_E2E_HOST_HEADER=api.localtest.me` so Traefik picks the API router.
- Do not probe actuator under `/api/v1`; actuator is not mounted behind the API
  servlet context. Use `https://127.0.0.1/actuator/health` with the same host
  header, or `https://api.localtest.me/actuator/health` when local DNS works.

```bash
cd tchalanet-infra
/usr/local/bin/docker compose --project-name tch-dev \
  --env-file envs/common/compose.env \
  --env-file envs/dev/compose.env \
  --env-file envs/dev/.secrets \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-edge-service.yml \
  -f compose/docker-compose-firebase-emulator.yml \
  -f compose/docker-compose.local-build.yml \
  -f compose/docker-compose-api.yml \
  -f /tmp/tchalanet-locust-local-perf.override.yml \
  up -d --force-recreate --no-deps api
```

The override file must contain the same issuer/secret used by Locust:

```yaml
services:
  api:
    environment:
      TCH_IDENTITY_PROVIDER: local-perf
      TCH_LOCAL_JWT_ISSUER: tchalanet-local
      TCH_LOCAL_JWT_SECRET: dev-only-change-me-at-least-32-characters
      FIREBASE_AUTH_EMULATOR_HOST: ""
```

Before running Locust locally, mirror active seller terminals into the local-perf
identity table. This is a local-only bootstrap; it lets Locust mint JWTs with
`sub=<sellerTerminalId>` and avoids loading Firebase Auth Emulator. By default
it also marks active local terminals as PIN-ready (`must_change_pin=false`) so
the real seller-terminal actor can confirm sales; set
`TCH_LOCUST_MARK_TERMINALS_READY=false` to skip that local prep:

```bash
cd tchalanet-server/testing/e2e
TCH_LOCAL_JWT_ISSUER=tchalanet-local \
  python scripts_prepare_locust_local_perf.py
```

Then run a smoke read:

```bash
TCH_BASE_URL='https://127.0.0.1/api/v1' \
TCH_E2E_HOST_HEADER='api.localtest.me' \
TCH_E2E_VERIFY_SSL=false \
TCH_E2E_AUTH_PROVIDER=local-perf \
TCH_LOCAL_JWT_ISSUER=tchalanet-local \
TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters \
locust -f loadtest/locustfile.py --headless \
  -u 2 -r 1 -t 30s --host 'https://127.0.0.1/api/v1' \
  --csv target/locust/read-smoke --html target/locust/read-smoke.html \
  --basket-min 1 --basket-max 2 --tags read
```

Run a small sales smoke:

```bash
TCH_BASE_URL='https://127.0.0.1/api/v1' \
TCH_E2E_HOST_HEADER='api.localtest.me' \
TCH_E2E_VERIFY_SSL=false \
TCH_E2E_AUTH_PROVIDER=local-perf \
TCH_LOCAL_JWT_ISSUER=tchalanet-local \
TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters \
locust -f loadtest/locustfile.py --headless \
  -u 1 -r 1 -t 20s --host 'https://127.0.0.1/api/v1' \
  --csv target/locust/sales-smoke --html target/locust/sales-smoke.html \
  --basket-min 1 --basket-max 1 --tags sales
```

Restore the API to the normal local E2E provider after load testing:

```bash
cd tchalanet-infra
/usr/local/bin/docker compose --project-name tch-dev \
  --env-file envs/common/compose.env \
  --env-file envs/dev/compose.env \
  --env-file envs/dev/.secrets \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-edge-service.yml \
  -f compose/docker-compose-firebase-emulator.yml \
  -f compose/docker-compose.local-build.yml \
  -f compose/docker-compose-api.yml \
  up -d --force-recreate --no-deps api
```

## Run — Web UI (interactive tuning)

The Locust Web UI is useful when you want to tune a run live instead of
committing to a fixed headless command. Start the API in `local-perf`, run the
prep script above, then start Locust without `--headless`:

```bash
export TCH_E2E_AUTH_PROVIDER=local-perf
export TCH_BASE_URL='https://127.0.0.1/api/v1'
export TCH_E2E_HOST_HEADER='api.localtest.me'
export TCH_E2E_VERIFY_SSL=false
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters
locust -f loadtest/locustfile.py --class-picker --host "$TCH_BASE_URL"
```

Open [http://localhost:8089](http://localhost:8089).

On the start screen:

| Field | What to enter |
|-------|---------------|
| User classes | `CashierUser` |
| Number of users | Concurrent virtual seller-terminals, e.g. `1`, `5`, `10`, `25` |
| Spawn rate | New users per second, e.g. `1`, `2`, `5` |
| Host | `https://127.0.0.1/api/v1` |
| Run time | Optional, e.g. `30s`, `2m`, `10m`; leave empty for manual stop |
| basket-min | Minimum lines per sold ticket |
| basket-max | Maximum lines per sold ticket |

After launch, use the UI to:

- watch live RPS, p50/p95/p99 latency, failures, and exceptions;
- change the number of users while the run is active;
- stop the run manually when the signal is enough;
- download CSV reports from the Locust stats pages.

The Web UI does **not** change authentication, selected tenant, selected seller
terminal, SSL verification, or host header after Locust has started. Change
those with environment variables before launching Locust:

```bash
export TCH_LOAD_TENANT_ID='...'
export TCH_LOAD_SELLER_TERMINAL_ID='...'
export TCH_LOAD_RUN_ID='load-001'
```

Use tags from the command line to choose the scenario family exposed in the UI:

```bash
locust -f loadtest/locustfile.py --class-picker --tags read --host "$TCH_BASE_URL"
locust -f loadtest/locustfile.py --class-picker --tags sales --host "$TCH_BASE_URL"
```

## Run — headless (CI / scripted)

```bash
export TCH_E2E_AUTH_PROVIDER=local-perf
export TCH_LOCAL_JWT_ISSUER=tchalanet-local
export TCH_LOCAL_JWT_SECRET=dev-only-change-me-at-least-32-characters
locust -f loadtest/locustfile.py --headless \
  -u 10 -r 2 -t 2m --host "$TCH_BASE_URL" \
  --csv results/run --html results/run.html \
  --basket-min 5 --basket-max 10
```

- `-u` users, `-r` spawn rate, `-t` duration.
- `--csv` / `--html` export stats + a shareable report.
- Filter tasks with `--tags sales` or `--tags read`.

## Tuning Knobs

| Knob | Example | Effect |
|------|---------|--------|
| Users | `-u 50` | Concurrent virtual seller-terminals |
| Spawn rate | `-r 5` | New users per second |
| Duration | `-t 10m` | Total run time |
| Basket size | `--basket-min 5 --basket-max 10` | Lines per sold ticket |
| Scenario | `--tags read` / `--tags sales` | Read-only cache path or real sale path |
| Seller terminal | `TCH_LOAD_SELLER_TERMINAL_ID=...` | Force one terminal instead of auto-selected prepared terminal |
| Tenant | `TCH_LOAD_TENANT_ID=...` | Force the tenant used for discovery |
| Run id | `TCH_LOAD_RUN_ID=load-001` | Groups idempotency/metadata for analysis |

Recommended tuning sequence:

1. Start with `--tags read -u 2 -r 1 -t 30s`.
2. Run `--tags sales -u 1 -r 1 -t 20s --basket-min 1 --basket-max 1`.
3. Increase basket size to the real target, usually `5..10`.
4. Increase users gradually: `5`, `10`, `25`, `50`.
5. Watch p95/p99, failures/s, API logs, Postgres CPU/locks, and Redis latency.

## Current Load Users

- `CashierUser.sell_basket` — seller-terminal `preview` + `sell` of a randomized 5–10 line ticket (idempotent).
- `CashierUser.read_pos` — seller-terminal POS available draws (cached read path).

Budgets are **observational** in v1 (record p50/p95/p99, RPS, error ratio per run).

Do not add a new business scenario list here. Add scenario intent to the
canonical entry point first, then expose it as a Locust user.
