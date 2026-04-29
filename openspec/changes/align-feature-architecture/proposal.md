## Why

Several existing backend features predate the strict layer ownership rules. `features/` must stay an orchestration/BFF layer, while domain state, command handlers, event listeners, and persistent aggregates belong in the owning `core/` domain.

`features.news` is intentionally documented as a temporary/external aggregation BFF. `features.reporting` is a useful model for read-only cross-domain aggregation. `features.stats`, `features.publicdraw`, and `features.ops` need explicit alignment or documented exceptions.

## What Changes

- Move stats aggregate write-side ownership out of `features.stats` and into a core-owned package.
- Move ops refresh command orchestration out of `features.ops` and into `core.drawresult`, matching `DOMAIN_DRAWRESULT.md`.
- Keep `features.publicdraw` as a public BFF only when it is read-only, and document any projection SQL exception explicitly.
- Update near-code feature documentation so deviations are justified and bounded.

## Context

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `tchalanet-server/docs/ARCHITECTURE.md`
- `tchalanet-server/src/main/java/com/tchalanet/server/features/reporting/FEATURE_REPORTING.md`
- `tchalanet-server/src/main/java/com/tchalanet/server/features/news/FEATURE_NEWS.md`
