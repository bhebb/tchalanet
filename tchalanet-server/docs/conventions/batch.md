# Batch And Scheduler Conventions

This document is the backend operational reference for scheduled jobs.

Schedulers stay thin: they check active flags and gates, compute the operational window/candidates,
launch registered Spring Batch jobs through `BatchJobStarter`, and log summaries. Business
correctness belongs in command handlers and batch steps.

Spring Batch is the single execution engine for recurring, long-running, restartable, or
scheduler-triggered operational work. Direct command execution from Ops is reserved for targeted
human actions such as manual result entry, result override, result confirmation, and cache clear.

## Draw Scheduler Policy

Draw processing uses one configuration root:

```yaml
tch:
  draw:
    scheduler:
      active: true
      timezone: America/Port-au-Prince

      generate:
        active: true
        cron: '0 5 0 * * *'
        days-ahead: 7
        max-tenants-per-run: 1000

      open:
        active: true
        cron: '0 15 0 * * *'
        lookahead-hours: 24
        lag-hours: 1
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
- open the day's eligible upcoming draws.

### Generate

Default: daily at `00:05` in `tch.draw.scheduler.timezone`.

The scheduler loops over active tenants and launches the `draw:lifecycle:generate` Spring Batch job.
Generation is idempotent and preserves the unique key `(tenant_id, draw_channel_id, draw_date)`.

Generated draw snapshots:

- `scheduled_at` comes from `draw_channel.draw_time + draw_channel.timezone`;
- `cutoff_at` comes from `scheduled_at - draw_channel.cutoff_sec`;
- `status` starts as `SCHEDULED`, except forced past backfills may be closed.

### Open Daily

Default: daily at `00:15` in `tch.draw.scheduler.timezone`.

The scheduler loops over active tenants and launches the `draw:lifecycle:open` Spring Batch job.

Open eligibility:

```text
draw.status = SCHEDULED
draw.scheduled_at is within [now - lagHours, now + lookaheadHours]
draw.cutoff_at > now
draw.locked = false
```

`draw.opened_at` is the actual transition timestamp. The provider calendar check still cancels a
scheduled draw with no provider occurrence instead of opening it.

## Draw Processing Scheduler

`DrawProcessingTickScheduler` runs the repeated processing pipeline:

```text
close -> fetch -> result-reminder -> apply -> settle
```

Each step has its own active flag and batch gate. A step failure is logged and counted; later safe
steps may still run.

`result-reminder` is part of this same tick. It creates a platform operational notification for a
manual slot missing its result after five minutes, or an automatic provider still missing its result
after one hour. It does not fetch, apply, or settle a result itself.

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
draw:lifecycle:generate
draw:lifecycle:open
draw:lifecycle:close
draw:lifecycle:settle
results:external:fetch
results:external:apply
catalog:search:reindex
```

The global processing gate can disable the full repeated pipeline. Per-step gates allow targeted
pause/resume.

## Ops Paths

Automatic scheduler operations have matching guided Ops endpoints under `/platform/ops/**` where
available. These endpoints launch the same registered Spring Batch jobs and return execution ids;
they do not call domain commands directly.

In V0, `refresh` is intentionally not a batch job or guided Ops action. Run `results:external:fetch`
first, then `results:external:apply` for the target tenant(s).

Forced operations require:

- authorization;
- `force=true`;
- non-blank `reason`;
- audit logging.

Force can bypass scheduler timing windows and retry intervals. It must not bypass core invariants,
RLS, authentication, valid state transitions, or idempotency protections.

## Canonical Scheduler Inventory

Do not add a scheduler merely because a task is periodic. Existing work is grouped by operational
ownership and each entry has one configuration root:

| Owner | Runtime entry point | Cadence | Configuration root | Purpose |
|---|---|---|---|---|
| Draw lifecycle | `DrawLifeCycleTickScheduler` | generate 00:05; open 00:15 | `tch.draw.scheduler` | Generates and opens tenant draws. |
| Draw processing | `DrawProcessingTickScheduler` | every 5 min | `tch.draw.scheduler.processing` | `close -> fetch -> result-reminder -> apply -> settle`. |
| Draw watchdog | `DrawProvisionalWatchdogScheduler` | every 15 min | `tch.draw.watchdog` | Detects provisional results stuck after application. |
| Sale preparation retention | `SalePreparationRetentionScheduler` | every 5 min | `tch.sales.preparation` | Expires and purges temporary preparation records. |
| Communication delivery | `OutboundMessageRetryScheduler` | every 30 sec | `tch.communication.dispatcher` | Retries durable outbound messages; it does not create notifications. |
| Analytics maintenance | `AnalyticsMaintenanceScheduler` | daily | `tch.analytics` | Retention only today; reconciliation is added here when its durable run model exists. |
| Public content | `PublicContentRefreshScheduler` | every 6 h | `tch.news.refresh` | Refreshes external RSS content. |
| Archive restore cleanup | `ArchiveRestoreCleanupScheduler` | daily | `tch.archive.restore-cleanup` | Cleans expired temporary restore runs. |
| Batch history | `BatchJobHistoryPurgeScheduler` | weekly | `tch.batch.history` | Purges Spring Batch execution history. |

The obsolete roots `tch.draw.lifecycle`, `tch.draw.settlement`, and
`tch.draw.results.scheduler` are intentionally unsupported. Draw scheduling is configured only
under `tch.draw.scheduler`; result fetch limits and notification policy remain under
`tch.draw.results`.

## In-App Notification Push

Private notifications use an authenticated server-sent-events refresh signal, not a polling loop:

```text
NotificationPublishedEvent after commit
  -> NotificationRealtimeStreamService
  -> SSE notification-change
  -> private notification store reloads latest items + unread count through REST
```

The REST endpoints remain the source of notification content. SSE carries no notification body, so
it does not leak another audience's notification and a reconnect cannot create duplicate UI items.
The web client uses `fetch` rather than native `EventSource` because authorization and support
tenant-override headers are required, and sends a fresh `X-Request-Id` for each connection. It
reconnects after transient failure with a bounded `5 s → 15 s → 30 s → 60 s` backoff; it must not
add a periodic unread-count poll while the stream is healthy.
