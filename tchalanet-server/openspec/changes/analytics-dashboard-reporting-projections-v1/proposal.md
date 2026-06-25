# Change: Analytics Dashboard Reporting Projections V1

## Why

Dashboards, PageModel widgets and reporting screens need the same business metrics. Today the
platform dashboard projection exposes only a summary and top tenants, while tenant analytics has a
partial daily projection and an empty game breakdown. This pushes consumers toward one-off
aggregation and makes charts hard to wire safely.

Payout analytics also needs a clearer source event. There is no payout table anymore; the source of
truth is the ticket settlement lifecycle. Analytics must therefore consume ticket lifecycle events
when a winning ticket is created, paid, or reversed.

Financial dashboard metrics must also come from sale-time snapshots. Odds, commissions, fees and
promotions are configurable, so analytics cannot recalculate historical tickets from the current
tenant or seller-terminal settings.

## What

- Extend `core.analytics.api` dashboard views so they can power dashboards and reports:
  - platform summary;
  - daily trend points;
  - game breakdown contract;
  - top tenants.
- Keep `core.analytics` as the only source for dashboard/reporting metrics.
- Add sales public events for payout payment and reversal.
- Project:
  - winning settlement creation into `winningsCalculated`;
  - payout paid into `payoutsPaid`;
  - payout reversed as a negative `payoutsPaid` delta for V1.
- Add command handlers for existing payout commands if they are not wired yet.
- Resolve ticket-line odds through `core.pricing` using seller-terminal override before tenant
  default, then snapshot the effective odds on the sale line.
- Document the next analytics projection slice for seller commissions, charge payer totals and
  promotion-funded/free-play metrics.

## Impact

- `features.pagemodel` and `features.reporting` consume `core.analytics.api` only.
- PageModel chart widgets can be wired later without Angular-side API orchestration.
- No new payout table is introduced.
- No Flyway migration in this slice unless a later task adds explicit `payouts_reversed` columns.
- No recomputation of historical commissions or winnings from current pricing/commission settings.

## Non-goals

- No frontend template wiring for platform charts until `gameBreakdown` is populated.
- No replacement of all reporting endpoints in this slice.
- No financial ledger implementation.
