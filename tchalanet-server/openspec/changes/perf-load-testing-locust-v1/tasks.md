# Tasks — perf-load-testing-locust-v1

## 1. Locust scaffold

- [ ] Add `locust` dependency under Python `[perf]` extra.
- [ ] Add `testing/e2e/locust` package.
- [ ] Add `locustfile.py` entrypoint.
- [ ] Add README with local headless, Web UI, and CI manual-dispatch examples.

## 2. Client adapter

- [ ] Implement `LocustApiClient` wrapping `tch_e2e.client.ApiClient`.
- [ ] Fire Locust request events manually for every wrapped call.
- [ ] Use stable metric names, not dynamic URLs with IDs.
- [ ] Preserve existing e2e headers, tenant context, op context, auth token, and SSL config.
- [ ] Track expected business errors separately from infrastructure failures.

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

- [ ] Implement `sell_basket.py`.
- [ ] Time `sales.prepare`.
- [ ] Time `sales.confirm`.
- [ ] Time `sales.sell`.
- [ ] Generate unique idempotency key per sell intent.
- [ ] Assert successful sale response has a ticket/outcome.
- [ ] Preserve Maryaj gratis behavior when campaign is active.

## 7. Read POS task

- [ ] Implement `read_pos.py`.
- [ ] Time POS dashboard/home endpoint.
- [ ] Time active draw list endpoint.
- [ ] Time games/options/config endpoint if used by POS sale screen.
- [ ] Keep read scenario lightweight and realistic.

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

- [ ] Enable `--class-picker` so the web page lists the available **scenarios/test cases**
      (User classes + LoadShapes) to select and launch.
- [ ] Expose domain **inputs as editable web form fields** (via Locust custom command-line args that
      render in the UI): basket size min/max (5–10), scenario profile (read/sales/mixed), target
      tenant/terminal/cashier, `TCH_LOAD_RUN_ID`, concurrency & spawn rate.
- [ ] Support start / stop and **live adjustment** of user count & spawn rate from the page, with
      live RPS / p50-p95-p99 / failure charts and CSV download.
- [ ] (Optional) a small **custom web route/page** (Locust web extension) that presents the test
      cases and their descriptions/inputs more explicitly than the default form.
- [ ] Publish the post-run **HTML report** as a shareable artifact.
- [ ] Document opening the page, picking a case, editing inputs, launching, and exporting results.

## 13. Metrics analysis (optional, v2)

- [ ] (Optional, opt-in) stream stats to Prometheus + a Grafana dashboard for run-to-run comparison;
      flagged, non-prod only. Not required for v1 — the operation page above already covers usage.
