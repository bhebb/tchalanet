# Time & Timezone — Backend Policy & Usage

> **Status**: NORMATIVE  
> **Scope**: tchalanet-server (`common`, `core`, `catalog`, `features`, `batch`)  
> **Audience**: Backend developers, reviewers, ops  
> **Last reviewed**: 2026-01-21  
> **Related**:
>
> - `docs/conventions/api/context.md` (TchRequestContext usage)
> - `docs/conventions/security_permissions.md`
> - `docs/conventions/rls.md`
> - `docs/flow/auth_and_context.md`
> - `docs/flow/results_pipeline.md`

---

## 1. Purpose

This document defines the **official rules** for time and timezone handling in Tchalanet.

It fixes:

- which temporal types MUST be used (`Instant`, `LocalDate`, etc.),
- how `Clock` is used as the sole source for `now()`,
- how to apply tenant timezones and provider/slot timezones correctly,
- and what is forbidden.

This document is **normative**.

---

## 2. Core principles (non-negotiable)

### MUST

- Treat **`Instant` as the single source of truth** for business events.
- Persist and expose all event timestamps as **`Instant`**:
  - `createdAt`, `updatedAt`, `occurredAt`, `paidAt`, `syncedAt`, etc.
- Use an injected **`Clock`** for all “now” computations.

### MUST NOT

- Use `LocalDateTime` to represent a business event moment.
- Assume JVM timezone for business logic.
- Accept a client-provided timezone as system truth.

---

## 3. Time source: `Clock` (testable)

- A single `Clock` bean is provided.
- Configured via `app.zone-id` (default `UTC`).
- Goal: deterministic tests and consistent behavior across web/batch.

> `Clock` provides a testable `Instant`.  
> It is **not** a business timezone.

---

## 4. Tenant timezone & provider/slot timezone

### 4.1 Tenant timezone (default for tenant-centric rules)

Tenant default timezone is carried by request context:

- `TchRequestContext.tenantZoneId`

**Use it for**:

- tenant “today”
- calendar windows in dashboards/reports
- “draws of the day” tenant view
- any tenant-specific scheduling semantics

### 4.2 Provider/slot timezone (results pipeline)

Some computations must use a timezone **that is not** the tenant timezone.

Example:

- a results slot is in `America/New_York`
- tenant is in `America/Port-au-Prince`

**Rule**:

- provider/slot ZoneId MUST be passed explicitly to the computation
- it MUST NOT mutate the request context timezone

Approved examples:

- `OccurredAtResolver.resolve(..., slotZoneId, clock)`
- “window for slot” calculations use slot timezone

### 4.3 Fallback timezone

If tenant timezone is missing/invalid:

- fallback is `UTC` (or `app.zone-id`), but tenant config SHOULD make it non-null.

---

## 5. Allowed temporal types — why and when

### 5.1 `Instant` (DEFAULT)

**Why**: stable across timezones, DST-safe, best for persistence and APIs.

**Use for**:

- any business event moment
- DB columns representing moments in time
- API timestamps (ISO-8601 UTC)

**Examples**:

- ticket sold time, payout time, sync time
- result occurrence time

---

### 5.2 `LocalDate` (calendar date only)

**Why**: represents a date on a calendar without implying a moment.

**Use for**:

- date ranges in reporting (`startDate`, `endDate`)
- “draw date” as a business calendar concept
- user input where only a date exists

**Examples**:

- “report for 2026-01-21”
- “process last 7 days by date”

---

### 5.3 `LocalTime` (time-of-day only)

**Why**: represents schedule time independent of date.

**Use for**:

- draw schedules
- slot definitions

**Examples**:

- `drawTime=14:30`

---

### 5.4 `ZonedDateTime` (CALCULATION ONLY)

**Why**: needed when the rule is calendar-based and depends on a zone  
(DST shifts, “today” per zone, day boundaries).

**Use only for**:

- converting `Instant` to “calendar view” in a specific zone
- computing an `Instant` from `(LocalDate + LocalTime + ZoneId)`
- scheduler windows that depend on day boundaries in a zone

**Rule**:

- any computed `ZonedDateTime` MUST be converted back to `Instant` before persistence.

