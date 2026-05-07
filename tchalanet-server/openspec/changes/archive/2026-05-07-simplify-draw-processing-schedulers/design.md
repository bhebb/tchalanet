# Design — Simplified Draw Processing Schedulers

## Principle

Schedulers must stay thin. They should:

- check global active flags and gates;
- identify coarse due candidates;
- call commands;
- log compact summaries;
- never encode complex business rules.

Handlers must enforce:

- idempotence;
- valid status transitions;
- tenant/RLS safety;
- no double settlement;
- no silent overwrite of attached results;
- audit and events after commit.

## Proposed configuration

Use one clear root for draw processing schedulers.

```yaml
tch:
  draw:
    scheduler:
      active: true

      generate:
        active: true
        cron: '0 0 5 * * *'
        days-ahead: 7
        max-tenants-per-run: 1000

      open-today:
        active: true
        cron: '0 */5 4-10 * * *'
        default-sales-open-time: '05:30'
        max-items-per-run: 10000

      processing:
        active: true
        cron: '0 */5 * * * *'
        timezone: 'America/New_York'

        close:
          active: true
          max-items-per-tick: 500

        fetch:
          active: true
          start-minutes-after-draw: 5
          retry-every-minutes: 10
          stop-minutes-after-draw: 240
          max-slots-per-tick: 10

        apply:
          active: true
          start-minutes-after-draw: 10
          retry-every-minutes: 30
          stop-minutes-after-draw: 720
          max-items-per-tick: 500

        settle:
          active: true
          start-minutes-after-draw: 20
          retry-every-minutes: 30
          stop-minutes-after-draw: 1440
          max-items-per-tick: 1000
```

### Naming rule

Prefer these names:

- `start-minutes-after-draw`
- `retry-every-minutes`
- `stop-minutes-after-draw`
- `default-sales-open-time`

Avoid ambiguous names such as `cooldown`, `due.min/max`, or `lookback` for MVP scheduler behavior.

## Gates

Keep independent gates:

```text
draw.generate
draw.open_today
draw.processing
draw.close
drawresult.fetch
drawresult.apply
sales.settle
```

A global processing gate may disable the whole processing tick, while per-step gates allow targeted stop.

## Generate policy

Runs daily at 05:00.

- Generate missing draws for all active tenants.
- Horizon: configured `days-ahead`, default 7.
- Idempotent: do not duplicate existing draws; do not mutate existing non-generated data.

## Open today policy

Runs every 5 minutes from 04:00 to 10:00 UTC by default.

- Open only SCHEDULED draws for the current local draw date of each channel.
- Use `draw_channel.sales_open_time` when present.
- Fall back to `tch.draw.scheduler.open-today.default-sales-open-time`, default `05:30`.
- Do not open a draw if `now >= draw.cutoff_at`.
- Skip OPEN/CLOSED/RESULTED/SETTLED/CANCELED/ARCHIVED.
- Set `draw.opened_at` to the actual transition timestamp.
- Ops may open manually if needed.

## Close policy

Close is intentionally simple.

A draw is close-due when:

```text
draw.status = OPEN
draw.cutoffAt <= now
```

The scheduler should use already generated draw data (`cutoffAt`) and should not recompute provider, slot, or channel cutoff rules.

Ops may close due draws manually. Targeted force-close is part of the Ops hardening slice and must keep reason + audit.

## Fetch policy

Fetch is global and result-slot driven.

For each active result slot/date, calculate:

```text
occurredAt = drawDate + result_slot.draw_time + result_slot.timezone
age = now - occurredAt
```

Fetch due if:

```text
age >= fetch.startMinutesAfterDraw
age <= fetch.stopMinutesAfterDraw
retry interval OK for slot/date
result is not already CONFIRMED
```

Default MVP:

```text
start = drawTime + 5 min
retry = every 10 min
stop = drawTime + 4h
```

Fetch writes/upserts global `draw_result` only. It must not attach results to tenant draws.

## Apply policy

Apply attaches existing global `draw_result` records to tenant draws.

Apply due if:

```text
age >= apply.startMinutesAfterDraw
age <= apply.stopMinutesAfterDraw
retry interval OK for slot/date
matching draw_result exists
CLOSED tenant draws without draw_result_id exist
```

Default MVP:

```text
start = drawTime + 10 min
retry = every 30 min
stop = drawTime + 12h
```

Apply must never fetch. Apply must never silently overwrite an existing `draw_result_id`.

## Settle policy

Settle calculates/settles tickets after a draw is RESULTED.

Settle due if:

```text
age >= settle.startMinutesAfterDraw
age <= settle.stopMinutesAfterDraw
retry interval OK for slot/date
draw.status = RESULTED
draw.draw_result_id exists
tickets not settled exist
```

Default MVP:

```text
start = drawTime + 20 min
retry = every 30 min
stop = drawTime + 24h
```

Settle must be idempotent and must never double settle a ticket.

## Manual Ops policy

Every automatic step must have an Ops path.

Common fields:

```json
{
  "tenantId": null,
  "drawDate": "2026-05-06",
  "slotKeys": ["NY_MID"],
  "drawIds": null,
  "force": false,
  "dryRun": false,
  "reason": "manual operation",
  "maxItems": 500
}
```

Fetch-specific fields:

```json
{
  "drawDate": "2026-05-06",
  "slotKeys": ["NY_MID"],
  "drawTimeOverride": null,
  "force": true,
  "dryRun": false,
  "skipRetry": true,
  "reason": "manual fetch",
  "maxSlots": 10
}
```

Rules:

- `force=true` bypasses automatic time windows/retry gates.
- `force=true` requires `reason`.
- Forced actions must be audited.
- Force must not bypass critical invariants such as no double settlement or no invalid status transition.

## Processing tick order

The processing tick should run steps in this order:

```text
1. close due draws
2. fetch due external results
3. apply available results
4. settle resulted draws
```

Each step should be individually gated and protected by idempotent command handlers.

## Logging

Each tick should log:

- active/gate status;
- number of candidates;
- number processed;
- skipped reasons at debug level;
- errors per item without killing the whole tick.

Avoid noisy logs for expected skips.
