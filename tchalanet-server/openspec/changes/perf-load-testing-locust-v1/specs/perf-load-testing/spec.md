# perf-load-testing Spec

## ADDED Requirements

### Requirement: Load tests reuse the e2e client and data layer

Locust load scenarios SHALL drive the system through the same `tch_e2e` client, auth, and data
builders used by the correctness e2e suite — not a parallel API re-implementation.

#### Scenario: Cashier session under load

- **GIVEN** a Locust `CashierUser`
- **WHEN** it starts
- **THEN** it authenticates via `tch_e2e.auth` and issues requests through the shared `ApiClient`
  (op-context headers preserved), so the flow matches the correctness e2e

### Requirement: Sell load scenario covers a 5–10 ticket basket

A load scenario SHALL exercise the real POS sell path (`prepare → confirm → sell`) with a basket of
5 to 10 ticket lines, and report the latency of each step.

#### Scenario: Selling a multi-line basket repeatedly

- **GIVEN** a seeded tenant, terminal and cashier
- **WHEN** the sell task runs
- **THEN** it prepares, confirms and sells a randomized 5–10 line ticket, with an idempotency key,
  and prepare/confirm/sell latencies are reported separately

#### Scenario: Retry does not double-sell

- **GIVEN** a sell request with an idempotency key
- **WHEN** it is retried under load
- **THEN** the outcome is idempotent (no duplicate ticket)

### Requirement: Load never targets production

The load runner SHALL refuse to run against a production host.

#### Scenario: Production host is rejected

- **GIVEN** a host that matches the production allowlist deny rule
- **WHEN** the locustfile starts
- **THEN** it aborts before generating traffic

### Requirement: Per-scenario performance metrics are captured

Each scenario run SHALL capture per-request latency percentiles (p50/p95/p99), throughput (RPS) and
error ratio, exportable for baseline tracking.

#### Scenario: Baseline export

- **GIVEN** a headless run at a fixed concurrency step
- **WHEN** the run completes
- **THEN** CSV/HTML stats are produced with p50/p95/p99, RPS and failure ratio per request type

### Requirement: Read paths are exercised to measure cache effect

The load mix SHALL include cached read paths (POS draws, dashboard overview) so the L1/L2 cache
effect on latency is observable under load.

#### Scenario: Read-heavy profile

- **GIVEN** a read-heavy scenario
- **WHEN** it runs after cache warm-up
- **THEN** POS draws / dashboard read latencies are reported and comparable across runs
