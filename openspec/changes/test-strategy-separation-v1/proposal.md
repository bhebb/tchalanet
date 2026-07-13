# OpenSpec Change — Test Strategy & Layer Separation V1

## Status

Proposed — 2026-07-13

## Why

Tchalanet has four test investments (Java unit, Java Spring integration with
Testcontainers, Python E2E, Python Locust load) plus web Playwright, but **no
written contract for what each layer owns**. Without it:

- the combinatorial sell matrix (`maryaj × limit × override`) risks being
  re-tested at every layer (slow, redundant, flaky);
- the API contract (ProblemDetail, idempotency-key) gets asserted twice — Java
  integration and Python E2E — with parallel maintenance;
- new tests land wherever the author is comfortable, not where the risk lives.

This change is the **cross-project coordination contract**. It does not add test
code; it defines the boundary rule and the per-domain allocation so the four
existing test changes stop overlapping.

## Decision (locked)

- **Unit** → Java (JUnit + AssertJ, no Spring, no Docker).
- **Integration** → Java + Testcontainers Postgres (in-build, runs on every PR).
- **E2E** → Python (`testing/e2e`, full deployed stack: API + PG + Keycloak + edge).
- **Load** → Python Locust (`testing/e2e/loadtest`, capacity/latency only).
- **Web** → Playwright (`tchalanet-web/apps/web-e2e`, browser flows) — outside
  this backend pyramid, referenced for completeness.

## The boundary rule (single source of truth)

> **Pure decision/calculation** → Unit ·
> **needs real DB / transaction / RLS / idempotency-store, but one seeded
> endpoint suffices** → Integration ·
> **needs the whole stack + real auth + multiple client roles** → E2E.

Anti-duplication: **a permutation is asserted once (Unit)**. Integration takes
**one representative** per branch. E2E takes **the** happy path. No layer repeats
what the layer below already proved.

## What Changes

- Add this contract + the per-domain allocation (see `design.md`).
- Align the four existing changes to it (see `tasks.md`): remove permutations
  from integration that Unit now owns; keep E2E to scenarios, not contract
  re-assertions.

## Impact

- No production runtime change.
- `tchalanet-server/openspec/changes/{unit-coverage-critical-domains-v1,
  spring-integration-business-flows-v1, e2e-business-runtime-v1,
  perf-load-testing-locust-v1}` reference this contract.
- `docs/conventions/testing.md` (backend) points to the boundary rule.

## Non-goals

- No new test code in this change (it is a contract).
- No blocking coverage gate (tracked per layer, later).
- No change to the chosen tools per layer.
