# Tasks

## Phase 0 — Setup
- [ ] Add `locust` to `testing/e2e/pyproject.toml` (a `[project.optional-dependencies] perf` extra).
- [ ] Create `testing/e2e/locust/` package skeleton.
- [ ] Add a host allowlist guard that refuses to run against a production host.

## Phase 1 — Client adapter
- [ ] `LocustApiClient` wrapping `tch_e2e.client.ApiClient`: fire `events.request` with name/latency/
      exception per call; preserve `OpContext` headers and `with_tenant`.
- [ ] One authenticated cashier session per Locust user via `tch_e2e.auth` (token refresh).

## Phase 2 — Sell scenario (5–10 tickets)
- [ ] `sell_basket` task: `PrepareSaleCommand` → `ConfirmPreparedSaleCommand` → `SellTicketCommand`
      with a randomized 5–10 line basket built from `data_factory` / `ticket_matrix`.
- [ ] Per-step timing (prepare / confirm / sell) reported separately.
- [ ] Idempotency key per sell; assert no double-sell on retry.

## Phase 3 — Read scenario + mix
- [ ] `read_pos` task: list POS draws + dashboard overview (exercises the cache read paths).
- [ ] Weighted `scenarios.py`: write-heavy, read-heavy, and mixed profiles.

## Phase 4 — Budgets & reporting
- [ ] Run at concurrency steps (1, 5, 10, 25, 50 cashiers); export CSV + HTML.
- [ ] Record baseline p50/p95/p99, RPS, error ratio per scenario in the runbook.
- [ ] Define observational budgets (thresholds to be promoted later).

## Phase 5 — Docs & CI
- [ ] `locust/README.md` runbook: prerequisites (seeded tenant/terminal/cashier), env vars, commands.
- [ ] Optional manual-dispatch CI perf job (non-blocking) uploading stats artifacts.

## Phase 6 — E2E base refresh (alignment)
- [ ] Confirm the reused `tch_e2e` helpers (client/auth/data_factory) are current; update any drift so
      correctness e2e and Locust share one layer.
