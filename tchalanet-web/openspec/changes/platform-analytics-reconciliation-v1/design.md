# Design - Platform Analytics Reconciliation Screen

## Placement and access

The screen lives under `Platform > Operations` at
`/app/platform/ops/analytics/reconciliation`. The backend enforces `SUPER_ADMIN`; the web route
does not infer or duplicate financial authorization.

It is a controlled operation, not a dashboard. It uses `tch-admin-page-shell` and
`tch-admin-section-card`; all colors, dimensions, breakpoints and typography come from the shared
console primitives and `--tch-*` tokens.

## Layout

```text
tch-admin-page-shell
  actions: refresh/reload of the last completed result only
  feedback: tch-section-error / tch-notice from tchMutation
  main
    target section
      tenant search select
      from / to business dates (maximum 90 days)
      Validate button
    result summary section
      terminal status, run id, selected scope, mismatch count
      if mismatch: Rebuild button
    mismatch table / mobile cards
  aside
    guarded rebuild summary and current constraints
```

The target remains editable after a result. Changing it invalidates the visible result until the
next validation so an operator cannot repair an old result against a new scope by mistake.

## Actions and feedback

`VALIDATE` is a one-shot `tchMutation` with no idempotency header. It can return `SUCCESS` or
`MISMATCH`; both are normal operational outcomes and are rendered as result states, not generic
errors.

`REBUILD_AND_VALIDATE` is a separate `tchMutation`, passes a generated idempotency key and starts
only after a Material confirmation dialog validates a non-empty repair reason. The dialog displays
tenant and date scope and warns that only selected tenant projections are replaced. The button is
disabled while pending. A successful repair immediately replaces the screen result with the
post-rebuild validation response.

HTTP/ProblemDetail failures use the shared `tchMutation` error mapping. Server-side validation
stays attached to the relevant form control where it can be mapped; all other errors remain in the
section feedback area. The page does not use `HttpClient` directly or hand-roll error conversion.

## Result details

The result is rendered only after an action succeeds. It includes:

- run id, completed time, tenant, inclusive date scope and mode;
- terminal status and mismatch count;
- per mismatch: projection, business date, optional draw and terminal ids, expected and observed
  metric snapshots;
- values in backend minor units displayed through the shared money formatter using the tenant
  currency once exposed by the tenant selector.

The result list is bounded by backend response policy. Desktop uses a compact table; mobile uses a
stacked card with projection/date first and financial differences below. A zero-mismatch result is
an explicit success state, never an empty table with no explanation.

## Test boundary

Playwright verifies route/access, target controls, validate command affordance and the result state
from a deterministic API fixture. Backend tests own source aggregation, repair transaction,
authorization and accounting correctness.
