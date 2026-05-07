# OpenSpec Change — simplify-draw-processing-schedulers

## Why

The current draw/result schedulers are too difficult to reason about for MVP. They mix several responsibilities:

- draw generation
- opening draws
- closing draws
- fetching external provider results
- applying global results to tenant draws
- settling tickets
- runtime gates
- per-slot time windows
- manual/debug behavior

This makes local testing difficult and makes the operational story hard to explain to clients.

The MVP policy should be simpler:

> Schedulers trigger simple idempotent commands in predictable windows. Business correctness belongs in handlers. Exceptional cases are handled through Ops endpoints with `force + reason + audit`.

## What changes

Introduce a simplified scheduler policy:

1. **Generate** draws daily at 05:00 for the next 7 days.
2. **Open today** draws repeatedly during the morning window, using `draw_channel.sales_open_time`.
3. **Close** OPEN draws automatically when their generated `cutoff_at` snapshot is reached.
4. **Fetch** external results after `drawTime + 5 minutes`, retry every 5/10 minutes, stop after a configurable window.
5. **Apply** results after `drawTime + 10 minutes`, retry every 30 minutes, continue longer than fetch.
6. **Settle** tickets after `drawTime + 20 minutes`, retry every 30 minutes, continue longer than apply.
7. Keep Ops endpoints for manual/force execution of every step.

## Out of scope

- Rebuilding draw/result domain models.
- Changing result-slot/provider mapping.
- Changing settlement calculation rules.
- Replacing existing command handlers if they can be reused safely.
- Building the web Ops UI.

## Impact

- Schedulers become easier to test and explain.
- Runtime behavior becomes predictable.
- Ops/manual paths remain powerful for debug and late provider results.
- Command handlers must remain idempotent and enforce invariants.

## High-level target

```text
05:00  generate next 7 days
04:00-10:00 every 5 min  open scheduled draws when channel sales_open_time is due

Every 5 minutes:
  close draws where cutoff_at <= now
  fetch due results
  apply available results
  settle resulted draws
```

Each step has its own due window and retry interval.
