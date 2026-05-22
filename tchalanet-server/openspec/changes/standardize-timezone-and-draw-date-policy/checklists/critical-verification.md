# Critical Verification Checklist

This checklist focuses on cases most likely to cause financial, settlement, or operational defects.

## A. Draw generation

### A1. UTC date differs from channel-local draw date

Input:

```text
channelZone = America/New_York
drawDate = 2026-05-20
drawTime = 22:30
cutoffBeforeDraw = 5 minutes
```

Expected:

```text
draw.drawDate = 2026-05-20
scheduledAt = 2026-05-20T22:30 America/New_York -> Instant
cutoffAt = scheduledAt - 5 minutes
```

Verify:

- [ ] `drawDate` is not overwritten with UTC date.
- [ ] `scheduledAt` is persisted as `Instant`/`timestamptz`.
- [ ] `cutoffAt` is persisted as `Instant`/`timestamptz`.
- [ ] unique key remains `(tenant_id, draw_channel_id, draw_date)`.

## B. Sale cutoff boundary

Input:

```text
cutoffAt = 2026-05-21T02:25:00Z
```

Cases:

| now | expected |
|---|---|
| `2026-05-21T02:24:59.999Z` | accepted by cutoff gate |
| `2026-05-21T02:25:00Z` | rejected |
| `2026-05-21T02:25:00.001Z` | rejected |

Verify:

- [ ] gate uses `now.isBefore(cutoffAt)`.
- [ ] no local time comparison.
- [ ] no server timezone involved.

## C. Open-today scheduler

Input:

```text
channelZone = America/New_York
salesOpenTime = 05:30
now = fixed Instant near midnight/day boundary
```

Expected:

- [ ] scheduler computes channel-local date from channel zone.
- [ ] scheduler computes channel-local time from channel zone.
- [ ] scheduler does not use tenant timezone unless the channel specifically uses tenant timezone by configuration.
- [ ] scheduler does not use JVM timezone.

## D. Close scheduler

Input:

```text
draw.status = OPEN
cutoffAt = fixed Instant
```

Expected:

- [ ] `now < cutoffAt` -> not close.
- [ ] `now == cutoffAt` -> close eligible.
- [ ] `now > cutoffAt` -> close eligible.

## E. Result occurredAt

Cases:

1. Provider gives `occurredAt` instant.
2. Provider omits `occurredAt`; slot has `drawDate + drawTime + slotZone`.
3. Provider omits `occurredAt`; slot schedule incomplete.

Expected:

- [ ] case 1 uses provider instant.
- [ ] case 2 computes via slot zone.
- [ ] case 3 fallback is logged/warned.
- [ ] tenant timezone is not used for provider/slot occurrence.

## F. Apply result to tenant draw

Verify:

- [ ] apply does not fetch provider results.
- [ ] apply does not overwrite existing `drawResultId`.
- [ ] apply does not match by UTC date when draw date is channel-local.
- [ ] correction path is explicit Ops/correction flow.

## G. Offline sync cutoff

Cases:

| sync time | trusted sale instant | expected |
|---|---|---|
| after cutoff | before cutoff | can pass cutoff gate if grant/signature valid |
| after cutoff | equal cutoff | reject/review |
| after cutoff | after cutoff | reject/review |
| after cutoff | missing/untrusted | reject/review |

Verify:

- [ ] device timezone is metadata only.
- [ ] target draw cutoff is server-generated `Instant`.
- [ ] late sync after result does not bypass cutoff/result invariants.

## H. DST regression

Use at least one test around `America/New_York` DST transition.

Verify:

- [ ] schedule calculation uses `ZonedDateTime.of(..., zone)`.
- [ ] ambiguous/nonexistent local times have explicit behavior or are avoided by configured draw times.
- [ ] tests do not use JVM default timezone.

## I. Static safety

Search or ArchUnit checks:

- [ ] no `LocalDateTime` in persisted business event models.
- [ ] no `Instant.now()` in business code.
- [ ] no `LocalDate.now()` in business code.
- [ ] no `ZoneId.systemDefault()` in business code.
- [ ] no `new Date()` in business code.
