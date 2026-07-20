# Spec: draw-scheduling

## ADDED Requirements

### Requirement: Draw date is channel-local

`draw.drawDate` SHALL represent the commercial draw date in the draw channel timezone.

It SHALL NOT mean UTC date, server-local date, or tenant-local date by default.

#### Scenario: New York evening crosses UTC midnight

- **GIVEN** a draw channel timezone `America/New_York`
- **AND** `drawTime` is `22:30`
- **AND** `drawDate` is `2026-05-20`
- **WHEN** the generated draw is scheduled
- **THEN** `drawDate` remains `2026-05-20`
- **AND** `scheduledAt` is the instant corresponding to `2026-05-20T22:30` in `America/New_York`
- **AND** the UTC calendar date may be `2026-05-21` without changing the commercial draw date.

### Requirement: Generated draws store resolved instants

Generated draws SHALL store `scheduledAt` and `cutoffAt` as `Instant`.

#### Scenario: Compute cutoff

- **GIVEN** `drawDate=2026-05-20`
- **AND** `drawTime=22:30`
- **AND** `channelZone=America/New_York`
- **AND** `cutoffBeforeDraw=PT5M`
- **WHEN** the draw is generated
- **THEN** `scheduledAt = ZonedDateTime.of(drawDate, drawTime, channelZone).toInstant()`
- **AND** `cutoffAt = scheduledAt.minus(PT5M)`.

### Requirement: Daily opening uses the operating timezone and resolved draw instants

The daily opening scheduler SHALL run in the configured operating timezone. It SHALL select
eligible draws using their resolved `scheduledAt` and `cutoffAt`, not `salesOpenTime`.

#### Scenario: Tenant Haiti date differs from channel New York date

- **GIVEN** tenant timezone is `America/Port-au-Prince`
- **AND** channel timezone is `America/New_York`
- **AND** the current instant maps to different local dates in those zones
- **WHEN** the daily opening scheduler evaluates a draw
- **THEN** it uses the configured operating timezone for schedule execution
- **AND** it uses resolved draw instants for the eligibility window.

### Requirement: Close uses cutoff instant

The close scheduler SHALL close an open draw when `cutoffAt <= now`.

#### Scenario: Close exactly at cutoff

- **GIVEN** a draw is `OPEN`
- **AND** `cutoffAt` equals the current instant
- **WHEN** the close scheduler runs
- **THEN** the draw is eligible to close.
