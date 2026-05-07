# drawresult-processing Specification

## Purpose

TBD - created by archiving change simplify-draw-processing-schedulers. Update Purpose after archive.

## Requirements

### Requirement: Fetch is slot-driven and global

External result fetch MUST be driven by active global result slots, not by tenant draws.

#### Scenario: Fetch candidate calculation

- **GIVEN** an active result slot with `slotKey`, `drawTime`, and `timezone`
- **WHEN** the fetch scheduler evaluates the slot for a draw date
- **THEN** it MUST calculate `occurredAt` from `drawDate + drawTime + timezone`
- **AND** it MUST evaluate fetch due status from that instant
- **AND** it MUST NOT require a tenant id

### Requirement: Fetch starts after provider delay

Fetch MUST start only after a configured delay after draw time.

#### Scenario: Fetch starts at draw time plus five minutes

- **GIVEN** `startMinutesAfterDraw=5`
- **AND** `occurredAt=2026-05-06T14:30:00-04:00`
- **WHEN** current time is before `14:35`
- **THEN** the slot/date MUST NOT be fetch due
- **WHEN** current time is at or after `14:35`
- **THEN** the slot/date MAY be fetch due if retry and stop conditions pass

### Requirement: Fetch retry interval

Fetch MUST retry at most once per configured retry interval for a slot/date.

#### Scenario: Retry not due

- **GIVEN** `retryEveryMinutes=10`
- **AND** the last fetch attempt for a slot/date was 4 minutes ago
- **WHEN** the fetch scheduler runs
- **THEN** the slot/date MUST NOT be fetched again

#### Scenario: Retry due

- **GIVEN** `retryEveryMinutes=10`
- **AND** the last fetch attempt for a slot/date was 10 minutes ago
- **WHEN** the fetch scheduler runs
- **THEN** the slot/date MAY be fetched again if other conditions pass

### Requirement: Fetch stop window

Fetch MUST stop polling after a configured maximum duration after draw time.

#### Scenario: Fetch too old

- **GIVEN** `stopMinutesAfterDraw=240`
- **AND** the slot/date age is greater than 240 minutes
- **WHEN** the fetch scheduler runs
- **THEN** the slot/date MUST NOT be fetched automatically

### Requirement: Fetch does not process tenant draws

Fetch MUST only create or update global draw results.

#### Scenario: Fetch result obtained

- **WHEN** fetch obtains a provider result
- **THEN** it MUST upsert a global `draw_result`
- **AND** it MUST NOT attach that result to tenant draws
- **AND** it MUST NOT settle tickets

### Requirement: Fetch skips confirmed result unless forced

The automatic fetch scheduler MUST NOT fetch a slot/date whose result is already CONFIRMED.

#### Scenario: Confirmed result exists

- **GIVEN** a CONFIRMED `draw_result` exists for a slot/date
- **WHEN** the automatic fetch scheduler runs
- **THEN** it MUST skip that slot/date

#### Scenario: Ops force fetch

- **GIVEN** a CONFIRMED or old slot/date
- **WHEN** an authorized Ops user calls fetch with `force=true` and a non-blank reason
- **THEN** the system MAY bypass the automatic fetch window and retry interval
- **AND** the action MUST be audited

### Requirement: Apply attaches available results

Apply MUST attach existing global draw results to CLOSED tenant draws.

#### Scenario: Apply due

- **GIVEN** `startMinutesAfterDraw=10`
- **AND** a matching global `draw_result` exists
- **AND** a tenant draw is CLOSED and has no `draw_result_id`
- **WHEN** the apply scheduler is due for the slot/date
- **THEN** the system MUST attach the result and transition the draw to RESULTED

### Requirement: Apply never fetches or overwrites

Apply MUST NOT fetch external results and MUST NOT silently overwrite attached results.

#### Scenario: No draw_result exists

- **WHEN** apply runs for a slot/date without matching `draw_result`
- **THEN** it MUST skip without fetching

#### Scenario: Draw already has result

- **WHEN** apply runs for a draw with an existing `draw_result_id`
- **THEN** it MUST skip that draw
- **AND** correction MUST use a dedicated override/correction flow

### Requirement: Apply retry and stop policy

Apply MUST use its own retry interval and stop window.

#### Scenario: Apply window

- **GIVEN** `startMinutesAfterDraw=10`, `retryEveryMinutes=30`, and `stopMinutesAfterDraw=720`
- **WHEN** the scheduler evaluates a slot/date
- **THEN** it MUST apply only if age and retry conditions pass

### Requirement: Settle follows a later window than apply

Settlement MUST follow the same simple policy as apply, with a slightly later start.

#### Scenario: Settle due

- **GIVEN** `startMinutesAfterDraw=20`
- **AND** a tenant draw is RESULTED
- **AND** the draw has an attached `draw_result_id`
- **AND** unsettled tickets exist
- **WHEN** the settle scheduler is due
- **THEN** the system MUST settle eligible tickets

### Requirement: Settlement idempotency

Settlement MUST be idempotent.

#### Scenario: Already settled tickets

- **WHEN** settlement runs more than once for the same draw
- **THEN** already settled tickets MUST be skipped
- **AND** finalized payouts MUST NOT be overwritten
- **AND** no ticket MUST be paid twice

### Requirement: Processing order

The processing scheduler MUST run steps in a stable order.

#### Scenario: Processing tick

- **WHEN** the processing tick runs
- **THEN** it SHOULD run:
  1. close due draws
  2. fetch due external results
  3. apply available results
  4. settle resulted draws
