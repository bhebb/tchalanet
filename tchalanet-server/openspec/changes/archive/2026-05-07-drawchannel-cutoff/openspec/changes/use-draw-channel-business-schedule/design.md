# Design

## Boundary model

### `result_slot`

Global external result slot.

Owns provider/result metadata:

- `slot_key`
- `provider`
- `draw_time`
- `timezone`
- `source_cfg`
- `projection_cfg`
- `active`

It is used by:

- external result fetch;
- public draw results;
- next expected result time/countdown.

It MUST NOT contain tenant sales rules.

### `draw_channel`

Tenant-scoped commercial channel for selling a `result_slot`.

Owns tenant/channel selling configuration:

- `tenant_id`
- `result_slot_id`
- `code`
- `name` / label
- `active`
- `draw_time` snapshot/override
- `timezone` snapshot/override
- `sales_open_time`
- `cutoff_sec`
- `days_of_week`

It is used by draw generation and sales opening.

### `draw`

Concrete generated occurrence.

Owns snapshots:

- `scheduled_at`
- `cutoff_at`
- `status`
- `draw_result_id`

Once generated, sell and close use `draw.cutoff_at`. They should not read live `draw_channel.cutoff_sec`.

`draw.opened_at` remains the actual transition timestamp. It is not the configured opening
time.

## Generation rule

For every active tenant `draw_channel` and eligible date:

```text
scheduledAt = drawDate + draw_channel.draw_time + draw_channel.timezone
cutoffAt    = scheduledAt - draw_channel.cutoff_sec
```

The generated `draw` stores these snapshots.

## Open rule

The open scheduler runs repeatedly during the morning window.

```text
open SCHEDULED draws for each channel local today
when localTime(now, draw_channel.timezone) >= coalesce(draw_channel.sales_open_time, defaultSalesOpenTime)
and now < draw.cutoff_at
```

The default MVP fallback is `05:30`. `draw_channel.sales_open_time` is tenant/channel
commercial configuration. `draw.opened_at` is set only when the draw actually transitions
to `OPEN`.

## Sell rule

`SellTicketCommand` must consider a draw sellable only when:

```text
draw.status = OPEN
now < draw.cutoffAt
```

This protects sales even if the close scheduler runs late.

## Close rule

The close scheduler/handler is simple:

```text
close OPEN draws where cutoff_at <= now
```

Different tenant/channel cutoffs are already represented by each draw's `cutoff_at`.

## Result pipeline boundary

Fetch/apply/settle keep their previous conceptual boundary:

- fetch uses global `result_slot` and creates/upserts global `draw_result`;
- apply attaches global `draw_result` to tenant draws using `draw_channel.result_slot_id` + `draw_date`;
- settle processes tenant draws that are already `RESULTED`.

Fetch timing remains based on `result_slot.draw_time`, not `draw_channel.cutoff_sec`.

## Provisioning rule

When a tenant is created or initialized:

1. create/provision `draw_channel` rows from active/default `result_slot` rows or a tenant template;
2. copy `draw_time` and `timezone` from `result_slot` by default;
3. set `cutoff_sec` from template/default;
4. set `sales_open_time` from template/default, defaulting to `05:30`;
5. set `active` according to template/default;
6. scheduler or Ops generation creates actual `draw` rows.

No new migrations are required if existing columns are already present.
