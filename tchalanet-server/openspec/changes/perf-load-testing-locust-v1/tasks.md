# Tasks — perf-load-testing-locust-v1

> v1 slice landed: `testing/e2e/loadtest/` (package renamed from `locust` to avoid shadowing the
> library) — client adapter, cashier sell/read tasks, Web UI custom inputs. Remaining items unchecked.

## 1. Locust scaffold

- [x] Add `locust` dependency under Python `[perf]` extra.
- [x] Add `testing/e2e/loadtest` package (not `locust/` — would shadow the library).
- [x] Add `locustfile.py` entrypoint.
- [x] Add README with local headless, Web UI, and CI manual-dispatch examples.

## 2. Client adapter

- [x] Implement `LocustApiClient` wrapping `tch_e2e.client.ApiClient`.
- [x] Fire Locust request events manually for every wrapped call.
- [x] Use stable metric names, not dynamic URLs with IDs (sell-path endpoints are ID-free).
- [x] Preserve existing e2e headers, tenant context, op context, auth token, and SSL config (delegated
      to the wrapped `ApiClient`).
- [ ] Track expected business errors separately from infrastructure failures (v1: 5xx + unexpected
      4xx = failure; dedicated expected-block tasks are §8).

## 3. Safety guards

- [ ] Add non-prod host allowlist.
- [ ] Refuse known production hosts by default.
- [ ] Require explicit load-test tenant/cashier/terminal config.
- [ ] Add `TCH_LOAD_RUN_ID`.
- [ ] Prefix idempotency keys with run id and user id.
- [ ] Document cleanup/retention expectations for generated tickets.

## 4. Auth/session reuse

- [ ] Reuse `tch_e2e.auth`.
- [ ] Authenticate once per Locust user in `on_start`.
- [ ] Refresh token using existing e2e refresh behavior.
- [ ] Track auth/login/refresh metrics separately.
- [ ] Avoid logging in before every task.

## 5. Data and scenario world

- [ ] Reuse `scenario_world` for per-user seeded context.
- [ ] Reuse `data_factory` and `ticket_matrix` for basket generation.
- [ ] Randomize basket size between 5 and 10 lines.
- [ ] Generate only currently supported bet types/options from served config.
- [ ] Do not hard-code removed limit rules.
- [ ] Use `DrawChannelSummaryView.id` where scenario targeting needs channel specificity.

## 6. Sell task

NOTE: the real external cashier API is **preview → sell** (`/tenant/cashier/tickets/preview` then
`/sell`), not the internal prepare/confirm/sell command names.

- [x] Implement the `sell_basket` task (in `loadtest/users.py`).
- [x] Time `POST /tenant/cashier/tickets/preview`.
- [x] Time `POST /tenant/cashier/tickets/sell`.
- [x] Generate unique idempotency key per sell intent (via `CashierFlow`).
- [ ] Assert successful sale response has a ticket/outcome (v1 swallows to keep the user alive;
      request outcome already reported).
- [x] Preserve Maryaj gratis behavior when campaign is active (inherited from the real sell flow).

## 7. Read POS task

- [x] Implement the `read_pos` task (in `loadtest/users.py`).
- [x] Time active draw list endpoint (`GET /tenant/cashier/draws/available` — cached read path).
- [ ] Time POS dashboard/home endpoint.
- [ ] Time games/options/config endpoint if used by POS sale screen.
- [x] Keep read scenario lightweight and realistic.

## 8. Optional controlled failure paths

- [ ] Add expected limit-block scenario.
- [ ] Add expected cutoff-block scenario.
- [ ] Add idempotency replay scenario.
- [ ] Do not count expected 4xx business blocks as Locust infrastructure failures.
- [ ] Count unexpected 5xx and unexpected 4xx as failures.

## 9. Scenario profiles

- [ ] Add read-heavy profile.
- [ ] Add sales-heavy profile.
- [ ] Add mixed realistic profile.
- [ ] Support fixed concurrency steps: 1, 5, 10, 25, 50 users.
- [ ] Support duration/spawn rate via env or CLI.

## 10. Metrics and artifacts

- [ ] Export Locust CSV stats.
- [ ] Export HTML report.
- [ ] Document p50/p95/p99/RPS/failure ratio.
- [ ] Store run metadata: commit SHA, environment, tenant, run id, scenario profile.
- [ ] Keep budgets observational in v1.

## 11. CI integration

- [ ] Add manual-dispatch CI job.
- [ ] Run only against approved non-prod environment.
- [ ] Upload CSV + HTML artifacts.
- [ ] Keep job non-blocking in v1.
- [ ] Document how to compare two runs manually.

## 12. Operation web page (see cases, change inputs, run)

The Locust Web UI itself, extended, is the control panel — no external tooling.

- [x] Enable `--class-picker` so the web page lists the available **scenarios/test cases**
      (documented in README; wired at launch).
- [x] Expose domain **inputs as editable web form fields** (Locust custom args in `locustfile.py`):
      `basket-min` / `basket-max` (5–10) render on the Web UI start screen. (More inputs — profile,
      tenant/terminal — to add.)
- [x] Support start / stop and **live adjustment** of user count & spawn rate (Locust native), with
      live RPS / p50-p95-p99 / failure charts and CSV download.
- [ ] (Optional) a small **custom web route/page** (Locust web extension) that presents the test
      cases and their descriptions/inputs more explicitly than the default form.
- [ ] Publish the post-run **HTML report** as a shareable artifact.
- [ ] Document opening the page, picking a case, editing inputs, launching, and exporting results.

## 13. Metrics analysis (optional, v2)

- [ ] (Optional, opt-in) stream stats to Prometheus + a Grafana dashboard for run-to-run comparison;
      flagged, non-prod only. Not required for v1 — the operation page above already covers usage.
