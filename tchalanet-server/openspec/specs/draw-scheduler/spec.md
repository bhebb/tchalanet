# draw-scheduler Specification

## Purpose

TBD - created by archiving change simplify-draw-processing-schedulers. Update Purpose after archive.

## Requirements

### Requirement: Daily deterministic draw generation

The system MUST generate missing draws for active tenants on a daily deterministic schedule.

#### Scenario: Generate next seven days

- **WHEN** the generate scheduler runs at its configured cron
- **THEN** it MUST generate missing draws for all active tenants for the next configured number of days
- **AND** the default horizon SHOULD be 7 days
- **AND** it MUST NOT duplicate existing draws
- **AND** it MUST NOT mutate draws that already exist in non-generated states

### Requirement: Sales-open-time based open today

The system MUST open scheduled draws when the tenant/channel sales opening time is due.

The system MUST use `draw_channel.sales_open_time` when present.

If `draw_channel.sales_open_time` is null, the system MUST use the configured default sales opening time.

#### Scenario: Open today's draws

- **WHEN** the open-today scheduler runs at its configured cron
- **THEN** it MUST open only SCHEDULED draws for the current channel-local draw date
- **AND** it MUST open only draws whose effective sales opening time is due
- **AND** it MUST NOT open draws where `now >= cutoffAt`
- **AND** it MUST skip draws already OPEN, CLOSED, RESULTED, SETTLED, CANCELED, or ARCHIVED

### Requirement: Simple close due policy

The system MUST close due draws using the generated draw cutoff snapshot.

#### Scenario: Close due draw

- **GIVEN** an OPEN draw with `cutoffAt`
- **WHEN** `now >= cutoffAt`
- **THEN** the draw MUST be eligible for automatic close

#### Scenario: Not close too early

- **GIVEN** an OPEN draw with `cutoffAt`
- **WHEN** `now < cutoffAt`
- **THEN** the scheduler MUST NOT close the draw automatically

### Requirement: Close scheduler remains thin

The close scheduler MUST NOT implement complex provider or result-slot logic.

#### Scenario: Close uses generated draw state

- **WHEN** the close scheduler runs
- **THEN** it MUST use existing draw fields such as `cutoffAt`
- **AND** it MUST call an idempotent close command
- **AND** it MUST NOT recompute provider-specific draw times
- **AND** it MUST NOT recompute tenant/channel cutoff from live `draw_channel`

### Requirement: Ops can force close

Ops MUST be able to force close targeted draws outside the automatic close window.

#### Scenario: Force close with reason

- **GIVEN** an OPEN draw not yet eligible for automatic close
- **WHEN** an authorized Ops user calls force close with a non-blank reason
- **THEN** the system MAY close the draw
- **AND** the action MUST be audited

#### Scenario: Force close without reason

- **WHEN** an Ops user calls force close without a non-blank reason
- **THEN** the system MUST reject the request