---

### 5.5 `LocalDateTime` (RESTRICTED)

**Why it’s dangerous**: timezone-ambiguous; DST makes it unstable.

**Allowed only if**:

- explicitly documented as “UI/query helper”
- not persisted
- not used as a business event moment

**Forbidden for**:

- any domain event
- any persisted timestamp

---

## 6. Approved conversion patterns

### 6.1 "Now" in a specific timezone (display or calendar logic)

**Approved**:

```java
ZonedDateTime now = timeProvider.now(ctx.tenantZoneId());
LocalDate today = now.toLocalDate();
```

---

### 6.2 Compute a business event Instant from calendar inputs

**Approved**:

```java
Instant occurredAt = ZonedDateTime.of(drawDate, drawTime, zoneId).toInstant();
```

---

### 6.3 Persisting

**Approved**:

- persist only `Instant`
- never persist `ZonedDateTime` or `LocalDateTime`

---

## 7. OccurredAtResolver (results pipeline)

Use `OccurredAtResolver`:

- if external `Instant` exists → use it
- else if `(drawDate + drawTime + zone)` exists → compute `ZonedDateTime.of(...).toInstant()`
- else fallback → `Instant.now(clock)`

This guarantees:

- deterministic behavior
- correct timezone conversions
- stable persistence type (`Instant`)

---

## 8. Scheduler helpers

### 8.1 DateWindows.datesBackInclusive

**What it does**:  
Returns an inclusive list of dates from `baseDate` back `daysBack`.

**Example**:

```
baseDate=2026-01-07, daysBack=2
→ [2026-01-07, 2026-01-06, 2026-01-05]
```

**Why it exists**:

- avoids off-by-one mistakes and "exclusive end" confusion
- expresses "backfill by calendar date" clearly
- yields deterministic ranges for retry/replay jobs

**When to use**:

- scheduler tick that scans N previous calendar days
- backfill jobs by `LocalDate`
- reporting date loops

**MUST**:

- compute `baseDate` in the correct timezone:
  - tenant windows → `timeProvider.today(ctx.tenantZoneId())`
  - slot/provider windows → `timeProvider.today(slotZoneId)`

---

### 8.2 DaysOfWeekParser

**What it does**:  
Parses day-of-week specs into `EnumSet<DayOfWeek>`.

**Accepted formats**:

- comma list: `MON,TUE,FRI`
- range: `MON-SAT`
- hyphen list: `WED-SAT-SUN`
- numeric tokens: `1..7` (1=MON, 7=SUN)

**Why it exists**:

- YAML/config friendly (compact)
- consistent schedule interpretation across modules
- removes custom parsing scattered in code

**When to use**:

- parsing scheduler configuration (`allowedDays`, `disabledDays`, etc.)
- slot scheduling policies
- batch gating schedules

**MUST NOT**:

- implement ad-hoc day parsing in jobs or handlers
- accept localized day names (parser is English tokens by design)

---

## 9. Timezone rules — practical guidance

### 9.1 Choose the timezone based on the semantic owner

- tenant-centric semantics → `ctx.tenantZoneId()`
- provider/slot semantics → `slotZoneId` from configuration
- never use JVM default timezone

---

### 9.2 Do not mutate the request context timezone

The request context carries tenant defaults.

Provider/slot calculations pass `ZoneId` explicitly.

---

### 9.3 Day boundaries and DST

Any "day boundary" logic MUST use `ZonedDateTime` in the chosen zone.

Convert back to `Instant` for persistence and comparisons.

---

## 10. PR checklist — Time & Timezone

- [ ] All persisted/exposed timestamps are `Instant`
- [ ] No `LocalDateTime` used as business event
- [ ] All "now" uses injected `Clock`
- [ ] Tenant "today" uses `ctx.tenantZoneId()`
- [ ] Provider/slot windows use explicit `slotZoneId`
- [ ] Any calendar computation uses `ZonedDateTime` then converts to `Instant`
- [ ] Scheduler backfills use `DateWindows.datesBackInclusive` with correct `baseDate`
- [ ] Day-of-week config uses `DaysOfWeekParser`
