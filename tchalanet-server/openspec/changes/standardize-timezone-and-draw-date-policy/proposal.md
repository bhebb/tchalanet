# Change: standardize-timezone-and-draw-date-policy

## Status

Proposed for implementation.

## Summary

Standardize all date/time handling for draw channels, generated draws, result slots, schedulers, sales cutoff checks, and offline sync acceptance.

The goal is to remove ambiguity between:

- UTC/server time;
- tenant/user timezone, often `America/Port-au-Prince`;
- provider/result-slot timezone, often `America/New_York`;
- draw-channel commercial date/time;
- persisted business event timestamps.

## Problem

Tchalanet currently has several time semantics that can be confused:

1. A draw channel defines a commercial draw time, not a persisted moment.
2. A tenant dashboard uses tenant-local “today”.
3. Result providers publish or imply result occurrence times in provider/slot timezone.
4. The server/JVM timezone is not a business timezone.
5. Haiti, UTC, and New York can disagree around midnight.
6. DST transitions can make naive `LocalDateTime` logic unsafe.
7. Offline sales need strict cutoff validation using real instants, not device-local claims.

If this is not standardized now, the most likely bugs are:

- sales accepted after cutoff;
- sales rejected before cutoff;
- draw generated for the wrong commercial date;
- result applied to the wrong tenant draw;
- scheduler opens/closes the wrong draw near midnight;
- offline submissions accepted for a draw already closed or resulted;
- dashboard “today” showing the wrong set of draws.

## Decision

### D1. Business event moments are `Instant`

Persist and expose all real business event moments as `Instant`.

Examples:

- `createdAt`
- `updatedAt`
- `soldAt`
- `openedAt`
- `closedAt`
- `scheduledAt`
- `cutoffAt`
- `occurredAt`
- `resultedAt`
- `syncedAt`
- `paidAt`

`LocalDateTime` is forbidden for persisted business event moments.

### D2. Draw date is channel-local commercial date

`draw.draw_date` means:

```text
the commercial draw date in draw_channel.timezone
```

It is not UTC date, not server date, and not tenant date by default.

### D3. Draw channel stores schedule definition

A draw channel stores calendar semantics:

```text
LocalTime drawTime
ZoneId timezone
Duration cutoffBeforeDraw / cutoffSeconds
Optional<LocalTime> salesOpenTime
```

Generated draws store resolved moments:

```text
scheduledAt = ZonedDateTime.of(drawDate, drawTime, channelZone).toInstant()
cutoffAt    = scheduledAt - cutoffBeforeDraw
```

### D4. Tenant timezone is for tenant-centric views

Tenant timezone is used for:

- tenant “today”;
- tenant dashboards;
- tenant reports;
- tenant-local calendar filtering.

It must not override provider/slot/channel timezone in result or draw calculations.

### D5. Provider/result-slot timezone is explicit

Result-slot/provider calculations must receive an explicit `ZoneId`.

They must not mutate request context timezone.

### D6. Comparisons are made with `Instant`

Critical gates compare `Instant` to `Instant`:

```text
now < cutoffAt        -> sale allowed by cutoff gate
now >= cutoffAt       -> sale rejected by cutoff gate
cutoffAt <= now       -> draw eligible to close
```

### D7. All `now()` comes from injected `Clock` or `TchTimeProvider`

No `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `new Date()`, or `ZoneId.systemDefault()` in business logic.

## Scope

In scope:

- `common.time` or equivalent shared time helpers;
- draw channel schedule calculation;
- generated draw snapshot fields;
- result occurred-at resolution;
- draw lifecycle scheduler open/close windows;
- draw result fetch/apply windows;
- sales cutoff validation;
- offline sync acceptance gates;
- regression tests for critical timezone cases;
- ArchUnit/static checks against unsafe temporal types.

Out of scope:

- changing external provider APIs;
- changing frontend display formatting beyond consuming stable UTC instants;
- full outbox/eventing changes;
- replacing all historical database columns in this change unless required for correctness.

## Impacted modules

```text
common.time                 new helper abstractions
core.draw                   generation/open/close schedule semantics
core.drawresult             occurredAt resolver and fetch/apply windows
core.sales                  sell cutoff gate and ticket timestamps
core.offlinesync            offline acceptance/rejection gates
features.cashier            no business logic; may display server computed instants/views
batch/scheduler             use helper methods and explicit zones
```

## Non-negotiable implementation rules

- No business event timestamp may be represented with `LocalDateTime`.
- No business code may depend on JVM/system default timezone.
- Draw generation must use draw channel timezone.
- Result occurred-at resolution must use result-slot/provider timezone.
- Tenant “today” must use tenant timezone.
- Cutoff and acceptance gates must compare `Instant`s.
- Offline submissions must never trust device timezone as source of truth.
