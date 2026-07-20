# Change: simplify-draw-scheduler-v0

## Why

The draw runtime contains an unused `windows` configuration and an opening path that follows each
provider's `sales_open_time`. For V0, operations need a predictable daily operating model: create
the day's draws once, open the eligible upcoming draws once for every active tenant, then close
only draws whose persisted cutoff has elapsed.

## What changes

- Remove the dormant draw scheduler window component and configuration.
- Run daily generation at 00:05 in the configured operating timezone.
- Run one daily bulk opening at 00:15 in that timezone, using the existing upcoming-draw query
  rather than a provider-local `sales_open_time` filter.
- Keep the bounded five-minute processing tick for close, fetch, reminder, apply, and settlement.
- Remove the unused `OpenTodayDraws` command path and its provider-local opening query.
- Make the batch job's allowed parameters describe the new explicit opening horizon and lag.

## Non-goals

- No durable operation queue is introduced in this change.
- No change to the sales cutoff gate: `confirm` remains authoritative after `cutoffAt`.
- No change to tenant-admin manual open/close commands or their audit policy.
- No change to result-fetch retry policy.

## Impact

`core.draw`, application scheduler configuration, the batch job registry, and scheduler
documentation/tests.
