# Design — perf-load-testing-locust-v1

## Principle

Load = the **same** business flow as correctness e2e, driven at concurrency. Reuse
`testing/e2e/tch_e2e`; do not re-implement the API client, auth, or data building.

## Layout

```
testing/e2e/
  tch_e2e/                # existing: client, auth, config, data_factory, scenario_world, ticket_matrix
  locust/
    __init__.py
    client.py             # LocustApiClient: adapts tch_e2e.ApiClient onto locust's request timing
    users.py              # CashierUser (sell), ReaderUser (POS draws / dashboard)
    tasks/
      sell_basket.py      # prepare -> confirm -> sell of a 5–10 line ticket
      read_pos.py         # list POS draws, dashboard overview
    scenarios.py          # weighted task sets + ramp profiles
    locustfile.py         # entrypoint (host, users, spawn-rate via env/CLI)
    README.md
  pyproject.toml          # + locust dependency, [perf] extra
```

## Reuse of existing concepts

- **Client**: wrap `tch_e2e.client.ApiClient` (httpx) so each call is timed and reported to Locust
  (`events.request.fire`), keeping op-context headers (`OpContext`) and `with_tenant`.
- **Auth**: `tch_e2e.auth` — one authenticated cashier session per Locust user (Firebase token),
  refreshed as in the e2e suite.
- **Data**: `data_factory` + `ticket_matrix` build a valid 5–10 line basket (bet types, stakes) for
  the seeded tenant/terminal/cashier; `scenario_world` holds the per-user seeded context.
- **Config**: same env vars as e2e (`TCH_TENANT_ID`, `TCH_TERMINAL_ID`, `TCH_CASHIER_USER_ID`,
  `TCH_STAKE_CENTS`, base URL, `TCH_E2E_VERIFY_SSL`). No new config dialect.

## Sell scenario (the 5–10 ticket target)

1. `PrepareSaleCommand` → prepared sale id (basket of 5–10 `SellTicketLineInput`).
2. `ConfirmPreparedSaleCommand`.
3. `SellTicketCommand` → `SellTicketOutcome`.
Each step timed separately; the basket size (5–10) is randomized per iteration.

## Metrics & budgets

- Per request type: p50 / p95 / p99 latency, RPS, failure ratio (Locust native stats + CSV export).
- Recorded per scenario at fixed concurrency steps (e.g. 1, 5, 10, 25, 50 cashiers).
- Budgets start **observational** (documented baseline), promoted to thresholds once stable.

## Isolation & safety

- Runs against a **seeded non-prod** environment only (guard on host allowlist; refuse prod hosts).
- Idempotency keys per sell (reuse e2e pattern) so retries don't double-sell.
- Load data is confined to the seeded test tenant.

## Alignment with concurrent work (maryaj-gratis / limits ergonomics)

This change must reflect the in-flight `codex/maryaj-gratis-promotion-ux` work once merged:

- **Limit rule catalog changed** (`rules.v1.json`: `MAX_POTENTIAL_PAYOUT_PER_TICKET` and others
  dropped). The sell scenario evaluates limits, so limit-touching load/e2e scenarios MUST exercise
  the *current* rule catalog — do not hard-code removed rules; read config as the app serves it.
- **`DrawChannelSummaryView.id`** is now exposed — usable to target specific channels in scenarios.
- The Java `BusinessRuntimeIntegrationTestBase` (`@SpringBootTest`) is a **complementary in-process
  layer**; this OpenSpec is the **external Python/Locust** layer over the real HTTP API. They share
  business intent, not code — keep scenario definitions consistent with that harness.

## Run modes

- Local headless: `locust -f locust/locustfile.py --headless -u 10 -r 2 -t 2m --host $BASE_URL`.
- Web UI for exploratory ramps.
- CI: manual-dispatch job, artifacts = CSV stats + HTML report; non-blocking.
