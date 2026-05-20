# Design: Timezone and Draw Date Policy

## 1. Vocabulary

| Term | Meaning | Canonical type |
|---|---|---|
| Business event moment | A real point in time when something happened | `Instant` |
| Draw commercial date | The business date of a draw in the draw channel timezone | `LocalDate` |
| Draw scheduled time | Time-of-day configured for a channel | `LocalTime` |
| Draw/channel timezone | Timezone that owns the draw schedule | `ZoneId` |
| Tenant timezone | Timezone for tenant dashboards/reporting/calendar views | `ZoneId` |
| Result slot timezone | Timezone used by provider/slot occurrence rules | `ZoneId` |
| Server timezone | Runtime/JVM deployment timezone | Not business truth |

## 2. Type policy

### 2.1 Allowed

```java
Instant      // persisted/exposed event moments
LocalDate    // calendar-only business date
LocalTime    // time-of-day schedule definition
ZoneId       // explicit owner of a calendar interpretation
ZonedDateTime // calculation only, convert back to Instant
Duration     // offsets such as cutoffBeforeDraw
Clock        // source of now()
```

### 2.2 Restricted

`LocalDateTime` is allowed only for UI/query helper code that is:

- not persisted;
- not part of a business event;
- explicitly documented;
- converted using an explicit `ZoneId` before any business decision.

### 2.3 Forbidden in business logic

```java
Instant.now()
LocalDate.now()
LocalDateTime.now()
ZonedDateTime.now()
new Date()
Calendar.getInstance()
ZoneId.systemDefault()
```

Use injected `Clock` or `TchTimeProvider` instead.

## 3. Proposed shared API

### 3.1 Package

```text
common.time
```

`common` is acceptable because this is a technical primitive, not a workflow, not a table, and not a business policy.

### 3.2 `TchTimeProvider`

```java
package com.tchalanet.server.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface TchTimeProvider {
  Instant now();
  LocalDate today(ZoneId zoneId);
  ZonedDateTime nowAt(ZoneId zoneId);
  Clock clock();
}
```

### 3.3 Default implementation

```java
package com.tchalanet.server.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultTchTimeProvider implements TchTimeProvider {
  private final Clock clock;

  @Override
  public Instant now() {
    return clock.instant();
  }

  @Override
  public LocalDate today(ZoneId zoneId) {
    return LocalDate.now(clock.withZone(zoneId));
  }

  @Override
  public ZonedDateTime nowAt(ZoneId zoneId) {
    return ZonedDateTime.now(clock.withZone(zoneId));
  }

  @Override
  public Clock clock() {
    return clock;
  }
}
```

## 4. Draw scheduling

### 4.1 `DrawScheduleSnapshot`

```java
public record DrawScheduleSnapshot(
    LocalDate drawDate,
    LocalTime drawTime,
    ZoneId zoneId,
    Instant scheduledAt,
    Instant cutoffAt
) {}
```

### 4.2 `DrawScheduleCalculator`

Placement options:

- preferred: `core.draw.internal.domain.service.DrawScheduleCalculator` if pure and domain-owned;
- acceptable: `core.draw.internal.application.service.DrawSchedulePlanner` if it also loads channel data.

Pure calculator:

```java
public final class DrawScheduleCalculator {

  public DrawScheduleSnapshot compute(
      LocalDate drawDate,
      LocalTime drawTime,
      ZoneId zoneId,
      Duration cutoffBeforeDraw
  ) {
    var scheduledAt = ZonedDateTime.of(drawDate, drawTime, zoneId).toInstant();
    var cutoffAt = scheduledAt.minus(cutoffBeforeDraw);
    return new DrawScheduleSnapshot(drawDate, drawTime, zoneId, scheduledAt, cutoffAt);
  }
}
```

### 4.3 Generated draw fields

Generated tenant draw should contain at minimum:

```text
draw_date         date not null
scheduled_at      timestamptz not null
cutoff_at         timestamptz not null
opened_at         timestamptz null
closed_at         timestamptz null
resulted_at       timestamptz null
```

`draw_date` is channel-local commercial date.

## 5. Result occurred-at resolution

### 5.1 Resolver behavior

```text
1. If provider sends a valid Instant, use it.
2. Else compute from resultSlot.drawDate + resultSlot.drawTime + resultSlot.zoneId.
3. Else fallback to clock.instant(), but this fallback must be observable/logged.
```

### 5.2 API sketch

