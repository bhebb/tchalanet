# Load / perf testing (Locust)

Drives the **real** POS sell path (`preview` → `sell`, 5–10 line baskets) and POS reads at
concurrency, reusing the e2e layer (`tch_e2e` client/auth/config + `flows.cashier.CashierFlow`).
Package is `loadtest` (not `locust`) so it doesn't shadow the library.

## Install

```bash
cd tchalanet-server/testing/e2e
pip install -e '.[perf]'        # installs locust + the e2e deps
```

## Configure (env — same vars as the e2e suite)

| Var | Meaning |
|-----|---------|
| `TCH_BASE_URL` | API base URL (**non-prod**; prod hosts are refused, see `safety.py`) |
| `TCH_E2E_AUTH_PROVIDER` | `keycloak` (default) or a local JWT provider |
| `TCH_SELLER_USERNAME` / `TCH_SELLER_PASSWORD` | cashier credentials |
| `TCH_TENANT_ID` / `TCH_OUTLET_ID` / `TCH_TERMINAL_ID` | seeded op context |
| `TCH_STAKE_CENTS` | per-line stake (default 100) |
| `TCH_LOAD_RUN_ID` | tags idempotency keys / metadata for this run |
| `TCH_LOAD_ALLOWED_HOSTS` | comma list to allow an otherwise-denied host |

## Run — Web UI (the operation page)

```bash
locust -f loadtest/locustfile.py --class-picker --host "$TCH_BASE_URL"
# open http://localhost:8089
```

On the start screen you **pick the scenario** (class-picker) and **edit the inputs** —
users, spawn rate, plus `basket-min` / `basket-max` — then launch and watch live
RPS / p50-p95-p99 / failures; adjust users at runtime; download CSV.

## Run — headless (CI / scripted)

```bash
locust -f loadtest/locustfile.py --headless \
  -u 10 -r 2 -t 2m --host "$TCH_BASE_URL" \
  --csv results/run --html results/run.html \
  --basket-min 5 --basket-max 10
```

- `-u` users, `-r` spawn rate, `-t` duration.
- `--csv` / `--html` export stats + a shareable report.
- Filter tasks with `--tags sales` or `--tags read`.

## Scenarios

- `CashierUser.sell_basket` — `preview` + `sell` of a randomized 5–10 line ticket (idempotent).
- `CashierUser.read_pos` — POS available draws (cached read path).

Budgets are **observational** in v1 (record p50/p95/p99, RPS, error ratio per run).
