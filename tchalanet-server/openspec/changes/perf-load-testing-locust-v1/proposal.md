# Change: perf-load-testing-locust-v1

## Why

The Python e2e suite (`testing/e2e/tch_e2e`) proves the POS sell path is **correct**, but nothing
proves it holds under **load**. We have no repeatable way to answer: how many concurrent cashiers
can sell, what is the p95 latency of a sell, where does the sell/prepare/confirm path degrade, and
did a change (e.g. the L1/L2 cache work) actually move read latency.

Locust (Python, `httpx`-compatible) lets us drive that load while **reusing the existing e2e
concepts** — `tch_e2e.client.ApiClient`, auth, `data_factory`, `scenario_world`, `ticket_matrix` —
so a load scenario is the same business flow as the correctness tests, not a parallel re-implementation.

Scope target: a cashier selling **5–10 tickets** per basket, repeated by N virtual cashiers.

## What Changes

- Add Locust to the `testing/e2e` Python project (dependency + `locust/` package).
- A `locustfile` whose `User` tasks drive the real sell flow (`prepare → confirm → sell`) through
  `tch_e2e.ApiClient`, with a 5–10 line ticket basket built via `data_factory` / `ticket_matrix`.
- Load scenarios: single-ticket smoke, 5–10 ticket basket, mixed read (POS draws / dashboard) +
  write (sell) mix, ramped concurrency.
- Performance budgets (p50/p95/p99 latency, error-rate, RPS) recorded per scenario, run locally and
  on-demand (not a blocking CI gate initially).
- Align the reused e2e helpers so load and correctness share one client/auth/data layer.

## Impact

- `tchalanet-server/testing/e2e/` — new `locust` dependency, `locust/` package, run scripts, README.
- Docs: a perf runbook (how to run, read results, seeded data prerequisites).
- Optional CI: an on-demand perf job (manual trigger), non-blocking.
- **No production code changes.** Runs against a deployed/seeded environment, never prod.

## Non-goals

- Replacing the correctness e2e — Locust measures capacity/latency, not business assertions.
- A blocking CI performance gate (start observational; gate later once baselines are stable).
- Distributed / multi-worker load and production-scale traffic (single-node first).
- Load against real external providers (Ohio/Haiti) — provider calls are out of the sell hot path
  or stubbed.