```java
public final class ResultOccurredAtResolver {
  public Instant resolve(
      Optional<Instant> providerOccurredAt,
      LocalDate drawDate,
      LocalTime drawTime,
      ZoneId slotZoneId,
      Clock clock
  ) {
    return providerOccurredAt.orElseGet(() ->
        ZonedDateTime.of(drawDate, drawTime, slotZoneId).toInstant()
    );
  }
}
```

## 6. Sales cutoff gate

### 6.1 Rule

```text
sale is cutoff-eligible iff now < draw.cutoffAt
```

Boundary decision:

```text
now == cutoffAt -> rejected
```

### 6.2 Handler usage

```java
var now = timeProvider.now();
if (!now.isBefore(draw.cutoffAt())) {
  throw ProblemRest.conflict("draw.cutoff_reached");
}
```

## 7. Scheduler semantics

### 7.1 Generate

Generation computes draw dates in the channel timezone.

```text
baseDate = timeProvider.today(channelZone)
for each date in requested range:
  scheduledAt = drawDate + drawTime + channelZone -> Instant
  cutoffAt = scheduledAt - cutoffBeforeDraw
```

### 7.2 Open today

Open eligibility uses channel-local date/time:

```text
draw.status = SCHEDULED
draw.draw_date = channel-local date(now)
channel-local time(now) >= coalesce(channel.salesOpenTime, defaultSalesOpenTime)
draw.cutoff_at > now
draw.locked = false
```

### 7.3 Close

Close eligibility uses persisted `cutoffAt`:

```text
draw.status = OPEN
draw.cutoff_at <= now
draw.locked = false
```

### 7.4 Fetch/apply/settle

Fetch is result-slot/provider driven. Apply/settle are tenant-scoped.

Fetch timing windows use:

```text
occurredAt = drawDate + resultSlot.drawTime + resultSlot.zoneId -> Instant
```

Apply must attach existing global results to tenant draws without recomputing tenant-local dates incorrectly.

## 8. Offline sync semantics

Offline sync must separate:

- device-claimed sale local time;
- device signed sale instant, if available and trusted;
- server received/synced instant;
- target draw cutoff instant.

### 8.1 Acceptance baseline

A submitted offline sale can be promoted only if:

```text
trustedSaleInstant < draw.cutoffAt
```

If trustedSaleInstant is missing or not verifiable, fallback policy must be conservative:

- reject; or
- route to admin review;
- never silently accept as if it were before cutoff.

### 8.2 Device timezone

Device timezone is metadata only. It is not source of truth for draw cutoff.

### 8.3 Late sync

If sync happens after cutoff or after result, the sale can still be accepted only when cryptographic/offline grant rules prove that the sale occurred before cutoff and within the offline grant policy.

Otherwise:

```text
offline_submission.status = REJECTED or NEEDS_REVIEW
reason = OFFLINE_CUTOFF_UNVERIFIABLE or OFFLINE_CUTOFF_REACHED
```

## 9. Database guidance

### 9.1 PostgreSQL types

Use:

```text
timestamptz for Instant/date-time moments
date for LocalDate
time for LocalTime
varchar for ZoneId string, validated in application
integer/bigint for durations in seconds where practical
```

Avoid:

```text
timestamp without time zone for business event moments
```

### 9.2 Existing columns

If existing columns use `timestamp without time zone`, create a migration plan. Do not silently reinterpret them.

## 10. API guidance

Expose timestamps as ISO-8601 UTC instants:

```json
"scheduledAt": "2026-05-21T02:30:00Z"
```

Expose commercial date and timezone when the frontend needs calendar semantics:

```json
{
  "drawDate": "2026-05-20",
  "drawTime": "22:30:00",
  "drawZoneId": "America/New_York",
  "scheduledAt": "2026-05-21T02:30:00Z",
  "cutoffAt": "2026-05-21T02:25:00Z"
}
```

## 11. Observability

Log scheduler summaries with both instants and semantic zones:

```text
open_today tick now=2026-05-21T01:00:00Z channelZone=America/New_York channelDate=2026-05-20 channelTime=21:00 openable=...
```

Avoid logs that show only `LocalDateTime` without zone.

## 12. Migration strategy

1. Add `TchTimeProvider`.
2. Add pure schedule/occurredAt calculators.
3. Replace direct `now()` calls in draw/sales/result/offlinesync paths.
4. Add critical regression tests.
5. Add static/ArchUnit checks.
6. Migrate DB columns only where current schema is unsafe.
7. Update scheduler logs to show semantic owner zone.
