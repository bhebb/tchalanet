# Ops Sales Simulation

## Why

QA needs a repeatable way to load a tenant with realistic ticket volume before applying or overriding draw results. The scenario must prove multi-seller participation on the same draw and keep the normal sales rules active: seller terminal gates, draw lifecycle gates, tenant game/pricing configuration, limits, commissions, overrides, and Maryaj gratis.

## What Changes

- Add a platform ops endpoint to plan or execute synthetic sales for selected tenants, draws, and seller terminals.
- Generate deterministic single-line tickets for Borlette, Maryaj, Loto 3, Loto 4, and Loto 5 using a caller-provided seed.
- Execute each sale through the existing `PrepareSaleCommand` and `ConfirmPreparedSaleCommand` pipeline.
- Return per-draw/per-seller/per-game diagnostics with accepted, rejected, and failed counts.
- Keep draw-result manual/override/apply on the existing ops endpoints; this change only creates ticket volume.

## Non-Goals

- No direct writes to sales, ticket, exposure, analytics, or settlement tables.
- No bypass for closed, future, or past draws in V0.
- No asynchronous long-running job orchestration in V0.
- No new persistence or Flyway migration.

## Safety

- Endpoint is platform-only and guarded by `SUPER_ADMIN`.
- `dryRun=true` creates a deterministic plan without preparing sales.
- `dryRun=false` requires an explicit reason and creates real tickets.
- A max-ticket cap prevents accidental large loads.
- Every execution uses seller-terminal context so business rules are enforced by production handlers.
