# Batch And Scheduler Conventions

This document is the backend operational reference for scheduled jobs.

Schedulers stay thin: they check active flags and gates, bind the required context, call commands or
batch jobs, and log summaries. Business correctness belongs in handlers and batch steps.

## Draw Scheduler Policy

Draw processing uses one configuration root:

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
        timezone: America/New_York

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

## Draw Lifecycle Schedulers

`DrawLifeCycleTickScheduler` owns only:

- generate next draws;
- open today's scheduled draws.

### Generate

Default: daily at `05:00`.

The scheduler loops over active tenants and calls `GenerateDrawsForRangeCommand`.
Generation is idempotent and preserves the unique key `(tenant_id, draw_channel_id, draw_date)`.

Generated draw snapshots:

- `scheduled_at` comes from `draw_channel.draw_time + draw_channel.timezone`;
- `cutoff_at` comes from `scheduled_at - draw_channel.cutoff_sec`;
- `status` starts as `SCHEDULED`, except forced past backfills may be closed.

### Open Today

Default: every 5 minutes from `04:00` to `10:00` UTC.

The scheduler loops over active tenants and calls `OpenTodayDrawsCommand`.

Open eligibility:

```text
draw.status = SCHEDULED
draw.draw_date = channel-local date(now)
channel-local time(now) >= coalesce(draw_channel.sales_open_time, defaultSalesOpenTime)
draw.cutoff_at > now
draw.locked = false
```

`draw_channel.sales_open_time` is tenant/channel commercial configuration. If it is absent,
`tch.draw.scheduler.open-today.default-sales-open-time` is used. `draw.opened_at` is the actual
transition timestamp, not the configured opening time.

## Draw Processing Scheduler

`DrawProcessingTickScheduler` runs the repeated processing pipeline:

```text
close -> fetch -> apply -> settle
```

Each step has its own active flag and batch gate. A step failure is logged and counted; later safe
steps may still run.

### Close

Close is tenant-scoped and uses generated draw snapshots only.

Eligibility:

```text
draw.status = OPEN
draw.cutoff_at <= now
draw.locked = false
```

There is no global `minutes-before-draw` rule. Tenant/channel cutoff policy is already captured in
`draw.cutoff_at` during generation.

### Fetch

Fetch is global and result-slot driven.

Eligibility is calculated from:

```text
occurredAt = drawDate + result_slot.draw_time + result_slot.timezone
```

Fetch starts after `start-minutes-after-draw`, retries by `retry-every-minutes`, and stops after
`stop-minutes-after-draw`. It writes/upserts global `draw_result` records only.

Fetch must not require tenant context and must not attach results to tenant draws.

### Apply

Apply is tenant-scoped. It attaches existing global `draw_result` rows to matching tenant draws.

Apply must not fetch provider results and must not overwrite an existing `draw.draw_result_id`.
Corrections go through explicit Ops override/correction flows.

### Settle

Settle is tenant-scoped and runs after a draw is `RESULTED`.

Settlement must be idempotent and must never double payout a ticket. Forced Ops paths may bypass
time windows, but they must not bypass settlement invariants.

## Gates

Use the existing batch gates:

```text
draw.generate
draw.open_today
draw.processing
draw.close
drawresult.fetch
drawresult.apply
sales.settle
```

The global processing gate can disable the full repeated pipeline. Per-step gates allow targeted
pause/resume.

## Ops Paths

Automatic scheduler operations have matching Ops endpoints under `/platform/ops/**` where available.

Forced operations require:

- authorization;
- `force=true`;
- non-blank `reason`;
- audit logging.

Force can bypass scheduler timing windows and retry intervals. It must not bypass core invariants,
RLS, authentication, valid state transitions, or idempotency protections.
